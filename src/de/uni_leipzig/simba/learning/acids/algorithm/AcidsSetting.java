package de.uni_leipzig.simba.learning.acids.algorithm;

import java.util.ArrayList;
import java.util.Collections;

import libsvm.svm_parameter;
import de.uni_leipzig.simba.learning.acids.output.Fscore;
import de.uni_leipzig.simba.learning.acids.utility.GammaComparator;
import de.uni_leipzig.simba.learning.acids.data.Couple;
import de.uni_leipzig.simba.learning.acids.data.Resource;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class AcidsSetting {
	
	private ArrayList<Resource> sources, targets;
	private int trainingSetSize;
	
	private SvmHandler m;
	private AcidsMeasures measures;
	private AcidsOracle oracle;
	private AcidsEvaluation eval;
	private AcidsTrainer trainer;
	
	private int dk = 5;
	private int mapSize;
	
	protected Fscore fs;

	public Fscore getFs() {
		return fs;
	}
	
	public AcidsSetting(ArrayList<Resource> sources,
			ArrayList<Resource> targets, AcidsOracle oracle, int trainingSetSize) {
		
		this.sources = sources;
		this.targets = targets;
		this.oracle = oracle;
		this.trainingSetSize = trainingSetSize;
		this.mapSize = Math.min(sources.size(), targets.size());

		m = new SvmHandler(svm_parameter.LINEAR);
		measures = new AcidsMeasures(this);
		eval = new AcidsEvaluation(this);
		trainer = new AcidsTrainer(this);
	}
	
	public void run() {
		
		ArrayList<AcidsSimilarity> sims = measures.getAllSimilarities();
		ArrayList<Couple> couples = new ArrayList<Couple>();
		
		ArrayList<Couple> labelled = new ArrayList<Couple>();
		ArrayList<Couple> inferred = new ArrayList<Couple>();
		ArrayList<Resource> processed = new ArrayList<Resource>();
		
		for(int i=1; labelled.size() < trainingSetSize; i++) {
			
			System.out.println("\n### Iteration = "+i+" ###");
			
			// TODO The following works only with linear classifiers. Implement one for polynomial classifiers.
			if(i == 1) {
				couples = filtering(sims);
				if(couples == null)
					return;
				computeRemaining(sims, couples); // TODO Shouldn't it be included in filtering(sims)?
				for(Couple c : couples)
					measures.estimateMissingValues(c); // TODO Shouldn't it be included in filtering(sims)?
			}
			
			ArrayList<Couple> posInformative = new ArrayList<Couple>();
			ArrayList<Couple> negInformative = new ArrayList<Couple>();
			
			for(Couple c : couples) {
		        c.setGamma( m.computeGamma(c) ); // TODO Change to m.computeGamma(c);
				if(m.classify(c))
					posInformative.add(c);
				else
					negInformative.add(c);
			}
			
			System.out.println("theta = "+m.getTheta()+"\tpos = "+posInformative.size()+"\tneg = "+negInformative.size());
			
			Collections.sort(posInformative, new GammaComparator());
			Collections.sort(negInformative, new GammaComparator());
			
			ArrayList<Couple> poslbl = new ArrayList<Couple>();
			ArrayList<Couple> neglbl = new ArrayList<Couple>();
			
			if(i == 1) {
				// optional for right classifier orientation
//				orientate(poslbl, neglbl, labelled);
				// train with dk most likely positive examples
				trainer.train(posInformative, negInformative, inferred, labelled, poslbl, neglbl, this.dk, false);
			}
			
			// train with dk most informative positive examples
			trainer.train(posInformative, negInformative, inferred, labelled, poslbl, neglbl, this.dk, true);
			// train with dk most informative negative examples
			trainer.train(negInformative, posInformative, inferred, labelled, poslbl, neglbl, this.dk, true);
			
			for(Couple c : labelled)
				if(c.isPositive()) {
					final int NEG = 10;
					poslbl.add(c);
					Resource s = c.getSource(), t = c.getTarget();
					if(!processed.contains(s)) {
//						for(Resource t1 : targets) {
						for(int j=0; j<NEG; j++) {
							Resource t1 = targets.get((int) (targets.size()*Math.random()));
							if(t1 == t) { j--; continue; }
							Couple c1 = new Couple(s, t1);
							for(AcidsSimilarity sim : sims)
								c1.setDistance(sim.getSimilarity(s.getPropertyValue(sim.getProperty().getName()),
										t1.getPropertyValue(sim.getProperty().getName())), sim.getIndex());
							measures.estimateMissingValues(c1);
							c1.setPositive(false);
							neglbl.add(c1);
							inferred.add(c1);
						}
						processed.add(s);
					}
					if(!processed.contains(t)) {
//						for(Resource s1 : sources) {
						for(int j=0; j<NEG; j++) {
							Resource s1 = sources.get((int) (sources.size()*Math.random()));
							if(s1 == s) { j--; continue; }
							Couple c1 = new Couple(s1, t);
							for(AcidsSimilarity sim : sims)
								c1.setDistance(sim.getSimilarity(s1.getPropertyValue(sim.getProperty().getName()),
										t.getPropertyValue(sim.getProperty().getName())), sim.getIndex());
							measures.estimateMissingValues(c1);
							c1.setPositive(false);
							neglbl.add(c1);
							inferred.add(c1);
						}
						processed.add(t);
					}
				} else
					neglbl.add(c);
			
			inferred.addAll(labelled);
						
			System.out.println("Labeled pos: "+poslbl.size());
			System.out.println("Labeled neg: "+neglbl.size());
			
			if(!m.trace(poslbl, neglbl)) {
				// ask for more
				continue;
			}
			eval.evaluateOn(inferred);

		}
		
		System.out.println("\nEvaluating on filtered subset:");
		eval.labelAll(couples);
		eval.evaluateOn(couples);

		System.out.println("\nEvaluating on inferred subset:");
		eval.labelAll(inferred);
		eval.evaluateOn(inferred);

		System.out.println();
		eval.fastEvaluation(sources, targets);

	}
	
	private ArrayList<Couple> filtering(ArrayList<AcidsSimilarity> sims) {
		
		ArrayList<Couple> couples = new ArrayList<Couple>();
		ArrayList<AcidsRanking> ranking = new ArrayList<AcidsRanking>();
		
		double[] w = m.getWLinear();
		double[] means = measures.getMeans();
		for(int i=0; i<sims.size(); i++)
			if(sims.get(i).getFilter() != null && !(sims.get(i) instanceof AcidsNumericSimilarity))
				ranking.add(new AcidsRanking(sims.get(i), w[i] * means[i]));
		if(ranking.isEmpty()) {
			System.err.println("No filter available.");
			return null;
		}
		Collections.sort(ranking);
		System.out.println(ranking);
		
		AcidsSimilarity simPivot = ranking.get(0).getSim();
		AcidsProperty p = simPivot.getProperty();
		System.out.println("Processing similarity "+simPivot.getName()+" of "+p.getName());
		
//		double def = measures.computeThreshold(simPivot);
//		if(def == 0.0)
			double def = simPivot.getEstimatedThreshold();
		
		for(AcidsSimilarity sim : sims)
			sim.setComputed(false);
		simPivot.setComputed(true);

		couples = simPivot.getFilter().filter(sources, targets, p.getName(), def);
		System.out.println("thr = "+def+"\tsize = "+couples.size());
		return couples;
	}

	private void computeRemaining(ArrayList<AcidsSimilarity> sims, ArrayList<Couple> couples) {
		System.out.print("Computing similarities");
		for(AcidsSimilarity sim : sims) {
			AcidsProperty p = sim.getProperty();
			if(!sim.isComputed())
				for(Couple c : couples)
					c.setDistance(sim.getSimilarity(c.getSource().getPropertyValue(p.getName()), 
							c.getTarget().getPropertyValue(p.getName())), sim.getIndex());
			System.out.print(".");
		}
		System.out.println();
	}

	public AcidsMeasures getMeasures() {
		return measures;
	}

	public AcidsOracle getOracle() {
		return oracle;
	}

	public ArrayList<Resource> getSources() {
		return sources;
	}

	public ArrayList<Resource> getTargets() {
		return targets;
	}

	public int getK() {
		return trainingSetSize;
	}

	public SvmHandler getSvmHandler() {
		return m;
	}

	public int getMapSize() {
		return mapSize;
	}
	
	
}
