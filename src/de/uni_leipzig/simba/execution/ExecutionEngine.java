/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.Instruction.Command;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.mapper.AtomicMapper;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.mapper.atomic.EDJoin;
import de.uni_leipzig.simba.mapper.atomic.JaroMapper;
import de.uni_leipzig.simba.mapper.atomic.OrchidMapper;
import de.uni_leipzig.simba.mapper.atomic.PPJoinPlusPlus;
import de.uni_leipzig.simba.mapper.atomic.SymmetricHausdorffMapper;
import de.uni_leipzig.simba.mapper.atomic.TotalOrderBlockingMapper;
import de.uni_leipzig.simba.mapper.atomic.fastngram.FastNGram;
import de.uni_leipzig.simba.specification.LinkSpec;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Implements an execution engine. The idea is that the engine gets a series of
 * instructions (i.e., an execution plan) and runs these instructions
 * sequentially and returns a mapping.
 *
 * @author ngonga
 */
public class ExecutionEngine {

    static Logger logger = Logger.getLogger("LIMES");
    //contains the results     
    private List<Mapping> buffer;
    private String sourceVariable;
    private String targetVariable;
    private Cache source;
    private Cache target;

    /**
     * Constructor for an execution engine.
     *
     * @param source Source cache
     * @param target Target cache
     * @param sourceVar Source variable (usually "?x")
     * @param targetVar Target variable (usually "?y")
     */
    public ExecutionEngine(Cache source, Cache target, String sourceVar, String targetVar) {
        buffer = new ArrayList<Mapping>();
        this.source = source;
        this.target = target;
        sourceVariable = sourceVar;
        targetVariable = targetVar;
    }

    /**
     * Implementation of the execution of a plan.
     * Be aware that this doesn't excutes complex LinkSpecs! Use
     * runNestedPlan() instead!.
     *
     * @param plan An execution plan
     * @return The mapping that results from running the plan
     */
    private Mapping run(ExecutionPlan plan) {
//        logger.info("Beginning with execution of linear plan. Nr of instructions="+plan.getInstructionList().size());
        buffer = new ArrayList<Mapping>();
        if (plan.isEmpty()) {
            logger.info("Plan is empty. Done.");
            return new Mapping();
        }
        
        List<Instruction> instructions = plan.getInstructionList();
//        logger.info("Plan.empty?"+plan.isEmpty()+" - Instructions.empty?"+instructions.isEmpty()+" Instrcutions:"+ instructions);
        Mapping m = new Mapping();
        for (int i = 0; i < instructions.size(); i++) {
            //System.out.println("***** " + buffer);
            Instruction inst = instructions.get(i);
//            logger.info("Running instruction " + inst + " ...");
            //get the index for writing the results
            int index = inst.getResultIndex();
            
            //first process the RUN operator
            if (inst.getCommand().equals(Command.RUN)) {
                m = executeRun(inst);
            } // runs the filter operator
            else if (inst.getCommand().equals(Command.FILTER)) {
                m = executeFilter(inst, buffer.get(inst.getSourceMapping()));
            } //runs set operations such as intersection, 
            else if (inst.getCommand().equals(Command.INTERSECTION)) {
                m = SetOperations.intersection(buffer.get(inst.getSourceMapping()),
                        buffer.get(inst.getTargetMapping()));
            } // union 
            else if (inst.getCommand().equals(Command.UNION)) {
                m = SetOperations.union(buffer.get(inst.getSourceMapping()),
                        buffer.get(inst.getTargetMapping()));
            } // diff 
            else if (inst.getCommand().equals(Command.DIFF)) {
                m = SetOperations.difference(buffer.get(inst.getSourceMapping()),
                        buffer.get(inst.getTargetMapping()));
            } // end of processing. Return the indicated mapping
            else if (inst.getCommand().equals(Command.RETURN)) {
                logger.info("Reached return command. Returning results.");
                if (buffer.isEmpty()) {
                    return m;
                }
                if (index < 0) {
                    return buffer.get(buffer.size() - 1);
                } else {
                    return buffer.get(index);
                }
            }

            if (index < 0) {
                buffer.add(m);
            } else {
                //add placeholders to ensure that the mapping can be placed
                //where the user wanted to have it
                while ((index + 1) > buffer.size()) {
                    buffer.add(new Mapping());
                }
                buffer.set(index, m);
            }
        }
       
        // just in case the return operator was forgotten. 
        // Then we return the last mapping computed
        if (buffer.isEmpty()) {
            return new Mapping();
        } else {
//            logger.info("Done. Returning " + buffer.get(buffer.size() - 1).getNumberofMappings() + " results.");
            return buffer.get(buffer.size() - 1);
        }
        

    }

