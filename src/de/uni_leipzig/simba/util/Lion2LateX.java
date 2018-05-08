package de.uni_leipzig.simba.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
/**
 * Just a quick hack to read and format csv results for LION/EAGLE into Latex' tikzpicture figure format.
 * @author Klaus Lyko
 *
 */
public class Lion2LateX {
	File input;
	String  folder = "C:\\Users\\Lyko\\Desktop\\LION\\optimized_thresh_new\\";
//	String folder = "C:\\Users\\Lyko\\workspace2\\LIMES\\resources\\results\\";
	public Lion2LateX(String file) throws IOException {
		input = new File(folder+file);
	}
	
	public Lion2LateX(String folder, String file) throws IOException {
		this.folder = folder;
		input = new File(folder+file);
	}
	
	public List<String> readSortAndFormatEagle(File input) throws IOException {
		FileReader reader = new FileReader(input);
		BufferedReader br = new BufferedReader(reader);
		List<String> result = new LinkedList<String>();
		String s = br.readLine();
		s = br.readLine();
	
		List<LatexEntry> entries = new LinkedList<LatexEntry>();
		
	
		while(s!=null) {
//			System.out.println(s);
			String[] breaks = s.split(";");
			if(breaks.length >= 8) {
				
				if(Integer.parseInt(breaks[8])==5) {
					System.out.println("f="+breaks[1]+" dur="+breaks[7]+" cycle="+breaks[8]);
					try{
						LatexEntry entry = new LatexEntry(Double.parseDouble(breaks[7]), Double.parseDouble(breaks[1]));
						entries.add(entry);
					}catch(Exception e){e.printStackTrace();}
				}				
			} else {
//				System.out.println("Couldn't break s into 8 but "+breaks.length+": "+s);
			}
			s = br.readLine();
		}
		br.close();
		Collections.sort(entries);
		for(LatexEntry e : entries) {
			System.out.println(e);
			result.add(e.toString());
		}
		return result;
	}
	public List<String> readAndFormatEagle() throws IOException {
		return readSortAndFormatEagle(input);
	}

	/**
	 * Method to parse Lions files and transform them into Latex' tikzpicture figure format of
	 * data points.
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void writeTexesForLion(String source) throws IOException {
		FileReader reader = new FileReader(new File(source));
		BufferedReader br = new BufferedReader(reader);
		//begin 7th row
		List<String> result = new LinkedList<String>();
		String s = br.readLine();
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		for(int i=0; i<6; i++) {
			s = br.readLine();
			System.out.println(s);
		}
		
		df.applyPattern( "#,###,######0.0000" );
		DecimalFormat df1 =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		
		
		df1.applyPattern( "#,###,######0.000" );
		
		while(s!=null) {
//			System.out.println(s);
			String[] breaks = s.split(";");
			if(breaks.length >= 11) {
				result.add("("+df1.format(Double.parseDouble(breaks[3]))+","+df.format(Double.parseDouble(breaks[7]))+")");
				System.out.println("("+df1.format(Double.parseDouble(breaks[3]))+","+df.format(Double.parseDouble(breaks[7]))+")");
			}
			s = br.readLine();
		}
		br.close();
	}
/*
 * ---------------------------------------------- main	
 */
	
	public static void main(String args[]) throws IOException {
//		String folder = "C:\\Users\\Lyko\\Desktop\\LION_bacthPLUSPLUS\\unsup_noThresOpto\\";
//		folder = "C:\\Users\\Lyko\\Desktop\\LION_bacthPLUSPLUS\\";
//		String file = "EAGLE_PFM_Beta-1.0_MEAN_Pseudo_eval_Amazon-GoogleProducts.csv";
//		Lion2LateX fw = new Lion2LateX(folder, file);
//		fw.readAndFormatEagle();
		
		/** EUCLIDs */
//		String folder = "C:\\Users\\Lyko\\Desktop\\Work\\LION\\";
//		String file = "EuclidLog.csv";
//		Lion2LateX fw = new Lion2LateX(folder,file);
//		fw.readAndFormatEUCLID(new File(folder+file));
		/** EAGLEs */
		String folder =  "C:\\Users\\Lyko\\Desktop\\Work\\LION\\optimized_thresh_new\\";
		String file = "EAGLE_PFM_Beta-1.0_MEAN_Pseudo_eval_Restaurants.csv";
		Lion2LateX fw = new Lion2LateX(folder, file);
		fw.readAndFormatEagle();
		
		/**LIONS*/
//		String folder = "C:\\Users\\Lyko\\Desktop\\Work\\LION\\";
//		String file = "DBPedia-LinkedMDB_refine.csv";
//		Lion2LateX fw = new Lion2LateX(folder, file);
//		fw.writeTexesForLion(folder+file);
	}
	
	
	
/*
 * ---------------------------------------------- classes	
 */
	/**
	 * Class to simply sort eagles duration, FScore datapoints
	 * @author Klaus Lyko
	 *
	 */
	class LatexEntry implements Comparable{
		public double  dur = 0d;
		public double fMeasure = 0d;
		
		public LatexEntry(double dur, double f) {
			this.dur=dur;
			this.fMeasure=f;
		}
		
		@Override
		public int compareTo(Object other) {
			LatexEntry o = (LatexEntry) other;
			double diff = this.dur - o.dur;
			if(diff < 0)
				return -1;
			if(diff > 0)
				return 1;
			return 0;
		}
		@Override
		public String toString() {
			DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
			df.applyPattern( "#,###,######0.00000" );
			DecimalFormat df1 =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
			df1.applyPattern( "#,###,######0.000" );
			return "("+df1.format(dur)+","+df.format(fMeasure)+")";
		}
	}
	
	public List<String> readAndFormatEUCLID(File input) throws IOException {
		FileReader reader = new FileReader(input);
		BufferedReader br = new BufferedReader(reader);
		List<String> result = new LinkedList<String>();
		String s = br.readLine();
		s = br.readLine();
	
		List<LatexEntry> entries = new LinkedList<LatexEntry>();
		
	
		while(s!=null) {
//			System.out.println(s);
			String[] breaks = s.split("\t");
			if(breaks.length==1) {
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    String ss = bufferRead.readLine();
				for(LatexEntry e : entries)
					System.out.println(e);
				entries.clear();				
				System.out.println(s);
			}
			if(breaks.length >= 12) {
				try{
//					System.out.println("f="+breaks[8]+" dur="+breaks[3]);
					LatexEntry entry = new LatexEntry(Double.parseDouble(breaks[3]), Double.parseDouble(breaks[8]));
					entries.add(entry);			
//					System.out.println(breaks[0]);
				}catch(Exception e){}
			}
			s = br.readLine();
		}
		br.close();
		Collections.sort(entries);
		for(LatexEntry e : entries) {
			System.out.println(e);
			result.add(e.toString());
		}
		return result;
	}
}
