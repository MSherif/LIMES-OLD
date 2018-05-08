/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author ngonga
 */
public class SilkConfigGenerator {
public static void main(String args[])
{
    //write file in ArrayList of strings
    ArrayList<String> doc = new ArrayList<String>();
    try
    {
        String file = "D:/tmp/links_to_compare/books";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        System.out.println("Reading input");
        
        String s = reader.readLine();
        while(s!=null)
        {
            doc.add(s);
            s = reader.readLine();
        }
        int max = 17;
                //Integer.parseInt(args[1]);
//        max =
        for(int i=1; i<max; i = i*2)
        {
            System.out.println("Generating config for confidence "+i);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file+i+".xml")));
            for(int l=0; l<doc.size(); l++)
            {
            s = doc.get(l);
            if(s.contains("maxDistance")) writer.println("<Param name=\"maxDistance\" value=\""+(i*2)+"\"/>");
            else writer.println(s);
            }
            writer.close();
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
}
}
