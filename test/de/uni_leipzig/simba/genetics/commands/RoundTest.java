package de.uni_leipzig.simba.genetics.commands;

import java.math.BigDecimal;

import org.junit.Test;

public class RoundTest {
	
	
	@Test
	public void testRound() {
		double d1 = 0.123956789d;
		double d2 = 0.01010100101010101010d;
		
		
		BigDecimal bd =	new BigDecimal( d1 );
		bd = bd.setScale(4, BigDecimal.ROUND_HALF_EVEN);
		System.out.println(d1);
		System.out.println(bd);
		System.out.println(bd.doubleValue());
	}
}
