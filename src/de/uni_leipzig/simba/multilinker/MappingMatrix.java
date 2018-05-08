/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.multilinker;

import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public class MappingMatrix {

    Mapping[][] mappings;

    public MappingMatrix(int size) {
        mappings = new Mapping[size][size];
    }

    public void addMapping(int i, int j, Mapping m) {
        if (i < j) {
            mappings[i][j] = m;
        } else {
            mappings[j][i] = m;
        }
    }

    public Mapping getMapping(int i, int j) {
        if (i < j) {
            return mappings[i][j];
        } else {
            return mappings[j][i];
        }
    }
    
    public String toString()
    {
        String result = "";
        for(int i=0; i<mappings.length; i++)
        {
            for(int j=i+1; j<mappings.length; j++)
            {
                result = result + i + " -> "+j+"\n----------\n";
                result = result + mappings[i][j] + "\n";
            }
        }
        return result;
    }
    
    public MappingMatrix getSubMatrix(double threshold)
    {
        MappingMatrix m = new MappingMatrix(mappings.length);
        for(int i=0; i<mappings.length; i++)
        {
            for(int j=i+1; j<mappings.length; j++)
            {
                m.addMapping(i, j, getMapping(i, j).getSubMap(threshold));
            }
        }
        return m;
    }
}
