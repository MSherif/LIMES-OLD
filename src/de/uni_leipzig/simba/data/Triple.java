/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.data;

/**
 * Entry for a mapping. Maps two URIs with a similarity.
 * @author ngonga, Klaus Lyko
 */
public class Triple {
    String targetUri;
    String sourceUri;
    float similarity;

    public Triple(String source, String target, float sim)
    {
        targetUri = target;
        sourceUri = source;
        similarity = sim;
    }

    @Override
    public String toString()
    {
        String s="";
        s = "<"+sourceUri+">";
        s = s + " <"+targetUri+"> ";
        s = s + similarity;
        return s;
    }
    
    /**
     * Getter for the URI of the source kb.
     * @return The URI.
     */
	public String getSourceUri() {
		return sourceUri;
    }
	
	/**
    * Getter for the URI of the source kb.
    * @return The URI.
    */
	public String getTargetUri() {
    	return targetUri;
    }
	
	/**
	 * Getter for the similarity of both instances represented by the URIs.
	 * @return The similarity if it was set, null otherwise.
	 */
	public float getSimilarity() {
		return similarity;
	}
	
	@Override
	public int hashCode() {
		return this.sourceUri.hashCode()+targetUri.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof Triple) {
			Triple t = (Triple) o;
			if(this.sourceUri.equals(t.sourceUri) && this.targetUri.equals(t.targetUri))
			{	return true;}
		}
		return false;
	}
	
	public void setSimilarity(float f) {
		this.similarity=f;
	}
}
