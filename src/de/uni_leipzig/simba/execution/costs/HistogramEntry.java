/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.costs;

/**
 *
 * @author ngonga
 */
public class HistogramEntry {
    public long runtime;
    public long memory;
    public long mappingSize;
    
    public HistogramEntry()
    {
        runtime = 0;
        memory = 0;
        mappingSize = 0;
    }
    
    public HistogramEntry(long runtime, long memory)
    {
        this.runtime = runtime;
        this.memory = memory;
    }
    
    public void addMemory(long m)
    {
        memory = memory + m;
    }
    
    public void addRuntime(long r)
    {
        runtime = runtime + r;
    }
    
    public void addMappingSize(long s)
    {
        mappingSize = mappingSize + s;
    }
    
    public String toString()
    {
        return "["+runtime+" ms, "+memory/(1024) + " KB]";
    }
}
