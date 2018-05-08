/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

/**
 * Index item for trigram indexer
 * @author ngonga
 */
public class TrigramIndexItem implements IndexItem, Comparable{
    String name;
    int size = 0;

    public TrigramIndexItem(String name, int size)
    {
        this.size = size;
        this.name = name;
    }
    
    public String toString()
    {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getId() {
        return name;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof TrigramIndexItem)
        {
            return getId().compareTo(((TrigramIndexItem) o).getId());
        }
        return -1;
    }
    @Override
    public boolean equals(Object obj) {
    	if (((TrigramIndexItem) obj).toString().equals(toString()))
    		return true;
    	else
    		return false;
    }
}
