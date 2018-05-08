package de.uni_leipzig.simba.genetics.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.uni_leipzig.simba.genetics.util.Pair;

public class PairTest {
	Pair<String> p1 = new Pair<String>("a", "b");
	Pair<String> p2 = new Pair<String>("a", "b");
	Pair<String> p3 = new Pair<String>("a", "a");
	@Test
	public void testEquals() {

		assertTrue(p1.equals(p2));
		assertTrue(p2.equals(p1));
		assertFalse(p1.equals(p3));
	}
	@Test
	public void testList() {
		List<Pair<String>> list = new ArrayList<Pair<String>>();
		list.add(p1);
		assertTrue(list.contains(p2));
		assertFalse(list.contains(p3));
		list.add(p2);
		assertTrue(list.size() == 2);
	}
}
