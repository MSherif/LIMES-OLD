package de.uni_leipzig.simba.genetics.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
/**
 * <p>
 * Class to get the most controversy matches out of n <code>Mappings</code>. The
 * most controversy matches are those contained in close to n/2 <code>Mappings</code>
 * and vice versa.
 * This functionality is required by the Active Learning approach of the 
 * genetics package.
 * </p>
 * <p>
 * NOTE: Functionality requires <code>de.uni_leipzig.simba.data.Triple.class</code> to implement the
 * <code>Comparable</code> interface and overrides <code>hashCode()</code> function such that a 
 * <code>Triple</code> t1 is considered to equals another <code>Triple</code> t2 iff both source and 
 * target URIs are equal.
 * </p> 
 * @author Klaus Lyko
 */
public class ALDecider {
	
	static Logger logger = Logger.getLogger("LIMES");
	
	/* Remember already retrieved Triples, to avoid asking about them twice. */
	public  HashSet<Triple> retrieved = new HashSet<Triple>();
	/* limit number of instances in maps to get counter for */
	public int maxCount = 5000;
	/**
	 * Method returns the controversy matches of the given <code>Mapping</code>. This is just a 
	 * <code>HashMap</code>, whereas the keys are the <code>Triples</code> and the values the number of
	 * <code>Mappings</code> holding this <code>Triple</code>.
	 * @param mapList A List of all Mappings to process.
	 * @return <code>HashMap</code> a map of <code>Triples</code> (matches) and the number of <code>Mappings</code> holding them.
	 */
	public HashMap<Triple, Integer> getControversyMatches(List<Mapping> mapList) {
		HashMap<Triple, Integer> answer = new HashMap<Triple, Integer>();
		for(Mapping m : mapList) { // for all Mapping
			int counter = 0;
			for(String key : m.map.keySet()) {// and all Matches <key>-<value> within them
				if(counter < maxCount)
				for(String value : m.map.get(key).keySet()) {
					Triple t = new Triple(key, value, 1f); // construct Triple
					if(!retrieved.contains(t)) // already have the triple answered some time ago
						if(answer.containsKey(t)) { // if we already have the match <key>-<value>
							int count = answer.get(t);// increment its counter
							answer.put(t, count+1);
						}else {// otherwise add the match 
							answer.put(t, 1);
						}
					counter++;
				}
				
			}
		}
		return answer;
	}	
	
