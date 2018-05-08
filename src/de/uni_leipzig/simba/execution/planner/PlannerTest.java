/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution.planner;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.NestedPlan;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.execution.rewriter.Rewriter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.mapper.SetOperations;
import de.uni_leipzig.simba.selfconfig.Experiment;
import de.uni_leipzig.simba.specification.LinkSpec;

/**
 *
 * @author ngonga
 */
public class PlannerTest {

    public static void test() {
        CanonicalPlanner cp = new CanonicalPlanner();
        Cache source = Experiment.readFile("Examples\\GeneticEval\\Datasets\\DBLP-Scholar/DBLP1.csv");
        Cache target = Experiment.readFile("Examples\\GeneticEval\\Datasets\\DBLP-Scholar/Scholar.csv");
//        Cache source = Experiment.readFile("C:\\Users\\Lyko\\workspace\\LIMES\\Examples\\GeneticEval\\Datasets\\DBLP-Scholar\\DBLP1.csv");
//        Cache target = Experiment.readFile("C:\\Users\\Lyko\\workspace\\LIMES\\Examples\\GeneticEval\\Datasets\\DBLP-Scholar\\Scholar.csv");
        HeliosPlanner hp = new HeliosPlanner(source, target);        
        LinkSpec spec = new LinkSpec();
        spec.readSpec("AND(euclidean(x.year,y.year)|0.8019,OR(cosine(x.title,y.title)|0.5263,AND(cosine(x.authors,y.authors)|0.5263,overlap(x.title,y.title)|0.5263)|0.2012)|0.2012)", 0.3627);
//        spec.readSpec("OR(cosine(x.title,y.title)|0.5263,AND(cosine(x.authors,y.authors)|0.5263,overlap(x.title,y.title)|0.5263)|0.2012)", 0.3627);
//        spec.readSpec("AND(jaccard(x.title,y.title)|0.4278,trigrams(x.authors,y.authors)|0.4278)", 0.36);
        
        System.out.println("Orginal: "+spec);
        Rewriter rewriter = new AlgebraicRewriter();
        spec = rewriter.rewrite(spec);
        System.out.println("Rewritten: "+spec);
        System.out.println("Canonical plan:\n"+cp.plan(spec));
        cp.plan(spec).draw();
        NestedPlan np = hp.plan(spec);
        np.draw();
        System.out.println("HELIOS plan:\n"+np);
        ExecutionEngine ee = new ExecutionEngine(source, target, "?x", "?y");
        long begin = System.currentTimeMillis();
        Mapping m1 = ee.runNestedPlan(np);
        long end = System.currentTimeMillis();
        System.out.println((end - begin)+" ms for HELIOS, "+m1.getNumberofMappings()+" results.");
        begin = System.currentTimeMillis();
        Mapping m2 = ee.runNestedPlan(cp.plan(spec));
        end = System.currentTimeMillis();
        System.out.println((end - begin)+" ms for CANONICAL, "+m2.getNumberofMappings()+" results.");        
        System.out.println(SetOperations.difference(m1, m2).getNumberofMappings() +" are missing somewhere");
        System.out.println(SetOperations.difference(m2, m1).getNumberofMappings() +" are missing somewhere");
    }

    public static void main(String args[]) {
        test();
    }
}
