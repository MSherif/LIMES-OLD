package de.uni_leipzig.simba.genetics.evaluation.pseudomeasures;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import de.uni_leipzig.simba.genetics.evaluation.pseudomeasures.EvaluationPseudoMemory;

public class TestEvaluationPseudoMemory {
	EvaluationPseudoMemory e1 = new EvaluationPseudoMemory(2, 1, null, 0d, 1d, 10l);
	EvaluationPseudoMemory e2 = new EvaluationPseudoMemory(3, 1, null, 1d, 0d, 20l);
	EvaluationPseudoMemory e3 = new EvaluationPseudoMemory(2, 2, null, 0.5d, 0.5d, 15l);
	EvaluationPseudoMemory e4 = new EvaluationPseudoMemory(2, 1, null, 0.5d, 0.5d, 15l);
	
	@Test
	public void testCompareTo() {
		
		assertTrue(e1.compareTo(e4) == 0);
		assertTrue(e1.compareTo(e2) < 0);
		assertTrue(e1.compareTo(e3) < 0);
		assertTrue(e2.compareTo(e1) > 0);
	}
	@Test
	public void testListSort() {
		List<EvaluationPseudoMemory> list = new LinkedList<EvaluationPseudoMemory>();
		list.add(e4);
		list.add(e2);
		list.add(e1);
		list.add(e3);
		System.out.println(list);
		Collections.sort(list);
		System.out.println(list);
		assertTrue(list.get(0).equals(e1) || list.get(0).equals(e4));
		assertTrue(list.get(1).equals(e1) || list.get(1).equals(e4));
		assertTrue(list.get(2).equals(e2));
		assertTrue(list.get(3).equals(e3));
	}
}
