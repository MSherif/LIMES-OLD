package de.uni_leipzig.simba.genetics.evaluation.statistics;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

/**
 * Class to compute mean, variance and standard derivation.
 * @author Klaus Lyko
 *
 */
public class Statistics {
	
	public int count = 0;
	public double sum = 0;
	public double sumOfSquanres = 0;
	public double mean = 0;
	public double standardDeviation = 0;
	public double variance = 0;
	public List<Double> elements = new LinkedList<Double>();

	/**
	 * Add a value, all aggregated values are updated accordingly.
	 * @param value
	 */
	public void add(double value) {
		count++;
		elements.add(value);
		this.sum += value;
	    this.sumOfSquanres += value * value;
	    this.mean += (value - mean) / count;
	    if(count > 1)
	    	this.standardDeviation = Math.sqrt((count * sumOfSquanres - sum*sum) / (count*(count-1)));
	    this.variance = this.standardDeviation * this.standardDeviation;
	}
	
	@Override
	public String toString() {
		return "statistics(#"+count+"): sum="+sum+"mean= "+mean+" std= "+standardDeviation+" var= "+variance;
	}
	
	@Test
	public void test1() {
		Statistics stat = new Statistics();
		stat.add(5);
		System.out.println(stat.mean);
		stat.add(3);
		System.out.println(stat.mean);
		stat.add(4);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		assertTrue(((stat.mean) - 2.4d ) <=0.001d);
	}
	
	@Test
	public void test2() {
		Statistics stat = new Statistics();
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(3);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(5);
		System.out.println(stat.mean);
		assertTrue(((stat.mean) - 2.4d ) <=0.001d);
	}
	
	@Test
	public void test3() {
		Statistics stat = new Statistics();
		stat.add(1);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		stat.add(0);
		System.out.println(stat.mean);
		assertTrue(((stat.mean) - (0.2) ) <=0.001d);
	}
	
	@Test
	public void test4() {
		Statistics stat = new Statistics();
		stat.add(1);
		for(int i = 0; i<1000; i++)
		stat.add(0);
		System.out.println(stat.mean);
		assertTrue(stat.mean >= 0d);
	}
	
	@Test
	public void test5() {
		Statistics stat = new Statistics();
		stat.add(0.5d);
		System.out.println("Only one entry...");
		System.out.println("std="+standardDeviation);
		assertTrue(!Double.isNaN(standardDeviation) && standardDeviation <= 0.00000001d);
	}
}
