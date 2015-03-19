/*
Group 17 - ECE 653
Filza Mazahir (20295951) - fmazahir@uwaterloo.ca
Sadaf Matinkhoo (20588163) - smatinkh@uwaterloo.ca
Nazli Medghalchi (20548504)- nmedghal@uwaterloo.ca
 */


public class BugDetectionTool {

    public static void main(String[] args) {
        
        if (args.length <= 3) {
            System.out.println("Part a, intra procedural analysis");
            /*Call the part a method here*/
        }
        else if (args.length <= 4){
             System.out.println("Part c, inter procedural analysis");
             /*Call the part c method here*/
        }
        else {
            System.out.println("Error");
        }

        
        
    }  
}
