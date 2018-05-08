package de.uni_leipzig.simba.learning.acids.algorithm;

import java.util.ArrayList;

import de.uni_leipzig.simba.learning.acids.data.Couple;

public class AcidsOracle {

	private ArrayList<String> oraclesAnswers;
	
	public AcidsOracle(ArrayList<String> oraclesAnswers) {
		this.oraclesAnswers = oraclesAnswers;
	}
    
	public boolean ask(String ids) {
		return oraclesAnswers.contains(ids);
	}

	public boolean ask(Couple c) {
		boolean feedback = ask(c.getID());
		c.setPositive(feedback);
		return feedback;
	}

	public ArrayList<String> getOraclesAnswers() {
		return oraclesAnswers;
	}	
	
}
