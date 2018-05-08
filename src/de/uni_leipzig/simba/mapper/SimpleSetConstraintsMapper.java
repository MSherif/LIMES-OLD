/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.mapper;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.controller.Parser;
import de.uni_leipzig.simba.mapper.atomic.fastngram.FastNGram;
import de.uni_leipzig.simba.mapper.atomic.EDJoin;
import de.uni_leipzig.simba.mapper.atomic.PPJoinPlusPlus;
import de.uni_leipzig.simba.mapper.atomic.TotalOrderBlockingMapper;
import de.uni_leipzig.simba.measures.Measure;
import de.uni_leipzig.simba.measures.MeasureFactory;
import org.apache.log4j.Logger;

/**
 * Implements a set constraints mapper with a simple execution plan, i.e., an
 * execution plan that uses a minimal amount of filter operations and a maximal
 * amount of set operations on mappings
 * @author ngonga
 */
public class SimpleSetConstraintsMapper implements SetConstraintsMapper {

    static Logger logger = Logger.getLogger("LIMES");
    Cache source;
    Cache target;
    KBInfo sourceInfo, targetInfo;
    Filter filter;
    int granularity;

    public SimpleSetConstraintsMapper()
    {
        //not to be used. Does nothing.
    }
    /** Constructor
     *
     * @param sInfo KBInfo that contains all infos on the source
     * @param tInfo KBInfo that contains all infos on the targer
     * @param s Cache that contains the data of the source
     * @param t Cache that contains the data of the target
     * @param m AtomicMapper that should be use to carry out the atomic mapping operations
     */
    public SimpleSetConstraintsMapper(KBInfo sInfo, KBInfo tInfo, Cache s, Cache t, Filter f, int _granularity) {
        source = s;
        target = t;
        filter = f;
        sourceInfo = sInfo;
        targetInfo = tInfo;
        granularity = _granularity;
    }

    /** Implements the functionality of a SetConstraintsMapper mapper. It splits
     *  the expression in a sequence of operations and carries out the corresponding
     *  execution plan. In this particular case, the least possible amount of filters are used
     *  and the call are carried out recursively.
     * @param expression Similarity expression that corresponds to the LIMES grammar
     * @param threshold General similarity threshold
     * @return
     */
    public Mapping getLinks(String expression, double threshold) {
        long startTime;
        Mapping map = new Mapping();
        Parser p = new Parser(expression, threshold);
        if (p.isAtomic()) {
//            logger.info("Expression " + expression + " is atomic.");
//            logger.info("Starting atomic mapper on " + expression + " with threshold " + threshold);
            map = getLinksFromAtomic(expression, threshold);
//            logger.info("Atomic mapper got "+map.map.size()+" links");
//            logger.info(map);
        } else {
            // use the merges
//            logger.info("Expression " + expression +" with threshold "+threshold+" is not atomic");
//            logger.info("Getting links for " + p.term1 + " with threshold " + p.threshold1);
            startTime = System.currentTimeMillis();
            Mapping map1 = getLinks(p.term1, p.threshold1);

            startTime = System.currentTimeMillis();
//            logger.info("Getting links for " + p.term2 + " with threshold " + p.threshold2);
            Mapping map2 = getLinks(p.term2, p.threshold2);

            startTime = System.currentTimeMillis();
            map = mergeMaps(p.op, map1, map2, p.coef1, p.coef2, expression, threshold);
//            logger.info("Merging and filtering took " + (System.currentTimeMillis() - startTime) + " ms");
//            logger.info("Merged via "+p.op+" to "+map.map.size()+" links.");
        }
        return map;
    }

