/*
Group 17 - ECE 653
Filza Mazahir (20295951) - fmazahir@uwaterloo.ca
Sadaf Matinkhoo (20588163) - smatinkh@uwaterloo.ca
Nazli Medghalchi (20548504)- nmedghal@uwaterloo.ca
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BugDetectionTool {

    public static void main(String[] args) {
        
        String bitcode = args[0];
        String callGraphLine = null;
        int lineNum = 0;
        
        try {
            Process p = Runtime.getRuntime().exec("opt-3.0 -print-callgraph " + bitcode);
            BufferedReader processOutput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            // Check if its okay that ouput is being printed in the error stream - DELETE LATER
            
            while ((callGraphLine = processOutput.readLine()) != null) {
                lineNum++;
                
                // Skip first line since its a global node and does not have a function name
                if (lineNum == 1) {
                    continue;
                }
                
                //Trim empty white space before and after each line to skip empty lines
                callGraphLine = callGraphLine.trim();
                if (callGraphLine.isEmpty()) {
                    //new node going to be considered now, consider that for storing data
                    continue;
                }
                
                String[] tokens = callGraphLine.split("'");
                
                //To avoid lines without function names e.g. external nodes
                if (tokens.length < 2){
                    continue;
                }
                
                //Only for debugging purposes - DELETE LATER
                System.out.println(callGraphLine);
                System.out.println(tokens[1]);
                System.out.println("--");
            }
            
        } catch (IOException ex) {
            System.err.println("Encountered exception while running opt command: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(-1);
        }
        
        
        System.out.println(args[0]);
        
        if (args.length <= 3) {
            System.out.println("Part a, intra procedural analysis");
            /*Call the part a method (Sadaf's) here and remove the previous print line*/
        }
        else if (args.length <= 4){
             System.out.println("Part c, inter procedural analysis");
             /*Call the part c method (Nazli's) here and remove the previous line*/
        }
        else {
            System.out.println("Error");
        }

    }  
}
