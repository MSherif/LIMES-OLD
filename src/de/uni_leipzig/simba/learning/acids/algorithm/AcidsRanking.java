package de.uni_leipzig.simba.learning.acids.algorithm;

public class AcidsRanking implements Comparable<AcidsRanking> {

	private AcidsSimilarity sim;
	private Double weight;
	
	
	public AcidsRanking(AcidsSimilarity sim, Double weight) {
		super();
		this.sim = sim;
		this.weight = weight;
	}

	@Override
	public int compareTo(AcidsRanking o) {
		// inverse order!
		return Double.compare(o.getWeight(), this.getWeight());
	}


	public AcidsSimilarity getSim() {
		return sim;
	}


	public Double getWeight() {
		return weight;
	}
	
	public String toString() {
		return sim.getName()+" | "+sim.getProperty().getName()+" | "+weight;
	}

}