    /**
     * Implements running the run operator. Assume atomic measures
     *
     * @param inst Instruction
     * @param source Source cache
     * @param target Target cache
     * @return Mapping
     */
    public Mapping executeRun(Instruction inst) {
        //get threshold

        double threshold = Double.parseDouble(inst.getThreshold());
        //generate correct mapper
        
        AtomicMapper mapper;
        if (inst.getMeasureExpression().startsWith("leven")) {
            mapper = new EDJoin();
        }else if (inst.getMeasureExpression().startsWith("euclid")) {
            mapper = new TotalOrderBlockingMapper();
        }else if (inst.getMeasureExpression().startsWith("jaro")){
        	mapper = new JaroMapper();
        }else if (inst.getMeasureExpression().startsWith("qgrams")){
        	mapper = new FastNGram();
        }else if (inst.getMeasureExpression().startsWith("hausdorff") || inst.getMeasureExpression().startsWith("geomean")){
        	mapper = new OrchidMapper();
        } 
        else {
            mapper = new PPJoinPlusPlus();
        }
        
//        logger.info("Execute run:"+inst);
        //run mapper
        return mapper.getMapping(source, target, sourceVariable, targetVariable,
                inst.getMeasureExpression(), threshold);
    }

    /**
     * Runs the filtering operator
     *
     * @param inst Instruction
     * @param input Mapping that is to be filtered
     * @return Filtered mapping
     */
    private Mapping executeFilter(Instruction inst, Mapping input) {
        LinearFilter filter = new LinearFilter();
        //logger.info(inst.getMeasureExpression());
        //logger.info(inst.getThreshold());
        return filter.filter(input, inst.getMeasureExpression(), Double.parseDouble(inst.getThreshold()),
                source, target, sourceVariable, targetVariable);
    }

    /**
     * Implements the intersection of mappings
     *
     * @param inst Instruction
     * @param m1 First mapping
     * @param m2 Second mapping
     * @return Intersection of m1 and m2
     */
    private Mapping executeIntersection(Instruction inst, Mapping m1, Mapping m2) {
        return SetOperations.intersection(m1, m2);
    }

    private Mapping executeUnion(Instruction inst, Mapping m1, Mapping m2) {
        return SetOperations.union(m1, m2);
    }

    public static void testNestedPlanExecution() {
        //data
        Cache source = new MemoryCache();
        source.addTriple("S1", "surname", "sandra");
        source.addTriple("S1", "name", "bullock");
        source.addTriple("S2", "surname", "lukas");
        source.addTriple("S2", "name", "duke");
        source.addTriple("S1", "age", "31");
        source.addTriple("S2", "age", "31");

        Cache target = new MemoryCache();
        target.addTriple("T1", "surname", "sandy");
        target.addTriple("T1", "name", "bullock");
        target.addTriple("T1", "alter", "31");

        target.addTriple("T2", "surname", "luke");
        target.addTriple("T2", "name", "duke");
        target.addTriple("T2", "alter", "30");

        //spec
        String metric = "AND(trigrams(x.name, y.name)|0.8,OR(cosine(x.surname, y.surname)|0.5, euclidean(x.age, y.alter)|0.7)|0.4)";
        //String metric = "trigrams(x.name, y.name)";
        LinkSpec spec = new LinkSpec(metric, 0.3);
        HeliosPlanner cp = new HeliosPlanner(target, target);
        NestedPlan plan = cp.plan(spec);
        logger.info(plan);
        ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y"); 
        System.out.println("Nested Plan Result = " + ee.runNestedPlan(plan));
    }

