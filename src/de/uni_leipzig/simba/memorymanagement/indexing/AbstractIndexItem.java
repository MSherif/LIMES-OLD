/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.indexing;

/**
 *
 * @author ngonga
 */
public abstract class AbstractIndexItem implements IndexItem, Comparable {
     public int compareTo(Object o) {
        if (((AbstractIndexItem) o).toString().equals(toString())) return 0;
        else return -1;
    }
}
