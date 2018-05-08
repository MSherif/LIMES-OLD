/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Implements execution plans that are given to an execution engine.
 * @author ngonga
 */
public class ExecutionPlan {
	static Logger logger = Logger.getLogger("LIMES");
    public List<Instruction> instructionList;
    public double runtimeCost;
    public double mappingSize;
    public double selectivity;
    
    /** Creates an empty instructionList
     * 
     */
    public ExecutionPlan()
    {
        instructionList = new ArrayList<Instruction>();
        runtimeCost = 0d;
        mappingSize = 0d;
        selectivity = 1d;
    }
    
    /** Adds an instruction to the instructionList
     * 
     * @param instruction Instruction
     */
    public void addInstruction(Instruction instruction)
    {
//    	System.out.println("Add Instruction to list"+instructionList+" the inst:"+instruction);
    	boolean added = instructionList.add(instruction);
    	if(!added)
    		logger.info("ExecutionPlan.addInstructiun() failed");
    }
    
    /** Removes the ith instruction from the instructionList
     * 
     * @param i Index of instruction to remove
     */
    public void removeInstruction(int i)
    {
        instructionList.remove(i);
    }
    
    /** Removes an instruction from a instructionList
     * 
     * @param i Instruction to remove
     */
    public void removeInstruction(Instruction i)
    {
        instructionList.remove(i);
    }
    
    /** Checks whether a instructionList is empty
     * 
     */
    public boolean isEmpty()
    {
        return instructionList.isEmpty();
    }
    
    /** Returns the list of instructions contained in a instructionList
     * 
     * @return List of instructions
     */
    public List<Instruction> getInstructionList()
    {
        return instructionList;
    }
    
    /** Returns the size of a instructionList
     * 
     * @return Number of instructions in the instructionList
     */
    public int size()
    {
        return instructionList.size();
    }
    
}
