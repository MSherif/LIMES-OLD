package de.uni_leipzig.simba.learning.refinement.graphic;

import de.uni_leipzig.simba.learning.refinement.SearchTreeNode;

public class GraphNode {

	/**
	 * @param args
	 */
	public String nodeId;
	public SearchTreeNode data;
	public String start="",end="";
	boolean vistited = false;
	public GraphNode(String nodeid, SearchTreeNode data)
	{
		this.nodeId=nodeid;
		this.data=data;
	}
	
		public static void main(String[] args) {
		// TODO Auto-generated method stub

		

	}

}
