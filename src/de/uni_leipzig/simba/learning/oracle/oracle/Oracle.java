package de.uni_leipzig.simba.learning.oracle.oracle;

import de.uni_leipzig.simba.data.Mapping;

/** Basic idea is that the interface can load reference data and act as a user in simulations
 * This data will be mostly given as a mapping 
 * @author ngonga
 *
 */

public interface Oracle {
	/** Returns true if the mapping contains the two URIs, else false
	 * 
	 * @param uri1 First instance in instance pair
	 * @param uri2 Second instance in instance pair
	 * @return
	 */
	public boolean ask(String uri1, String uri2);
	public void loadData(Mapping m);
        public int size();
        public Mapping getMapping();
        public String getType();
}
