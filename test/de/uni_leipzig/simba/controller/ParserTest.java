package de.uni_leipzig.simba.controller;

import org.junit.Test;

public class ParserTest {
	@Test
	public void testParsing() {
		String expr2 = "AND(AND(levenshtein(x.rdfs:label,y.rdfs:label)|1.0," +
				"levenshtein(x.dbp:nationality,y.dbp:nationality)|1.0)|0.5,levenshtein(x.dbp:name,y.dbp:name)|1.0)";
		String expr1 = "AND(levenshtein(x.rdfs:label,y.rdfs:label)|1.0," +
				"levenshtein(x.dbp:nationality,y.dbp:nationality)|1.0)";
		String expr3 = "AND(euclidean(x.year,y.year)|0.8019,OR(cosine(x.title,y.title)|0.5263,AND(cosine(x.authors,y.authors)|0.5263,overlap(x.title,y.title)|0.5263)|0.2012)|0.2012)";
		Parser p1 = new Parser(expr1, 1d);
		Parser p2 = new Parser(expr2, 1d);
		Parser p3 = new Parser(expr3, 1d);
	}
}