	/**
	 * Function to get the n most controversy matching candidates of the given mappings. The most controversy once
	 * are those, who are only in half of the given mappings.
	 * @param mapList List of Mappings.
	 * @param n Controls how many candidates should be retrieved.
	 * @return
	 */
	public List<Triple> getControversyCandidates(List<Mapping> mapList, int n) {
		logger.error("get "+n+" most controversy matches from "+mapList.size()+" Mappings");
		Map<Integer, Set<Triple>> answer = new HashMap<Integer, Set<Triple>> ();
		// initialize Map and Set
		for(int i = 1; i<=mapList.size(); i++)
			answer.put(i, new HashSet<Triple>());
		Map<Triple, Integer> sub = getControversyMatches(mapList);
		for(Entry<Triple, Integer> e : sub.entrySet()) {
			answer.get(e.getValue()).add(e.getKey());
		}
		logger.error("numbered controversy Matches with "+sub.size()+" triples. Putting them to "+answer.size()+" indices.");
		List<Triple> tripleList= new LinkedList<Triple>();
		int center = mapList.size()/2;
		int minDist = mapList.size(); // at most all maps contain a match
		while(tripleList.size() < Math.min(n, sub.size()) && !answer.isEmpty()) {
			// we look for those as close to center as possible
			boolean found = false;
			int MapsForFound = 0;
			for(int d : answer.keySet()) {
				if(Math.abs(d - center) <= minDist) 
					minDist = d;
					found = true;
					MapsForFound = d;
				
			}

			HashSet<String> retrievedIDs = new HashSet<String>();
			if(found && answer.containsKey(minDist)) {
				for(Triple t : answer.get(minDist)) {
					if(tripleList.size() < Math.min(n, sub.size())) {
						if(!retrievedIDs.contains(t.getSourceUri())) {
							t.setSimilarity((float)MapsForFound/(float)mapList.size());
							tripleList.add(t);
							retrieved.add(t); // remember the triple, to don't ask about it twice
							retrievedIDs.add(t.getSourceUri());
						}
					}
					else  {
						return tripleList;
					}					
				}
				answer.remove(minDist);
			}
			//reset center & dist
			center = mapList.size()/2;
			minDist = mapList.size(); // at most all maps contain a match			
		}
		logger.info("Controversy matches: "+tripleList);
//		if(tripleList.size() == 0)
//			System.exit(1);
		return tripleList;
	}
	/**
	 * 
	 * @param mapList
	 * @return tripleList with informative value
	 */
	public List<Triple> getControversyCandidates(List<Mapping> mapList) {
		
		Map<Integer, Set<Triple>> answer = new HashMap<Integer, Set<Triple>> ();
		// initialize Map and Set
		for(int i = 1; i<=mapList.size(); i++)
			answer.put(i, new HashSet<Triple>());
		Map<Triple, Integer> sub = getControversyMatches(mapList);
		for(Entry<Triple, Integer> e : sub.entrySet()) {
			answer.get(e.getValue()).add(e.getKey());
		}
		logger.info("numbered controversy Matches with "+sub.size()+" triples. Putting them to "+answer.size()+" indices.");
		List<Triple> tripleList= new LinkedList<Triple>();
		int center = mapList.size()/2;
		int minDist = mapList.size(); // at most all maps contain a match
		while(tripleList.size() < sub.size() && !answer.isEmpty()) {
			// we look for those as close to center as possible
			boolean found = false;
			int MapsForFound = 0;
			for(int d : answer.keySet()) {// 1 ... mapList.size()
				if(Math.abs(d - center) <= minDist)
					minDist = d;
					found = true;
					MapsForFound = d;
			}
			if(found && answer.containsKey(minDist)) {
				for(Triple t : answer.get(minDist)) {
					if(tripleList.size() <  sub.size()) {
						t.setSimilarity((float)MapsForFound/(float)mapList.size());
						tripleList.add(t);
						//retrieved.add(t); // I want all triples the  
						//TODO triples which were asked should be set before this method is called
					}
					else  {
						return tripleList;
					}					
				}
				answer.remove(minDist);
			}
			//reset center & dist
			center = mapList.size()/2;
			minDist = mapList.size(); // at most all maps contain a match			
		}
		//logger.info("Controversy matches: "+tripleList);
		if(tripleList.size() == 0)
			System.exit(1);
		return tripleList;
	}
	
	
	public static void main(String[] args) {
		Mapping a = new Mapping();
		Mapping b = new Mapping();
		Mapping c = new Mapping();
		Mapping d = new Mapping();
//		Mapping e = new Mapping();
//		Mapping f = new Mapping();
		a.add("a", "y", 1);
		a.add("d", "f", 8);
		
		b.add("a", "y", 1);
		b.add("a", "x", 4);
		
		c.add("c", "y", 5);
		c.add("d", "f", 5);
		
		d.add("D", "D", 77);
		d.add("a", "y", 1);
		d.add("a", "x", 4);
		List<Mapping> mapList = new LinkedList<Mapping>();
		mapList.add(a);
		mapList.add(b);
		mapList.add(c);
	//	mapList.add(c);
		mapList.add(d);

		ALDecider aLD = new ALDecider();
	//	aLD.setKnown(c);
		List<Triple> result;
		HashMap<Triple, Integer> result0 = aLD.getControversyMatches(mapList);
		System.out.println("aLD.getControversyMatches(mapList)\n"+result0);
		result = aLD.getControversyCandidates(mapList, 1);
		System.out.println(result);
		aLD.setKnown(new Mapping());
		aLD.retrieved.clear();
		
		result = aLD.getControversyCandidates(mapList, 2);
		System.out.println(result);
		result = aLD.getControversyCandidates(mapList, 8);
		System.out.println(result);
	}
	
	/**
	 * To set already retrieved instances. For example those provided at start up.
	 * @param m Mapping of already asked URIs of source and target.
	 */
	public void setKnown(Mapping m) {
		for(String a : m.map.keySet())
			for(String b : m.map.get(a).keySet())
				if(a != null && b!= null)
					retrieved.add(new Triple(a, b, 1));
				
	}
} 