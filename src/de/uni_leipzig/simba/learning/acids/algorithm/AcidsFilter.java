package de.uni_leipzig.simba.learning.acids.algorithm;

import java.util.ArrayList;

import de.uni_leipzig.simba.learning.acids.data.Couple;
import de.uni_leipzig.simba.learning.acids.data.Resource;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public abstract class AcidsFilter {
	
	protected AcidsSimilarity similarity;
	
	public AcidsFilter(AcidsSimilarity similarity) {
		this.similarity = similarity;
	}
	
	public abstract ArrayList<Couple> filter(ArrayList<Resource> sources,
			ArrayList<Resource> targets, String propertyName, double theta);
	
	public abstract ArrayList<Couple> filter(ArrayList<Couple> intersection, String propertyName, double theta);
	
	protected boolean verbose = true;
	
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
}
