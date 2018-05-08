package de.uni_leipzig.simba.grecall.optimizer.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.jamonapi.utils.Logger;

import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.optimizer.RecallOptimizerFactory;
import de.uni_leipzig.simba.specification.LinkSpec;

public class RuntimeScoreStrategy implements ScoreStrategy {

    @Override
    public boolean compare(LinkSpec spec1, LinkSpec spec2, NestedPlan plan1, NestedPlan plan2) {

	if (plan1.runtimeCost < plan2.runtimeCost && spec1.toString() != spec2.toString()) {
	    return true;
	}
	return false;
    }

    @Override
    public LinkedHashMap<LinkSpec, Double> LinkToScore(LinkedHashMap<LinkSpec, NestedPlan> plans) {

	LinkedHashMap<LinkSpec, Double> temp = new LinkedHashMap<LinkSpec, Double>();
	Iterator p = plans.entrySet().iterator();
	while (p.hasNext()) {
	    Map.Entry pr = (Map.Entry) p.next();
	    NestedPlan plan = (NestedPlan) pr.getValue();
	    temp.put((LinkSpec) pr.getKey(), new Double(plan.runtimeCost));

	}
	return temp;
    }

    @Override
    public LinkedHashMap<LinkSpec, Double> sortByValues(LinkedHashMap<LinkSpec, Double> plans, boolean flag) {

	List<Entry<LinkSpec, Double>> temp = new LinkedList<Entry<LinkSpec, Double>>(plans.entrySet());

	// Sorting the list based on values ascending
	if (flag == true) {
	    Collections.sort(temp, new Comparator<Entry<LinkSpec, Double>>() {
		public int compare(Entry<LinkSpec, Double> o1, Entry<LinkSpec, Double> o2) {
		    return o1.getValue().compareTo(o2.getValue());
		}
	    });
	} else {
	    Collections.sort(temp, new Comparator<Entry<LinkSpec, Double>>() {
		public int compare(Entry<LinkSpec, Double> o1, Entry<LinkSpec, Double> o2) {
		    return o2.getValue().compareTo(o1.getValue());
		}
	    });
	}

	// Maintaining insertion order with the help of LinkedList
	HashMap<LinkSpec, Double> sortedMap = new LinkedHashMap<LinkSpec, Double>();
	for (Entry<LinkSpec, Double> entry : temp) {
	    sortedMap.put((LinkSpec) entry.getKey(), (Double) entry.getValue());
	}
	return (LinkedHashMap<LinkSpec, Double>) sortedMap;
    }

}
