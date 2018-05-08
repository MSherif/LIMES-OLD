package de.uni_leipzig.simba.genetics.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.uni_leipzig.simba.genetics.commands.AddMetric;

public class AddMetricTest {

	@Test
	public void testremoveThresholdFromMeasure() {
		String ex1 = "trigrams(s.y,t.x)";
		String ex2 = "trigrams(s.y,t.x)|0.8";
		assertEquals(AddMetric.removeThresholdFromMeasure(ex1),AddMetric.removeThresholdFromMeasure(ex2));
	}
}
