package de.uni_leipzig.simba.genetics.util;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

public class ConfigMapper {
	static Logger logger = Logger.getLogger("LIMES");

	public static Mapping getMapping(ConfigReader cr) {
     HybridCache source = new HybridCache();
        source = HybridCache.getData(cr.getSourceInfo());

        //2.2 Then targetInfo
        logger.info("Loading target data ...");
        HybridCache target = new HybridCache();
        target = HybridCache.getData(cr.getTargetInfo());

        SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper(cr.executionPlan,
                cr.sourceInfo, cr.targetInfo, source, target, new LinearFilter(), cr.granularity);
        //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
        logger.info("Getting links ...");
        long time = System.currentTimeMillis();
        Mapping mapping = mapper.getLinks(cr.metricExpression, cr.verificationThreshold);
        logger.info("Got links in " + (System.currentTimeMillis() - time) + "ms.");
        //get Writer ready
        return mapping;
	}
}
