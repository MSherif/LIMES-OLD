/**
 * 
 */
package de.uni_leipzig.simba.multilinker;

import java.util.TreeSet;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;

/**
 * @author sherif
 *
 */
public class MultilinkToyData extends MultiInterlinker {

	public void testTOyData() {

		// 1. Fill caches
		Cache[] cache= new MemoryCache[5];
		for(int i=0 ; i<5 ; i++){
			cache[i] = ToyData.generateToyData(i);
			System.out.println(cache[i]);
			System.out.println();
		}
		datasetSize = cache[0].getAllInstances().size();

		for(int cycle=0 ; cycle<2; cycle++){

			// 2. Compute all unsupervised mappings M[i,j] for all i,j
			Mapping[][] m = new Mapping[5][5];
			for (int i = 0; i < m.length; i++) {
				for (int j = i + 1; j < m.length; j++) {
					m[i][j] = getDeterministicUnsupervisedMappings(cache[i],cache[j]);
					m[j][i] = m[i][j].reverseSourceTarget();
					results.put("P of M[" + i + "," + j + "]", MappingMath.computeFMeasure(m[i][j], datasetSize));
					results.put("R of M[" + i + "," + j + "]", MappingMath.computeRecall(m[i][j], datasetSize));
					results.put("F of M[" + i + "," + j + "]", MappingMath.computeFMeasure(m[i][j], datasetSize));
					System.out.println("M["+i+","+j+"]");
					System.out.println(m[i][j]);
				}
			}

			// 3. Compute all voting matrices V[i,j] for all i,j
			Mapping[][] v = new Mapping[5][5];
			v = getAllVotingScores(m, true);

			// 4. Get wrong mapping W[i,j] for each M[i,j]
			Mapping[][] w = findAllWrongMapping(v, 5, 2);

			// 5. Fix wrong mapping 
//			for(int i=0 ; i<w.length ; i++){
//				for(int j=i+1 ; j<w[0].length ; j++){
//					_fixCaches(w[i][j], m, cache, i, j);
//				}
//			}
			_fixCaches(w[0][1], m, cache, 0, 1);
		}
//		printResults();
	}
	
	/**
	 * @param m[][] for all datasets
	 * @return v as votingMatrix for all i,j
	 * @author Sherif
	 */
	protected Mapping[][] getAllVotingScores(Mapping[][] m, boolean useScores) {
		Mapping[][] v = new Mapping[m.length][m[1].length];
		for(int i=0 ; i<v.length ; i++){
			for(int j=i+1 ; j<v[i].length ; j++){
				v[i][j] = getVotingScores(m, i, j, useScores);
				v[j][i] = getVotingScores(m, j, i, useScores);
				System.out.println("V["+i+","+j+"]");
				System.out.println(v[i][j]);
			}
		}
		return v;
	}
	
	
	/**
	 * @param m[][] for all datasets
	 * @param sourceMapIndex i
	 * @param targetMapIndex j
	 * @return votingMatrix = m[i][j] + m[i][k] * m[k][j] for all k!=i & k!=j
	 * @author Sherif
	 */
	protected Mapping getVotingScores(Mapping[][] m, int sourceMapIndex, int targetMapIndex, boolean useScores) {
		Mapping votingMatrix = new Mapping();
		votingMatrix = m[sourceMapIndex][targetMapIndex];
		//		System.out.println("AA: "+votingMatrix.size);
		for (int k = 0; k < m.length; k++) {
			if (k != sourceMapIndex && k != targetMapIndex) {
				votingMatrix = MappingMath.add(votingMatrix, MappingMath.multiply(m[sourceMapIndex][k], m[k][targetMapIndex], useScores), useScores);
				//				System.out.println("k: "+votingMatrix.size);
			}
		}
		//		System.out.println("final: "+votingMatrix.size);
		//		System.exit(1);
		return votingMatrix;
	}
	
	/**
	 * @param votingMap V
	 * @param n number of datasets 
	 * @param k reverse acceptance threshold
	 * @return for each V[i,j] return W[i,j] containing all wrong mapping under threshold (n-k)
	 * @author sherif
	 */
	protected Mapping[][] findAllWrongMapping(Mapping[][] votingMap, int n, int k) {
		Mapping[][] w = new Mapping[votingMap.length][votingMap[0].length];
		for (int i = 0; i < votingMap.length; i++) {
			for (int j = i + 1; j < votingMap[0].length; j++) {
				w[i][j] = findWrongMapping(votingMap[i][j], n, k);
				System.out.println("W["+i+","+j+"]=");
				System.out.println(w[i][j]);
			}
		}
		return w;
	}
	
	protected Mapping findWrongMapping(Mapping votingMap, int n, int k) {
		Mapping result = new Mapping();
		for (String mapSourceUri : votingMap.map.keySet()) {
			for (String mapTargetUri : votingMap.map.get(mapSourceUri).keySet()) {
				Double sim = votingMap.getSimilarity(mapSourceUri, mapTargetUri);
				if (sim < (n - k)) {
					result.add(mapSourceUri, mapTargetUri, 1d);
				}
			}
		}
		return result;
	}
	
	/**
	 * @param mapping
	 * @param m
	 * @param cache
	 * @param i
	 * @param j
	 * @author sherif
	 */
	protected void _fixCaches(Mapping w, Mapping[][] m, Cache[] cache, int i, int j) {
		double sim=0d, maxSim=0d;
		int bestK=0;
		for (String wrongMappingSourceUri : w.map.keySet()) {
			//			for (String wrongMappingTargetUri : w.map.get(wrongMappingSourceUri).keySet()) { 

			for (int k = 0; k < m.length; k++) {
				if (k != i && k != j) {	
					for (String mIKSourceUri : m[i][k].map.keySet()) {
						for (String mIKTargetUri : m[i][k].map.get(mIKSourceUri).keySet()) {
							sim = m[i][k].getSimilarity(wrongMappingSourceUri, mIKTargetUri);
							if(sim > maxSim){
								maxSim = sim;
								bestK = k;
							}
						}
					}
				}
			}
			System.out.println("bestk: "+bestK);
			// fix wrongMappingSourceUri exist in m[i][j] by replacing its properties by the ones in m[i][bestK] 
			// in other words: replacing properties of cache[i] by ones of cache[bestK]
			for(String property: cache[i].getAllProperties()){
				if(cache[bestK].getInstance(wrongMappingSourceUri) != null){
					TreeSet<String> kValues = cache[bestK].getInstance(wrongMappingSourceUri).getProperty(property);
					TreeSet<String> iValues =cache[i].getInstance(wrongMappingSourceUri).getProperty(property);// can be removed later, just here for verification 
					cache[i].getInstance(wrongMappingSourceUri).replaceProperty(property,kValues);
					System.out.println("i lose, Replacing "+ iValues +" --> "+ kValues);
				}
			}
		}
		//		}
	}
	public static void main(String args[]) {
		//		testIteration();
		MultilinkToyData multInterlinker = new MultilinkToyData();
		multInterlinker.testTOyData();
	}
}
