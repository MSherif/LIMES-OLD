/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.transformation.stringops;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class StringOps {
    public static String SEPARATOR = " ";
    public static List<String> getAllSubstrings(String s)
    {
        String split[] = s.split(SEPARATOR);
        String subString;
        List<String> result = new ArrayList<String>();
        for(int i=0; i<split.length; i++)
        {
            subString = split[i];
            result.add(subString);
            for(int j=i+1; j<split.length; j++)
            {
                subString = subString+SEPARATOR+split[j];
                result.add(subString);
            }
        }
        return result;
    }
    
    public static void main (String args[])
    {
        System.out.println(getAllSubstrings("a b c d"));
    }
}
