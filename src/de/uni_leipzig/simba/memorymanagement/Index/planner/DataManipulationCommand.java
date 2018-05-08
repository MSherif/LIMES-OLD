/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.planner;

import de.uni_leipzig.simba.memorymanagement.indexing.IndexItem;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class DataManipulationCommand {
    public DataOperator op;
    public List<IndexItem> operands;
    
    public DataManipulationCommand(DataOperator op, List<IndexItem> operands)
    {
        this.op = op;
        this.operands = operands;
    }
        
    public String toString()
    {
        return op+"\t"+operands;
    }
}
