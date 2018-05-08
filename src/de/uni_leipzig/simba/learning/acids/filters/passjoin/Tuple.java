package de.uni_leipzig.simba.learning.acids.filters.passjoin;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 * @param <X>
 * @param <Y>
 */
public class Tuple<X, Y> {
	private final X x;
	private final Y y;
	
	public X getX() {
		return x;
	}

	public Y getY() {
		return y;
	}

	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "("+x+", "+y+")";
	}
	
}