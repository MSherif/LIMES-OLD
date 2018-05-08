package de.uni_leipzig.simba.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeMap;

public class ThresholdPlotData {
	public String name;
	public final static String SEP =";";
	TreeMap<Double, TreeMap<Double, Double>> data = new TreeMap();
	/*
	 * 	1, 2, 3, 4, 5
	 * 1
	 * 2
	 * 3
	 * 4
	 * 5
	 */
	
	/**
	 * Creates an copy of this instance.
	 * @return A ThresholdPlotData instance holding the same data as this.
	 */
	public ThresholdPlotData copy() {
		ThresholdPlotData copy = new ThresholdPlotData();
		copy.name = name;
		Iterator<Double> keyit = data.navigableKeySet().iterator();
		while(keyit.hasNext()) {
			double thres1 = keyit.next();
			Iterator<Double> keyit2 = data.get(thres1).navigableKeySet().iterator();
			while(keyit2.hasNext()) {
				Double thres2 = keyit2.next();
				copy.addEntry(thres1, thres2, data.get(thres1).get(thres2));
			}
		}
		return copy;
	}
	
	public void addEntry(Double threshold1, Double threshold2, Double fScore) {
		TreeMap<Double, Double> entries = new TreeMap();
		if(data.containsKey(threshold1)) {
			entries = data.get(threshold1);
		}
		entries.put(threshold2, fScore);
		data.put(threshold1, entries);
	}
	
	public void writeData(FileWriter fw) throws IOException {
		fw.write(name);
		fw.write(System.getProperty("line.separator"));
		fw.write("+++++++++++++++++++++++++++++++++++++++++++");
		fw.write(System.getProperty("line.separator"));
		fw.flush();
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,##0.00" );
		
		//first write head of colums
		Entry<Double, TreeMap<Double,Double>> first = data.firstEntry();
		Iterator<Double> keyitHead = first.getValue().navigableKeySet().iterator();
		String head = SEP;
		while(keyitHead.hasNext()) {
			head += df.format(keyitHead.next())+SEP;
		}
//		System.out.println(head);		
		fw.write(head);
		fw.write(System.getProperty("line.separator"));
		fw.flush();
		Iterator<Double> keyit = data.navigableKeySet().iterator();
		while(keyit.hasNext()) {
			double thres1 = keyit.next();
			Iterator<Double> keyit2 = data.get(thres1).navigableKeySet().iterator();
			String line = ""+df.format(thres1)+SEP;
			while(keyit2.hasNext()) {
				Double thres2 = keyit2.next();
				line+=df.format(data.get(thres1).get(thres2))+SEP;
			}
			fw.write(line);
			fw.write(System.getProperty("line.separator"));
			fw.flush();
//			System.out.println(line);
		}
	}
	
	
	public void printData() {
		System.out.println(name);
		System.out.println("############################################");
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,##0.00" );
		
		//first write head of colums
		Entry<Double, TreeMap<Double,Double>> first = data.firstEntry();
		Iterator<Double> keyitHead = first.getValue().navigableKeySet().iterator();
		String head = "\t";
		while(keyitHead.hasNext()) {
			head += df.format(keyitHead.next())+":\t";
		}
		System.out.println(head);		
		
		Iterator<Double> keyit = data.navigableKeySet().iterator();
		while(keyit.hasNext()) {
			double thres1 = keyit.next();
			Iterator<Double> keyit2 = data.get(thres1).navigableKeySet().iterator();
			String line = ""+df.format(thres1)+":\t";
			while(keyit2.hasNext()) {
				Double thres2 = keyit2.next();
				line+=df.format(data.get(thres1).get(thres2))+"\t";
			}
			System.out.println(line);
		}
	}
	
	public void testFill() {
		name = "test fill";
		for(int i = 1; i<=10; i++) {
			for(int j = 1; j<=10; j++) {
				addEntry(0.1*i, 0.1*j, (0.1*i*j));
			}
		}
	}
	
	public static void main(String args[]) {
		ThresholdPlotData plot = new ThresholdPlotData();
		plot.testFill();
		try {
			FileWriter fw = new FileWriter(new File("resources/testWrite.csv"), false);
	
			plot.writeData(fw);
			plot.printData();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
