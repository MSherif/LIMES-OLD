package de.uni_leipzig.simba.genetics.learner;

import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.selfconfig.Measure;
import de.uni_leipzig.simba.selfconfig.SizeAwarePseudoFMeasure;

public class UnSupervisedLearnerParameters extends LearnerParameters {
	
	public UnSupervisedLearnerParameters(ConfigReader cR,
			PropertyMapping propMap) {
		super(cR, propMap);
	}

	private Measure pseudoFMeasure = new SizeAwarePseudoFMeasure();
	private double PFMBetaValue = 1.0d;

	/**
	 * @return the pseudoFMeasure
	 */
	public Measure getPseudoFMeasure() {
		return pseudoFMeasure;
	}

	/**
	 * @param pseudoFMeasure the pseudoFMeasure to set
	 */
	public void setPseudoFMeasure(Measure pseudoFMeasure) {
		this.pseudoFMeasure = pseudoFMeasure;
	}

	/**
	 * @return the pFMBetaValue
	 */
	public double getPFMBetaValue() {
		return PFMBetaValue;
	}

	/**
	 * @param pFMBetaValue the pFMBetaValue to set
	 */
	public void setPFMBetaValue(double pFMBetaValue) {
		PFMBetaValue = pFMBetaValue;
	}
}
