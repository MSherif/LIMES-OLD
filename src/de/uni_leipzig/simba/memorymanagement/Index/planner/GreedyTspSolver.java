/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.planner;

import de.uni_leipzig.simba.memorymanagement.TSPSolver.NearestNeighbor;

/**
 *
 * @author ngonga
 */
public class GreedyTspSolver extends TSPSolver{
    
    @Override
    public int[] getPath(double[][] m)
    {
        return new NearestNeighbor(m,0).getPath();
    }
    
}
