package de.uni_leipzig.simba.learning.acids.algorithm;

import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;

public class AcidsCosineSimilarity extends AcidsStringSimilarity {

	
	public AcidsCosineSimilarity(AcidsProperty property, int index) {
		super(property, index);
	}

	@Override
	public String getName() {
		return "Cosine Similarity";
	}

	@Override
	public int getDatatype() {
		return AcidsDatatype.TYPE_STRING;
	}

	@Override
	public AcidsProperty getProperty() {
		return property;
	}

	@Override
	public double getSimilarity(String a, String b) {
		CosineSimilarity cs = new CosineSimilarity();
		double sim = cs.getSimilarity(a, b);
//		if(Double.isNaN(sim))
//			return 0.0;
		return sim;
	}

	@Override
	public AcidsFilter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}
