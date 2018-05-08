package de.uni_leipzig.simba.learning.acids.utility;

import java.util.Comparator;

import de.uni_leipzig.simba.learning.acids.data.Couple;

public class GammaComparator implements Comparator<Couple> {
	
	public GammaComparator() {
		super();
	}
	
	@Override
	public int compare(Couple o1, Couple o2) {
		double g1 = o1.getGamma();
		double g2 = o2.getGamma();
		if(g1 < g2)
			return -1;
		if(g1 > g2)
			return 1;
		return 0;
	}
}
