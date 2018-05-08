package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.SizeAwarePseudoFMeasure;
import de.uni_leipzig.simba.selfconfig.SizeUnawarePseudoFMeasure;

/**
 * Runs Evaluation on Pseudo measures.
 * @author Klaus Lyko
 */
public class PseudoEvaluationComplete extends PseudoBaseEvaluator{

	/**
	 * Profide name for the file log messages (System.out and System.err stream) should be bepassed to.
	 * @param name
	 */
	public PseudoEvaluationComplete(String name) {
		super(name);
	}

	public static void main(String args[]) {
		Double[] allBetaValues = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
				1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0};
		PseudoEvaluationComplete eval = null;
		Measure measures[] = { new SizeAwarePseudoFMeasure(), new SizeUnawarePseudoFMeasure()};	
		Map<DataSets, Measure[]> setup = new HashMap<DataSets, Measure[]>();
		if(args.length == 0) {
			setup = getEvaluationData();	
			
		} 
		else if(args.length == 2) {
			switch(Integer.parseInt(args[1])) {
			case 0:  measures = new Measure[]{new SizeAwarePseudoFMeasure()}; break;
			case 1:  measures = new Measure[]{new SizeUnawarePseudoFMeasure()}; break;
			}
		}
		if(args.length >= 1) {
				switch(Integer.parseInt(args[0])) {
					case 0: setup.put(DataSets.PERSON1_CSV, measures);	break; 
					case 1: setup.put(DataSets.PERSON2_CSV, measures); break;
					case 2: setup.put(DataSets.RESTAURANTS_CSV, measures); break;
					case 3: setup.put(DataSets.DBLPACM, measures); break;
					case 4: setup.put(DataSets.ABTBUY, measures); break;
					case 5: setup.put(DataSets.AMAZONGOOGLE, measures); break;
					default: setup.put(DataSets.PERSON1_CSV, measures);	break;					
				}			
				/* A run on Amazon-GoogleProducts requires up to 1 hour for a single run
				 * on 100 individuals for 20 generations. So running for 20 beta values 
				 * requires almost an entire day, and 5 days for a complete evaluation with 5 runs.
				 */
				
		}
		
		
		for(Entry<DataSets, Measure[]> test : setup.entrySet()) {
			EvaluationData param = DataSetChooser.getData(test.getKey());
			String nn = "DS_"+param.getEvaluationResultFileName();
			param.setEvaluationResultFileName(nn);
			try {
					eval = new PseudoEvaluationComplete(""+param.getName());
					eval.maxRuns=5;
					for(Measure measure : test.getValue()) {
						System.out.println("eval.run("+param.getName()+", "+measure.getName()+", allBetaValues)\n");
						logger.info("eval.run("+param.getName()+", "+measure.getName()+", allBetaValues)\n");
						eval.run(param, measure, allBetaValues);
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	
	/**
	 * Maps DataSets to their Measure they have to be tested with. 
	 * @return
	 */
	public static Map<DataSets, Measure[]> getEvaluationData() {
		Measure measures[] = { new SizeAwarePseudoFMeasure(), new SizeUnawarePseudoFMeasure()};
		Map<DataSets, Measure[]> testSetup = new HashMap<DataSets, Measure[]>();
		testSetup.put(DataSets.PERSON1_CSV, measures);
		testSetup.put(DataSets.PERSON2_CSV, measures);
		testSetup.put(DataSets.RESTAURANTS_CSV, measures);
		testSetup.put(DataSets.ABTBUY, measures);
		testSetup.put(DataSets.AMAZONGOOGLE, measures);
		return testSetup;
	}

}
