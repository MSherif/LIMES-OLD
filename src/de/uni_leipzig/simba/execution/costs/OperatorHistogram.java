/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.costs;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.mapper.SetOperations.Operator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class OperatorHistogram {
    
    int minSize;
    int maxSize;
    int iterations;
    List<Integer> mappingSizes;
    Operator operator;
    public OperatorHistogram(List<Integer> mappingSizes, int minStringSize, int maxStringSize, int iterations, Operator op) {
        this.minSize = minStringSize;
        this.maxSize = maxStringSize;
        this.iterations = iterations;
        this.mappingSizes = mappingSizes;
        this.operator = op;
    }
    
    public void computeHistogram()
    {
        for(int sourceSize: mappingSizes)
        {
            for(int targetSize: mappingSizes)
            {
                for(int i=0; i<iterations; i++)
                {
                    Mapping m1 = Mapping.generateRandomMapping(sourceSize, minSize, maxSize);
                    Mapping m2 = Mapping.generateRandomMapping(targetSize, minSize, maxSize);
                    long memory = Runtime.getRuntime().totalMemory();
                    long begin = System.currentTimeMillis();
                    Mapping m = SetOperations.getMapping(m1, m2, operator);
                    long end = System.currentTimeMillis();
                    long endMemory = Runtime.getRuntime().totalMemory();
                    System.out.println(sourceSize+"\t"+targetSize+"\t"+(end-begin)+"\t"+m.getNumberofMappings()+"\t"+(endMemory - memory));                    
                }
            }
        }
    }
    
    
    public static void main(String args[])
    {
        List<Integer> sizes = new ArrayList<Integer>();
        for(int i=1000; i<=10000; i= i+1000)
            sizes.add(i);
        int iterations = 20;
        System.out.println("\nOR\n");
        OperatorHistogram oh = new OperatorHistogram(sizes, 3, 20, iterations, Operator.OR);
        oh.computeHistogram();
        
        System.out.println("\nAND\n");
        oh = new OperatorHistogram(sizes, 3, 20, iterations, Operator.AND);
        oh.computeHistogram();
        
        System.out.println("\nDIFF\n");
        oh = new OperatorHistogram(sizes, 3, 20, iterations, Operator.DIFF);
        oh.computeHistogram();
        
        System.out.println("\nXOR\n");
        oh = new OperatorHistogram(sizes, 3, 20, iterations, Operator.XOR);
        oh.computeHistogram();
    }
}
