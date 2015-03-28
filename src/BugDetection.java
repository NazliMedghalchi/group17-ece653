/*
Group 17 - ECE 653
Filza Mazahir (20295951) - fmazahir@uwaterloo.ca
Sadaf Matinkhoo (20588163) - smatinkh@uwaterloo.ca
Nazli Medghalchi (20548504)- nmedghal@uwaterloo.ca
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BugDetection {
	
    private String bitcode;
    private int support;
    private int confidence;
 // Determines the level of expansion for inter-procedural analysis.
 // level = 0 implies intra-procedural analysis.  
    private int level; 
    
    private Hashtable<String, HashSet<String>> functionsToNodesTable = new Hashtable<String, HashSet<String>>();
    private Hashtable<Pair, HashSet<String>> functionsPairsToNodesTable = new Hashtable<Pair, HashSet<String>>();
    private Hashtable<String, HashSet<String>> nodesToFunctionsTable = new Hashtable<String, HashSet<String>>();

//  Parse the arguments of the main function and checks for the correct number of arguments
//  For inter-procedural analysis, the program can have either 2 or 4 arguments. 
//  - With 2 arguments, the default support and confidence values will be used, 
//  and the 2nd argument  will tell the program its an InterProcedural Analysis. 
//  - With 4 arguments, the 4th argument will tell the program that 
//  Inter-Procedural analysis is being called.
    
    public void parseArgs(String[] args) { //CHECK FOR FORMAT OF ARGUMENTS, CORRECT VALUES, NO NEGATIVES, ETC!
        bitcode = args[0];
        
        switch (args.length) {
            case 1: support = 3;
                    confidence = 65;
                    level = 0;
                    break;

            case 2: support = 3;
                    confidence = 65;
                    level = Integer.parseInt(args[1]);
                    break;

            case 3: support = Integer.parseInt(args[1]);
                    confidence = Integer.parseInt(args[2]);
                    level = 0;
                    break;

            case 4: support = Integer.parseInt(args[1]);
                    confidence = Integer.parseInt(args[2]);
                    level = Integer.parseInt(args[3]);
                    break;

            default:    System.err.println("Error: Wrong number of arguments provided. Program exiting.");
                        System.exit(-1);
        }
    }
        
    
    
//  This function uses LLVM to generate a call graph, then parses each line to get 
//  the functions that are called in each node, and stores them in the 
//  nodesToFunctionsTable hash table.
    public void parseCallGraph() {
        
        String callGraphLine = null;
        String nodeName = "";
        HashSet<String> functionsInNode = new HashSet<String>();
        State state = State.FIRST_LINE;
        
        try {
        	//DELETE -3.0 when submitting/testing on ecelinux.
        	Process p = Runtime.getRuntime().exec("opt-3.0 -print-callgraph " + bitcode);  
            BufferedReader processOutput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            
            while((callGraphLine = processOutput.readLine()) != null) {
                callGraphLine = callGraphLine.trim();
                switch(state) {
                    case FIRST_LINE:
                        state = State.WAIT_FOR_FIRST_EMPTY_LINE; 
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
                            //this.insertFunctionsPairsToNodes(functionsInNode, nodeName);
                            //this.insertFunctionsToNodes(functionsInNode, nodeName);
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
                System.out.println("NodesToFunctions: " + nodesToFunctionsTable); // DELETE LATER! For debugging only 
            
        } catch (IOException ex) {
            System.err.println("Error: Encountered exception while running opt command: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(-1);
        }
             
    }  
    
    
    public void createDataStructure (Hashtable <String, HashSet<String>> nodesToFunctionsTable, int level) {
    	Set<String> keySet = nodesToFunctionsTable.keySet();
    	Iterator<String> keySetIterator = keySet.iterator();
    	while (keySetIterator.hasNext()) {
    		String nodeName = keySetIterator.next();
    		HashSet<String> functionsInNode = new HashSet<String>();
    		getFunctionCalls(nodeName, level, functionsInNode, nodesToFunctionsTable);
    		HashSet<String> set = nodesToFunctionsTable.get(nodeName);
    		insertFunctionsPairsToNodes(set, nodeName);
    		insertFunctionsToNodes(set, nodeName);
    	}
    	System.out.println("Function pairs to Nodes: " + functionsPairsToNodesTable); // DELETE LATER! For debugging only
        System.out.println("FunctionsToNodes: " + functionsToNodesTable); // DELETE LATER! For debugging only 
    }
    
    public void getFunctionCalls(String nodeName, int level, HashSet<String> functionsInNode,
    		Hashtable<String, HashSet<String>> nodesToFunctionsTable) {
    	if (level < 0) return;
    	Iterator<String> iter = nodesToFunctionsTable.get(nodeName).iterator();
    	while (iter.hasNext()) {
    		String function = iter.next();
    		functionsInNode.add(function);
    		getFunctionCalls(function, level-1, functionsInNode, nodesToFunctionsTable);
    	}
    }
    
//  Inserts into hash table 'functionsPairsToNodesTable' where function pairs within a node are the keys,
//  and the nodes its being called in are the values
//  This function also creates the pairs from the set of functions calls within the node
    public void insertFunctionsPairsToNodes(HashSet<String> set, String nodeName){
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
    public void insertFunctionsToNodes (HashSet<String> set, String nodeName) {
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
        FIRST_LINE, WAIT_FOR_FIRST_EMPTY_LINE, SAVE_NODE_NAME, SAVE_FUNCTIONS
    }
    
    
 // Class Pair defined, used for function pairs
    public class Pair {
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
    
    
    
    //Main function
    public static void main(String[] args) {
        
        BugDetection tool = new BugDetection();
        tool.parseArgs(args);
        tool.parseCallGraph();
    	tool.createDataStructure(tool.nodesToFunctionsTable, tool.level);
        
    }

}
