package de.uni_leipzig.simba.genetics.core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.data.Triple;
import de.uni_leipzig.simba.genetics.core.ALDecider;

public class ALDeciderTest {

	private static List<Mapping> getExamples() {
		Mapping a = new Mapping();
		Mapping b = new Mapping();
		Mapping c = new Mapping();
		Mapping d = new Mapping();
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
		mapList.add(d);
		return mapList;
	}
	
	@Test
	public void testGetControVersyCandidates() {
		ALDecider aLD = new ALDecider();
		List<Triple> result = aLD.getControversyCandidates(getExamples(), 1);
		System.out.println("testGetControVersyCandidates 1:\n"+result);
		assertTrue(result.size() == 1);
		assertTrue(result.get(0).getSimilarity()==0.75f); //das ist falsch!
		
		result = aLD.getControversyCandidates(getExamples(), 2);
		System.out.println("testGetControVersyCandidates 2:\n"+result);
		assertTrue(result.size() == 2);
		assertTrue(result.get(0).getSimilarity()==0.5f);
		assertTrue(result.get(1).getSimilarity()==0.5f);
	}
	
	@Test
	public void testGetControVersyMatches( ) {
		ALDecider aLD = new ALDecider();		
		HashMap<Triple, Integer> numbered = aLD.getControversyMatches(getExamples());
		assertTrue(numbered.size()<= 5);
		assertTrue(numbered.get(new Triple("a","y",1f)) == 3);
		assertTrue(numbered.get(new Triple("a","x",1f)) == 2);
		assertTrue(numbered.get(new Triple("d","f",1f)) == 2);
	}
}
