/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.memorymanagement.Index.planner.execution;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Instance;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.memorymanagement.Index.planner.DataManipulationCommand;
import de.uni_leipzig.simba.memorymanagement.Index.planner.DataOperator;
import de.uni_leipzig.simba.memorymanagement.datacache.DataCache;
import de.uni_leipzig.simba.memorymanagement.indexing.Indexer;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class CacheAccessExecution {

    DataCache cache;
    List<DataManipulationCommand> commands;
    Measure measure;
    double threshold;
    String property = "p1|p2";//
    Indexer indexer;

    public CacheAccessExecution(DataCache c, List<DataManipulationCommand> commands, Measure m, double threshold, Indexer indexer) {
        cache = c;
        this.commands = commands;
        measure = m;
        this.threshold = threshold;
        this.indexer = indexer;
    }

    /**
     * Run a computation plan including loading and flushing of data from memory
     * TODO: Implement comparisons
     */
    public void run() {
        Mapping m = new Mapping();
        int count = 0;
        DataManipulationCommand currentCommand;
        for (int i = 0; i < commands.size(); i++) {
            currentCommand = commands.get(i);
            if (currentCommand.op.equals(DataOperator.LOAD)) {
                for (int j = 0; j < currentCommand.operands.size(); j++) {
//                    System.out.println("LOADING " + currentCommand.operands.get(j).getId());
                    cache.getData(currentCommand.operands.get(j), indexer);
                }
            } else if (currentCommand.op.equals(DataOperator.FLUSH)) {
                for (int j = 0; j < currentCommand.operands.size(); j++) {
//                    System.out.println("FLUSHING " + currentCommand.operands.get(j).getId());
                    cache.deleteData(currentCommand.operands.get(j));
                }
            } else {
                Cache source = cache.getData(currentCommand.operands.get(0), indexer);
//                System.out.println("LOADING "+currentCommand.operands.get(0).getId());
                Cache target = cache.getData(currentCommand.operands.get(1), indexer);
//                System.out.println("LOADING "+currentCommand.operands.get(1).getId());

//                System.out.println(source);
//                System.out.println(target);
                double d=0;
                for (Instance s : source.getAllInstances()) {
                    for (Instance t : source.getAllInstances()) {
                        d = measure.getSimilarity(s, t, property, property);
                        if (d >= threshold) {
                            count++;
//                           m.add(s.getUri(), t.getUri(), d);
                        }
                    }
                }
            }
        }
 //       System.out.println("Mapping contains "+m.getNumberofMappings()+" mappings");
 //              System.out.println("Mapping contains "+count+" mappings");
    }
}
