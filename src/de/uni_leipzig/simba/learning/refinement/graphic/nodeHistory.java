package de.uni_leipzig.simba.learning.refinement.graphic;

import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;

public class nodeHistory {
	String expression="";
	int expansion;
	double score;
	double bestScore;
	double threshold;
	int children;
	public int historyTiming; // this is to record the timing for the history event
	public nodeHistory(SearchTreeNode node)
	{
		expression= node.getSpec().getFilterExpression();
		score = node.getScore();
		bestScore = node.getBestScore();
		threshold = node.getSpec().threshold;
		expansion = node.getExpansion();
		children = node.getChildren().size();
	}
	
	@Override
	public String toString() {
		
		return super.toString();
	}

}
