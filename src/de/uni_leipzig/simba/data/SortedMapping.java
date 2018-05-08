package de.uni_leipzig.simba.data;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.uni_leipzig.simba.genetics.util.Pair;

/**
 * Class to transform a mapping in a sorted data structure
 * @author Lyko
 */
public class SortedMapping {
	Mapping base;
	TreeMap<Double, Pair<String>> sortedMapping;
	
	public SortedMapping(Mapping base) {
		this.base = base;
		sortedMapping = new TreeMap<Double, Pair<String>>();
	}
	
	public TreeMap<Double, Pair<String>> sort() {
		sortBase();
		return sortedMapping;
	}
	
	private void sortBase() {
		for(String s1 : base.map.keySet()) {
			for(Entry<String, Double> e2 : base.map.get(s1).entrySet()) {
				sortedMapping.put(e2.getValue(), new Pair<String> (s1, e2.getKey()));
			}
		}
	}
	
	public String toString() {
		String s = "";
		for(Entry<Double, Pair<String>> e : sortedMapping.descendingMap().entrySet()) {
			s += e.getKey() +" : "+e.getValue()+System.getProperty("line.separator");
		}
		return s;
	}
	
	public static void main(String args[]) {
		Mapping m = new Mapping();
		m.add("a", "b", 6);
		m.add("a", "c", 4);
		m.add("a", "d", 22);
		
		m.add("aa", "bb", 5);
		m.add("aaa", "bbb", 3);
		m.add("aaaa", "bbbb", 1);
		
		SortedMapping sortMap = new SortedMapping(m);
		sortMap.sort();
		System.out.println("Mapping:\n"+m);
		System.out.println("Sorted:\n"+sortMap);
		
		
	}
}
