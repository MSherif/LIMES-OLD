package de.uni_leipzig.simba.io.serializer;

import java.util.HashMap;
import java.util.Map.Entry;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.Serializer;
import de.uni_leipzig.simba.io.SerializerFactory;

public class CSVSerializer extends TabSeparatedSerializer {
	 public String SEPARATOR = ",";
	 
	 public String getName() {
	        return "CommaSeparatedSerializer";
	 }
	 
	 @Override
	 public void printStatement(String subject, String predicate, String object, double similarity) {
	    try {
	             writer.println("\"" + subject + "\"" + SEPARATOR + "\"" + object + "\"" + SEPARATOR + similarity);
	    }
	    catch (Exception e) {
	             logger.warn("Error writing");
	    }
	 }
	 
	 public String getFileExtension() {
	    	return "csv";
	 }
	 
	 public static void main(String args[]) {
		 Mapping m = new Mapping();
		 m.add("foo:a", "foo:b", 1d);
		 m.add("aa", "bb", 1d);
		 m.add("foo:aaaa", "foo:bb", 0.8d);
		 
		 Serializer serial = SerializerFactory.getSerializer("csv");
		 
		 String fileName   = System.getProperty("user.home")+"/";
			fileName += "test";
			serial.open(fileName);
			String predicate = "foo:sameAs";
			HashMap<String,String> prefixes = new HashMap<String, String>();
			prefixes.put("foo", "http://example.com/");
			serial.setPrefixes(prefixes);
			for(String uri1 : m.map.keySet()) {
				for(Entry<String, Double> e : m.map.get(uri1).entrySet()) {
					serial.printStatement(uri1, predicate, e.getKey(), e.getValue());
				}
			}
			serial.close();
	 }
}
