package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import org.jgap.gp.IGPProgram;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.rewriter.AlgebraicRewriter;
import de.uni_leipzig.simba.genetics.core.Metric;
import de.uni_leipzig.simba.measures.pointsets.evaluation.FMeasureRecorder;
import de.uni_leipzig.simba.specification.LinkSpec;

public class EvaluationPseudoMemory implements Comparable{

	public int sortNumber;
	public int run;
	public Metric metric;
	public Double pseudoFMeasure;
	public Double fmeasue;
	public Double recall;
	public Double precision;
	public Double fmeasue_1to1;
	public Double recall_1to1;
	public Double precision_1to1;
	public long runTime;
	public int generation;
	public Double fitness;
	public int knownInstances;
	public IGPProgram program;
	public double betaValue;
	public Mapping fullMapping = new Mapping();
	public double matthew;
	
	/**
	 * @param run should be a >= 1 for sort to function properly
	 * @param gen Number of the generation this individual was recorded
	 * @param m LinkSpec of this instance
	 * @param fitness fitness value of this instance
	 * @param pfm PseudoFMeasure which should be the basis of the fitness value and in most cases 1-fitness
	 * @param runTime Means runtime of this run until this generation
	 */
	public EvaluationPseudoMemory(int run, int gen, Metric m, double fitness, double pfm, long runTime) {
		this.run = run;
		this.generation = gen;
		this.metric = m;
		if(Double.isNaN(fitness))
			this.fitness = 0d;
		else if(fitness > 10d)
				this.fitness = 10d;
		else
			this.fitness = fitness;
		if(Double.isNaN(pfm))
			this.pseudoFMeasure = 0d;
		else {
			this.pseudoFMeasure = pfm;
//			if(pfm < 0.0001d)
//				this.pseudoFMeasure = 0d;
		}
		if(runTime < 0)
			this.runTime = runTime*-1;
		else
			this.runTime = runTime;
		this.sortNumber = gen+run;
	}
	
	/**
	 * To sort them later per gen and not run, we construct a sort number := generation+run
	 * So we can automatic write mean values over all runs.
	 * @return
	 */
	public int getSortNumber() {
		return  sortNumber = Integer.parseInt(""+generation+""+run);
	}

	/**
	 * All fields sperated by sep.
	 * @param sep Sepations String, e.g. ";"
	 * @return field[i] + sep + field[i+1]
	 */
	public String toString(String sep) {
		String ret = run + sep +generation + sep + 
				fitness +sep+
				pseudoFMeasure +sep+
				this.fmeasue+sep+
				recall+sep+
				precision+sep+
				this.fmeasue_1to1+sep+
				recall_1to1+sep+
				precision_1to1+sep+
				runTime/1000;
		if(knownInstances > 0) {
			ret += sep + knownInstances;
		} else {
			ret += sep + 0;
		}
		ret+=sep+metric;
		try {
			AlgebraicRewriter rewriter = new AlgebraicRewriter();
			LinkSpec original = new LinkSpec();
			original.readSpec(metric.getExpression(), metric.getThreshold());
			
			LinkSpec rewritten = rewriter.rewrite(original);
			ret +=sep+rewritten.toStringOneLine();
		} catch(Exception e) {
			ret += sep + "-none-";
		}
		
		
		return ret;
	}

	public int compareTo(Object o) {
		EvaluationPseudoMemory os =  (EvaluationPseudoMemory)o;		
		if(generation == os.generation)
			return run-os.run;
		else {
			return generation - os.generation;
		}
	}
	@Override
	public String toString() {
		return "gen="+generation+"run="+run+": "+metric+" ..." +
				" Mapping.size()="+fullMapping.size() +
				"[fit="+fitness+" pfm= "+pseudoFMeasure+" f="+fmeasue+" f_1to1="+fmeasue_1to1+"]";
	}
	
	public boolean isValid() {
		if(fmeasue == null || recall == null || precision == null || Double.isNaN(fmeasue) || Double.isNaN(recall) || Double.isNaN(precision)) {
			return false;
		}		
		return true;
	}
	
	public static void main(String args[]) {
		Double d=null;
		double d2 = Double.NaN;

		System.out.println(d == null);
		System.out.println(Double.isNaN(d2));
	}
}
