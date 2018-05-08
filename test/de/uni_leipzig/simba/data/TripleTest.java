package de.uni_leipzig.simba.data;

import static org.junit.Assert.*;

import org.junit.Test;

import de.uni_leipzig.simba.data.Triple;

public class TripleTest {
	@Test
	public void testEqulas() {
		Triple t1  = new Triple("a","b",1f);
		Triple t1a = new Triple("a", "b", 0.477f);
		Triple t2  = new Triple("c","b",44f);
		Triple t3 = new Triple("b", "a", 555f);
		assertEquals(t1, t1a);
		assertFalse(t1.equals(t2));
		assertFalse(t1.equals(t3));
	}
}
