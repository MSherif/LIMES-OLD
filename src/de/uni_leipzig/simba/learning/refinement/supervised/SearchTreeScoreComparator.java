package de.uni_leipzig.simba.learning.refinement.supervised;

import java.util.Comparator;

import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;

public class SearchTreeScoreComparator implements Comparator<SearchTreeNode>{

//	@Override
	public int compare(SearchTreeNode arg0, SearchTreeNode arg1) {
		return Double.compare(arg0.getScore(), arg1.getScore());
	}

}
