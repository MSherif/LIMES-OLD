package de.uni_leipzig.simba.grecall.util;

public class DiffPair<X, Y> extends Object {
    public X left;
    public Y right;

    public DiffPair(X left, Y right) {
	this.left = left;
	this.right = right;
    }

    @Override
    public String toString() {
	return "(" + left + "," + right + ")";
    }

    public X getX() {
	return this.left;
    }

    public Y getY() {
	return this.right;
    }

    public void setX(X x) {
	this.left = x;
    }

    public void setY(Y y) {
	this.right = y;
    }

    @Override
    public boolean equals(Object other) {
	if (other == null) {
	    return false;
	}
	if (other == this) {
	    return true;
	}
	if (!(other instanceof DiffPair)) {
	    return false;
	}
	DiffPair<X, Y> other_ = (DiffPair<X, Y>) other;
	return this.equals(other_.left) && this.equals(other_.right);
    }

}
