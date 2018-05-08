package de.uni_leipzig.simba.grecall.optimizer.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashMap;

import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.specification.LinkSpec;

public interface ScoreStrategy {
	
	boolean compare(LinkSpec spec1, LinkSpec spec2, NestedPlan plan1, NestedPlan plan2);
	
	LinkedHashMap<LinkSpec,Double> LinkToScore(LinkedHashMap<LinkSpec,NestedPlan> plans);

	LinkedHashMap<LinkSpec, Double> sortByValues(LinkedHashMap<LinkSpec, Double> plans, boolean b);

	}
