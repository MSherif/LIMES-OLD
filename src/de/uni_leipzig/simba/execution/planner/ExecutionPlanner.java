/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.grecall.util.DiffPair;
import de.uni_leipzig.simba.specification.LinkSpec;

/**
 *
 * @author ngonga
 */
public interface ExecutionPlanner {

    

    public NestedPlan plan(LinkSpec spec);
}