    /**
     * Implements the test
     *
     */
    public static void test() {
        //data
        Cache source = new MemoryCache();
        source.addTriple("S1", "surname", "sandra");
        source.addTriple("S1", "name", "bullock");
        source.addTriple("S2", "surname", "lukas");
        source.addTriple("S2", "name", "duke");
        source.addTriple("S1", "age", "31");
        source.addTriple("S1", "age", "31");

        Cache target = new MemoryCache();
        target.addTriple("T1", "surname", "sandy");
        target.addTriple("T1", "name", "bullock");
        target.addTriple("T1", "alter", "31");

        target.addTriple("T2", "surname", "lukas");
        target.addTriple("T2", "name", "dorkas,love");
        target.addTriple("T2", "name", "12");

        //instructions
        Instruction i1 = new Instruction(Command.RUN, "levenshtein(x.surname, y.surname)",
                "0.5", -1, -1, 0);
        Instruction i2 = new Instruction(Command.RUN, "levenshtein(x.name, y.name)",
                "0.5", -1, -1, 1);
        Instruction i3 = new Instruction(Command.UNION, "", "0.5", 0, 1, 2);

        //execution plan
        ExecutionPlan plan = new ExecutionPlan();
        plan.addInstruction(i1);
        plan.addInstruction(i2);
        plan.addInstruction(i3);

        //engine
        ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
        //test run method
        System.out.println(source);
        System.out.println(target);
        Mapping m1 = ee.executeRun(i1);
        System.out.println(m1);
        Mapping m2 = ee.executeRun(i2);
        System.out.println(m2);
        Mapping m3 = ee.executeUnion(i3, m1, m2);
        System.out.println(m3);

        System.out.println(ee.run(plan));

    }

    /**
     * Runs a nested plan
     *
     * @param nestedPlan NestedPlan
     * @return Mapping
     */
    public Mapping runNestedPlan(NestedPlan nestedPlan) {
        //empty nested plan contains nothing
        long begin = System.currentTimeMillis();
        
        Mapping m = new Mapping();
        if (nestedPlan.isEmpty()) {
        } //atomic nested plan just contain simple list of instructions 
        else if (nestedPlan.isAtomic()) {
            logger.info("Running this (atomic): "+ nestedPlan);
            m = run(nestedPlan);
            
        } //nested plans contain subplans, an operator for merging the results of the
        //subplans and a filter for filtering the results of the subplan
        else {
        
            //run all the subplans
            logger.info("Running this (complex): "+ nestedPlan.subPlans.get(0));
            m = runNestedPlan(nestedPlan.subPlans.get(0));
            Mapping m2, result = m;
            for (int i = 1; i < nestedPlan.subPlans.size(); i++) {
        	logger.info("Running this (complex): "+ nestedPlan.subPlans.get(i));
                m2 = runNestedPlan(nestedPlan.subPlans.get(i));
                logger.info("Operator of plan "+ nestedPlan.operator);
                if (nestedPlan.operator.equals(Command.INTERSECTION)) {
                    result = SetOperations.intersection(m, m2);
                } // union 
                else if (nestedPlan.operator.equals(Command.UNION)) {
                    result = SetOperations.union(m, m2);
                } // diff 
                else if (nestedPlan.operator.equals(Command.DIFF)) {
                    result = SetOperations.difference(m, m2);
                } else if (nestedPlan.operator.equals(Command.XOR)) {
                	
                    result = SetOperations.xor(m, m2);
                }
//                logger.info("Running " + nestedPlan.operator + " on " + m.getNumberofMappings() + "results and " + m2.getNumberofMappings() + " "
//                        + "mappings and got " + result.getNumberofMappings() + " mappings.");

//                logger.info("Running "+nestedPlan.operator+" on "+m+" and "+m2+" and got "+result);
                m = result;
            }
            //only run filtering if there is a filter indeed, else simply return mapping
            if (nestedPlan.filteringInstruction != null) {
                if (Double.parseDouble(nestedPlan.filteringInstruction.getThreshold()) > 0) {
                    logger.info("Filtering with "+nestedPlan.filteringInstruction);
                    m = executeFilter(nestedPlan.filteringInstruction, m);
                }
            }
        }
        long end = System.currentTimeMillis();
        return m;
    }

    public static void main(String args[]) {
//        test();
//        System.out.println("_______________________");
        testNestedPlanExecution();
    }
}
