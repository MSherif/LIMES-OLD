package de.uni_leipzig.simba.learning.acids.algorithm;

import java.util.ArrayList;

public class AcidsProperty {

	private String name;
	private int datatype;
	private AcidsMeasures measures;

	private ArrayList<AcidsSimilarity> similarities = new ArrayList<AcidsSimilarity>();

	public ArrayList<AcidsSimilarity> getSimilarities() {
		return similarities;
	}

	public AcidsProperty(String name, int datatype, int index, AcidsMeasures measures) {
		super();
		this.name = name;
		this.datatype = datatype;
		this.measures = measures;
		
		switch(datatype) {
		case AcidsDatatype.TYPE_STRING:
			similarities.add(new AcidsWeightedNgramSimilarity(this, index));
			similarities.add(new AcidsCosineSimilarity(this, index + 1));
			similarities.add(new AcidsWeightedEditSimilarity(this, index + 2));
			break;
		case AcidsDatatype.TYPE_NUMERIC:
			similarities.add(new AcidsNumericSimilarity(this, index));
			break;
		case AcidsDatatype.TYPE_DATETIME: // TODO datetime similarity and filtering?
			similarities.add(new AcidsWeightedNgramSimilarity(this, index));
			similarities.add(new AcidsCosineSimilarity(this, index + 1));
			similarities.add(new AcidsWeightedEditSimilarity(this, index + 2));
			break;
		default:
			System.err.println("Error: Invalid datatype for property " + name + ".");
			break;
		}
		
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public AcidsMeasures getMeasures() {
		return measures;
	}

	public int getDatatype() {
		return datatype;
	}
	public void setDatatype(int datatype) {
		this.datatype = datatype;
	}
	
	public int getSize() {
		return similarities.size();
	}

}