    public Mapping getLinksFromAtomic(String expression, double threshold) {
        Parser p = new Parser(expression, threshold);
        AtomicMapper mapper = MeasureFactory.getMapper(p.op);
        Measure m = MeasureFactory.getMeasure(p.op);
        if(mapper instanceof TotalOrderBlockingMapper)
        {
            ((TotalOrderBlockingMapper) mapper).granularity = granularity;
        }
//        ;
//        if (m.getType().equalsIgnoreCase("string")) {            
//            if(m.getName().startsWith("leven"))
//                mapper = new EDJoin();
//            else if (m.getName().startsWith("qgrams"))
//                mapper = new FastNGram();
//            else 
//                //change to generic mapper
//                mapper = new PPJoinPlusPlus();
//        } else if (m.getType().equalsIgnoreCase("spatial")) {
//            TotalOrderBlockingMapper mapperr = new TotalOrderBlockingMapper();
//            mapperr.granularity = granularity;
//            mapper = mapperr;
//            
//        } else {
//            logger.warn("Could not get type of measure " + p.op + ". Returning empty mapping.");
//            return new Mapping();
//        }
//        System.out.println("get Mapping for expr: "+expression+" >= "+threshold);
        return mapper.getMapping(source, target, sourceInfo.var, targetInfo.var, expression, threshold);
    }

    /** Merges maps with respect to a given operation
     *
     * @param operation Operation for merging
     * @param map1 First map
     * @param map2 Second map
     * @param condition Metric expression to use
     * @param threshold Threshold value
     * @return A merge mapping
     */
    public Mapping mergeMaps(String operation, Mapping map1, Mapping map2,
            double coef1, double coef2, String condition, double threshold) {
        Mapping map = new Mapping();
        if (operation.equalsIgnoreCase("MAX")) {
            //for max, we know that at least one maps the threshold so no need to
            //run filter operations
            logger.info("Merging with MAX");
            return SetOperations.union(map1, map2);
        } else if (operation.equalsIgnoreCase("AND")) {
            //here a supplementary condition can be given with respect to thresholds
            //thus we need to run a threshold filter
//            logger.info("Merging with AND and filtering with threshold " + threshold);            
            return filter.filter(SetOperations.intersection(map1, map2), threshold);
        } else if (operation.equalsIgnoreCase("MIN")) {
            //similar situation to max and or
            logger.info("Merging with MIN");
            return SetOperations.intersection(map1, map2);
        } else if (operation.equalsIgnoreCase("OR")) {
//            logger.info("Merging with OR and filtering with threshold" + threshold);
            return filter.filter(SetOperations.union(map1, map2), threshold);
        } else if (operation.equalsIgnoreCase("MINUS")||operation.equalsIgnoreCase("DIFF")) {
//            logger.info("Merging with MINUS");
            return filter.filter(SetOperations.difference(map1, map2), threshold);
        } else if (operation.equalsIgnoreCase("XOR")) {
//            logger.info("Merging with XOR and filtering with threshold " + threshold);
            return filter.filter(SetOperations.union(SetOperations.difference(map1, map2), SetOperations.difference(map2, map1)), threshold);
        } else if (operation.equalsIgnoreCase("ADD")) {
            // we use the intersection because we know that each entry in the resulting map
            // must fulfill both conditions that led to the split when parsing the linear
            //combination
            logger.info("Merging with ADD and filtering with condition " + condition + " and threshold " + threshold);
            return filter.filter(map1, map2, coef1, coef2, threshold, "ADD");
            //less efficient would be
            //return filter.filter(SetOperations.intersection(map1, map2), condition,
            //                     threshold, source, target, sourceInfo.var, targetInfo.var);
        } else if (operation.equalsIgnoreCase("MULT")) {
            // we use the intersection because we know that each entry in the resulting map
            // must fulfill both conditions that led to the split when parsing the multiplication
//            logger.info("Merging with MULT and filtering with condition " + condition + " and threshold " + threshold);
            return filter.filter(map1, map2, coef1, coef2, threshold, "MULT");
            //less efficient would be
            //return filter.filter(SetOperations.intersection(map1, map2), condition,
            //                     threshold, source, target, sourceInfo.var, targetInfo.var);
        } else {
            logger.fatal("Operation " + operation + " unknown or not yet implemented");
        }
        //should not happen
        return map;
    }
}
