package de.uni_leipzig.simba.learning.acids.algorithm;

public abstract class AcidsStringSimilarity extends AcidsSimilarity {
	
	public AcidsStringSimilarity(AcidsProperty property, int index) {
		super(property, index);
	}

	public int getDatatype() {
		return AcidsDatatype.TYPE_STRING;
	}
	
}
