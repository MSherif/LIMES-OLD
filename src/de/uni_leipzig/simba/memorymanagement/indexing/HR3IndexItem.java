/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Simple implementation of an index item
 * @author ngonga
 */
public class HR3IndexItem extends AbstractIndexItem implements IndexItem, Comparable{
    int size;
    
    // Index is simply coordinates of hypercube
    ArrayList<Integer> coordinates;
    
    
    public String toString()
    {
        if(coordinates.size() == 0) return "Empty";
        String result = coordinates.get(0)+"";
        for(int i=1; i<coordinates.size(); i++)
        result = result + " - " + coordinates.get(i);
        return result;
    }
    
    public HR3IndexItem(int size, String id)
    {
        this.size = size;
        String split[] = id.split(Pattern.quote("A"));
        coordinates = new ArrayList<Integer>();
        for(int i=0; i<split.length; i++)
        {
            coordinates.add(Integer.parseInt(split[i]));
        }
    }
    
    public int getSize()
    {
        return size;
    }
    
    public void setSize(int n)
    {
        size = n;
    }
    
    public String getId()
    {
        StringBuffer s = new StringBuffer();
        for(int i=0; i<coordinates.size(); i++)
            s.append(coordinates.get(i)+" ");
        //return coordinates;
        return s.toString();
    }

    @Override
    public int compareTo(Object o) {
        if (((HR3IndexItem) o).toString().equals(toString())) return 0;
        else return -1;
    }
    @Override
    public boolean equals(Object obj) {
    	if (((HR3IndexItem) obj).toString().equals(toString()))
    		return true;
    	else
    		return false;
    }
}
