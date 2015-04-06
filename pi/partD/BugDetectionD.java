/*
Group 17 - ECE 653
Filza Mazahir (20295951) - fmazahir@uwaterloo.ca
Sadaf Matinkhoo (20588163) - smatinkh@uwaterloo.ca
Nazli Medghalchi (20548504)- nmedghal@uwaterloo.ca

Part D
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/* This program will do a static analysis on a callgraph and report bugs based on likely invariant. For details,
 refer to the write up in proj_sub.pdf for part I (d).
 
 This program is a modification of the BugDetection.java program written for part I (a). 
 There are two algorithms being implemented:
 
 1. Reduce False Positives:
 To reduce false positives, the approach used here is to use a sliding confidence threshold. The T_CONFIDENCE
 should increase based on the support of the function linearly. 
 To use this algorithm, the 5th argument has to be 1.
 
 2: Find More Bugs:
To find more bugs, the order of functions being called is taken into account when determining pairs for bugs.
 To use this algorithm, the 5th argument has to be 2.
 */



public class BugDetectionD {
	
    private String callgraph;
    private int support;
    private int confidence;
    private int level; // level of expansion for inter-procedural analysis. level = 0 implies intra-procedural analysis. 
    private int algorithm; // 1 for reducing false positives, 2 for finding more bugs
    
