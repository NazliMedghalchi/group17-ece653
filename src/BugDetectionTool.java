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
import java.util.List;

public class BugDetectionTool {
	
	private String bitcode;
    private int support;
    private int confidence;
    private String interProceduralFunction;
    
    Hashtable<String, Integer> functionsTable = new Hashtable<String, Integer>();
    Hashtable<Pair, Integer> functionsPairsTable = new Hashtable<Pair, Integer>();

    //Parse the arguments of the main function
    public void parseArgs(String[] args)   {
        if (/*args.length == 2 ||*/ args.length > 4) {
            /* It is not possible to do intra-procedural analysis with two arguments.
             * Considering the first argument should always be the bitcode file,
             * if there is only one argument after that, it is either the optional
             * argument for inter-procedural, or just one integer which is not possible
             * to determine whether it is intended for support or confidence.
             * 
             * But what about the case where we want to do inter-prodecural with default support
             * and confidence? that would only have 2 arguments... */
        	
        	//-------------------------------------------------------------------------------------
        	// This is my suggested fix:
        	
        	/* Parses the arguments and checks for any kind of error in the
        	 * format of arguments. Returns an integer array in the following format:
        	 * {support, confidence, inter} where inter is a 0 or 1 value determining
        	 * whether we are doing intra- or inter-procedural analysis.
        	 * Assumption: the format of the arguments is correct e.g. no negatives. 	
        	private static void parseArgs(String[] args)   {
                bitcode = args[0];
            	
                switch (args.length) {
                	case 1: support = 3;
                    		confidence = 65;
                    		// Call intra- function
                    		break;
                	
                	case 2: support = 3;
            				confidence = 65;
            				//Call inter- function
            				break;
                	
                	case 3: support = Integer.parseInt(args[1]);
            				confidence = Integer.parseInt(args[2]);
            				// Call intra- function
            				break;
                	
                	case 4: support = Integer.parseInt(args[1]);
        					confidence = Integer.parseInt(args[2]);
        					// Call inter- function
        					break;
        					
        			default: System.err.println("Error: Wrong mnumber of arguments provided. Program exiting.");
                    		 System.exit(-1);
                    		
                }
            } */
        	System.err.println("Error: Wrong mnumber of arguments provided. Program exiting.");
            System.exit(-1);
        }
        
        bitcode = args[0];
        
        if (args.length == 1) {
            support = 3;
            confidence = 65;
        } else {
            support = Integer.parseInt(args[1]);
            confidence = Integer.parseInt(args[2]);
        }
        
        //Call the respective methods here - inter-procedural and intra-procedural
        if (args.length == 1 || args.length == 3) {
            //Call the part a method (Sadaf's) here 
        }
        else if (args.length == 4) {
            interProceduralFunction = args[3];
            //Call the part c method (Nazli's) here 
        }
        
    }
    
    
    //Use LLVM to generate call graph, then parse it
    public void parseCallGraph() {
        
        String callGraphLine = null;
        int lineNum = 0;
        boolean newNode = false; 
        HashSet<String> functionsInNode = new HashSet<String>();
        
        try {
            /* opt-3.0 is not recognized on ecelinux. I tried and I got "command not found"	
             * error. But opt worked just fine. */
        	Process p = Runtime.getRuntime().exec("opt-3.0 -print-callgraph " + bitcode);
        	BufferedReader processOutput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            
            while ((callGraphLine = processOutput.readLine()) != null) {
                System.out.println("Line in callgraph: " + callGraphLine); // DELETE LATER! - For debugging only
                
                lineNum++;
                
                // Skip first line since its a global node and does not have a function name
                if (lineNum == 1) {
                    continue;
                }
                
                //Trim empty white space before and after each line to skip empty lines
                callGraphLine = callGraphLine.trim();
                if (callGraphLine.isEmpty()) {
                    newNode = true;
                    continue;
                }
                
                String[] tokens = callGraphLine.split("'");
                if (newNode == true) {
                    System.out.println("Funstions in the node:" + functionsInNode); // DELETE LATER! - For debugging only
                    System.out.println("--"); // DELETE LATER! - For debugging only
                    this.createPairs(functionsInNode);
                    functionsInNode.clear();
                    newNode = false;
                    continue;
                }
                
                //To avoid lines without function names e.g. external nodes
                if (tokens.length < 2){
                    continue;
                }
                String functionName = tokens[1];
                functionsInNode.add(functionName);
                
            }
            
        } catch (IOException ex) {
            System.err.println("Error: Encountered exception while running opt command: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(-1);
        }
             
    }  
    
    public void createPairs(HashSet<String> set){
        List<String> functions = new ArrayList<String>(set);
        
        for (int i = 0; i< functions.size(); i++){
            for (int j = i+1; j< functions.size(); j++){
                Pair tempPair = new Pair(functions.get(i), functions.get(j));
                if (functionsPairsTable.containsKey(tempPair)){
                    int currentCount = functionsPairsTable.get(tempPair);
                    functionsPairsTable.put(tempPair,currentCount+1);
                } else{
                    functionsPairsTable.put(tempPair, 1);
                }
            }
        }
        System.out.println("Function pairs: " + functionsPairsTable); // DELETE LATER! For debugging only
    }
    
    
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
        
        BugDetectionTool tool = new BugDetectionTool();
        tool.parseArgs(args);
        tool.parseCallGraph();
 
    }

}
