package de.uni_leipzig.simba.learning.refinement.evaluation;

import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;

public class EvaluationMemory {
	double realf = 0d;
	public double realfFull = 0d;
	double realPrec = 0d;
	public double realPrecFull;
	double realRecall = 0d;
	public double realRecallFull = 0d;
	public SearchTreeNode node = null;
	public double pfm = 0d;
	int loop = 0;
	long duration = 0;
	public double truePositives = 0;
	public double trueNegatives = 0;
	public double falsePositives = 0;
	public double falseNegatives = 0;
	
	
	public EvaluationMemory(SearchTreeNode node, int loop, long dur) {
		this.loop = loop;
		this.node = node;
		this.duration = dur;
	}
	
	public void setRealF(double FScore) {
		this.realf = FScore;
	}
	public void setRealPrec(double prec) {
		this.realPrec = prec;
	}
	public void setRealRecall(double recall) {
		this.realRecall = recall;
	}
	
	
	public void setRealfFull(double FScore) {
		this.realfFull = FScore;
	}
	public void setRealPrecFull(double prec) {
		this.realPrecFull = prec;
	}
	public void setRealRecallFull(double recall) {
		this.realRecallFull = recall;
	}
}