 // A hash table with function names as keys and a hash set of nodes where the function have been  called as values.
    private Hashtable<String, HashSet<String>> functionsToNodesTable = new Hashtable<String, HashSet<String>>();
 // A hash table with pairs of function names as keys and a hash set of nodes where the function pair have been  called as values.    
    private Hashtable<Pair, HashSet<String>> functionsPairsToNodesTable = new Hashtable<Pair, HashSet<String>>();
 // A hash table with node names as keys and a hash set of functions that are called in the node as values. 
    private Hashtable<String, HashSet<String>> nodesToFunctionsTable = new Hashtable<String, HashSet<String>>();

//  IMPORTANT NOTE: We assume all 5 arguments passed for this partD of the project, 
//  as it is hard to figure out otherwise which algorithm is to be used.
//  
    private void parseArgs(String[] args) {
        if (args.length != 5){
            System.err.println("Error: Wrong number of arguments provided. Need 5 arguments for part D. Program exiting.");
            System.exit(-1);
        }
        callgraph = args[0];
        support = Integer.parseInt(args[1]);
        confidence = Integer.parseInt(args[2]);
        level = Integer.parseInt(args[3]);
        algorithm = Integer.parseInt(args[4]);                       
    }
       
    
    
//  This function uses LLVM to generate a call graph, then parses each line to get 
//  the functions that are called in each node, and stores them in the 
//  nodesToFunctionsTable hash table.
    private void parseCallGraph() {
        
        String callGraphLine = null;
        String nodeName = "";
        HashSet<String> functionsInNode = new HashSet<String>();
        State state = State.IGNORE_FIRST_FEW_LINES;
        int lineNum=0;
        try {
            BufferedReader processOutput = new BufferedReader(new FileReader(callgraph));
            while((callGraphLine = processOutput.readLine()) != null) {
               lineNum++;
                callGraphLine = callGraphLine.trim();
                switch(state) {
                    //to ignore the warnings in the beginning
                    case IGNORE_FIRST_FEW_LINES:
                        if (lineNum == 7) state = State.WAIT_FOR_FIRST_EMPTY_LINE; 
                        //else System.out.println(callGraphLine);
                        break;
                    // callGraphLines within the first <null function> node are ignored    
                    case WAIT_FOR_FIRST_EMPTY_LINE:
                        if (callGraphLine.isEmpty()) state = State.SAVE_NODE_NAME;
                        break;
                        
                    case SAVE_NODE_NAME:
                        String[] nodeNameLine = callGraphLine.split("'");
                        nodeName = nodeNameLine[1];
                        state = State.SAVE_FUNCTIONS;
                        break;
                        
                    case SAVE_FUNCTIONS:
                        if (callGraphLine.isEmpty()) { // this node is now done being parsed
                            nodesToFunctionsTable.put(nodeName, new HashSet<String>(functionsInNode));
                            functionsInNode.clear();
                            state = State.SAVE_NODE_NAME;
                            break;
                        }
                        String[] functionName = callGraphLine.split("'");
                        if (functionName.length < 2) break; // to ignore the external nodes
                        functionsInNode.add(functionName[1]);
                        break;
                }
            } // end of while (finished reading callgraph)
            processOutput.close();
        } catch (Exception ex) {
            System.err.println("Error: Error encountered in parsing call graph: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(-1);
        }
             
    }  
    
    
 // This function iterates over the key set of nodesToFunctionsTable, and expands
 // the nodes contained in each node to the desired level of expansion.
 // Then, fills the functionsToNodesTable and functionPairsToNodesTable.
    private void createDataStructure () {
    	Set<String> keySet = nodesToFunctionsTable.keySet();
    	Iterator<String> keySetIterator = keySet.iterator();
    	while (keySetIterator.hasNext()) {
    		String nodeName = keySetIterator.next();
    		HashSet<String> functionsInNode = new HashSet<String>();
    		if (!nodesToFunctionsTable.get(nodeName).isEmpty()) { 
			Set<String> path = new HashSet<String>();
			getFunctionCalls(nodeName, level, functionsInNode, path);
		}
    		insertFunctionsPairsToNodes(functionsInNode, nodeName);
    		insertFunctionsToNodes(functionsInNode, nodeName);
    	}
    }
    
   
  // This is a recursive function that expands scopes to desired level.
  // It takes the scope name as an argument, retrieves the values for that scope, which is
  // a set of function names, then iterates over those functions and retrives the functions called in each of them
  // from the nodesToFunctionsTable, and adds them to the functionsInNode hash set.
    private void getFunctionCalls(String nodeName, int level, HashSet<String> functionsInNode, Set<String> path) {
    	if (path.contains(nodeName)) return; // Avoid loops.
	Iterator<String> iter = nodesToFunctionsTable.get(nodeName).iterator();
    	if (!iter.hasNext() || level < 0)  {
		// Only add leaf nodes. 
    		functionsInNode.add(nodeName);
		return;
	}
	path.add(nodeName);
    	while (iter.hasNext()) {
    		String function = iter.next();
    		getFunctionCalls(function, level-1, functionsInNode, path);
    	}
	path.remove(nodeName);
    }
    
//  Inserts into hash table 'functionsPairsToNodesTable' where function pairs within a node are the keys,
//  and the nodes its being called in are the values
//  This function also creates the pairs from the set of functions calls within the node
    private void insertFunctionsPairsToNodes(HashSet<String> set, String nodeName){
        List<String> functions = new ArrayList<String>(set);
        for (int i = 0; i< functions.size(); i++){
            for (int j = i+1; j< functions.size(); j++){
                Pair tempPair = new Pair(functions.get(i), functions.get(j));
                if (! functionsPairsToNodesTable.containsKey(tempPair)){
                    functionsPairsToNodesTable.put(tempPair, new HashSet<String>());
                } 
                functionsPairsToNodesTable.get(tempPair).add(nodeName); 
            }
        }
    }
    
    
//  Inserts into hash table 'functionsToNodesTable' where function names are they keys,
//  and the nodes its being called in are the values
    private void insertFunctionsToNodes (HashSet<String> set, String nodeName) {
        List<String> functions = new ArrayList<String>(set);
        for (int i = 0; i< functions.size(); i++){
            if (! functionsToNodesTable.containsKey(functions.get(i))){
                functionsToNodesTable.put(functions.get(i), new HashSet<String>());
            } 
            functionsToNodesTable.get(functions.get(i)).add(nodeName); 
        } 
    }
    
    
//  State machine used for parsing the call graph
    private enum State {
        IGNORE_FIRST_FEW_LINES, WAIT_FOR_FIRST_EMPTY_LINE, SAVE_NODE_NAME, SAVE_FUNCTIONS
    }
    
    
 // Class Pair defined, used for function pairs
    private static class Pair {
        private final String left;
        private final String right;

        public Pair (String left, String right) {
            this.left = left;
            this.right = right;
        }

        public String getLeft() {
            return left;
        }

        public String getRight(){
            return right;
        }

        @Override
        public int hashCode() {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) return false;
            Pair pairo = (Pair) o;
            return (this.left.equals(pairo.getLeft()) && this.right.equals(pairo.getRight())) || (this.right.equals(pairo.getLeft()) && this.left.equals(pairo.getRight()));
        }
        
        @Override
        public String toString(){
            return "("+ left + "," + right + ")";
        }
        
    }
    
    private void findBugs() {
    	// Iterate over the keys in functionPairsToNodesTable
    	Set<Pair> functionPairsKeySet = functionsPairsToNodesTable.keySet();
    	Iterator<Pair> functionPairsKeySetIterator = functionPairsKeySet.iterator();
    	while (functionPairsKeySetIterator.hasNext()) {
    		Pair keyPair = functionPairsKeySetIterator.next();
    		int pairSupport = functionsPairsToNodesTable.get(keyPair).size();
    		if (pairSupport >= support) {
    			String function = keyPair.getLeft();
                findBugs(function, keyPair, pairSupport);
        		function = keyPair.getRight();
                findBugs(function, keyPair, pairSupport);
    		} 
    	}
    }
    
    private void findBugs(String function, Pair keyPair, int pairSupport) {
    	HashSet<String> functionScopes = functionsToNodesTable.get(function);
    	HashSet<String> functionPairScopes = functionsPairsToNodesTable.get(keyPair);
    	int functionSupport = functionScopes.size();
    	// If pairSupport and functionSupport are equal, it means that the two functions in 
    	// the pair are always called together --> no bug!
    	if (functionSupport > pairSupport) {
    		float bugConfidence = pairSupport / ((float)functionSupport) * 100;
    		if ((bugConfidence >= confidence && algorithm == 2)||(bugConfidence >=findConfidenceThreshold(pairSupport) && algorithm ==1)) {
    			// if confidence is greater than or equal to the threshold
    			// --> there is a bug! Print it!
    			Iterator<String> iter = functionScopes.iterator();
    	    	while (iter.hasNext()) {
    	    		String scope = iter.next();
    	    		if (!functionPairScopes.contains(scope)) {
    	    			// this is one scope where the function is called but the function pair
    	    			// is not called --> this is the scope where the bug happens
    	    			
    	    			// Sort the functions in pair and add them to the bug report
    	    			String leftFunction = keyPair.getLeft();
    	    			String rightFunction = keyPair.getRight();
    	    			int compare = leftFunction.compareTo(rightFunction);
    	    			String firstFunctionInPair, secondFunctionInPair;
    	    			if (compare <= 0){
    	    				firstFunctionInPair = keyPair.getLeft();
    	    				secondFunctionInPair = keyPair.getRight();
    	    			} else {
    	    				firstFunctionInPair = keyPair.getRight();
    	    				secondFunctionInPair = keyPair.getLeft();
    	    			}
    	    			String bugReport = String.format("bug: %s in %s, pair: (%s, %s), "
    	    					+ "support: %d, confidence: %.2f%%\n", function, scope,
    	    					firstFunctionInPair, secondFunctionInPair, pairSupport, bugConfidence);
    	    			System.out.print(bugReport);
    	    		}
    	    	}
    		}
    	}
    }
    
    //Calculate confidence threshold 
    private int findConfidenceThreshold(int functionPairSupport){
        int confidenceThreshold = ((functionPairSupport - support)*(99-confidence)/(100 - support))+confidence;
        return confidenceThreshold;
    }
    
    //Main function
    public static void main(String[] args) {    
        BugDetectionD tool = new BugDetectionD();
        tool.parseArgs(args);
        tool.parseCallGraph();
        tool.createDataStructure();
    	tool.findBugs();
    }

}
