package de.uni_leipzig.simba.util;

import org.apache.log4j.Logger;

import de.uni_leipzig.simba.io.KBInfo;

public class DataCleaner {
	static Logger logger = Logger.getLogger("LIMES");
	KBInfo kbInfo;
	
	public static String[] separate(String line, String SEP, int NumberOfProperties) {
		String[] result = new String[NumberOfProperties];
		String[] split = line.split(SEP);
//		logger.info(line + " - " + SEP + " - " + NumberOfProperties);
		if(split.length == NumberOfProperties) {
//			logger.info("returning standard");
			for(int i=0;i<split.length;i++) {
				split[i] = removeQuotes(split[i]);
			}
			return split;
		}
		else {
			// brute force: read each character if it equals SEP and is NOT between a open " make a split
			for(int i = 0; i<NumberOfProperties; i++) {
				int sepOc = line.indexOf(SEP);
				int openApostrophe = line.indexOf("\"");
//				logger.info("openApostrophe="+openApostrophe);
				if(openApostrophe == -1 && sepOc == -1) {
					result[i]=removeQuotes(line);
					return result;
				}
				if(sepOc == -1) {
					result[i] = removeQuotes(line);
					return result;
				}
					
				if(sepOc < openApostrophe) {
					result[i] = removeQuotes(line.substring(0, sepOc));
					line = line.substring(sepOc+1);
					
				}
				else {
					// we found a SEP within an apostrophe
					int closingApostrophe = line.indexOf("\"", openApostrophe+1);
					int nextSep = line.indexOf(SEP, closingApostrophe);
//					logger.info("closingApostrophe:" + closingApostrophe);
//					logger.info("nextSep="+nextSep);
					if(openApostrophe < nextSep && openApostrophe > -1) {
						result[i] = removeQuotes(line.substring(openApostrophe, nextSep));
					}
					else 
						result[i] = "";
//					logger.info("closingAp="+closingApostrophe+"  cut out: "+openApostrophe+" - "+nextSep);
					line = line.substring(nextSep+1);
				}
//				logger.info("=>"+line);
//				logger.info(i + " " + result[i]+"\n\n");
			}
			
		}		
		return result;
	}
	
	public static void main(String[] args) {
		String example[] = {"304586,\"The WASA2 object-oriented workflow management system\",\"Gottfried Vossen, Mathias Weske\",\"International Conference on Management of Data\",1999",
		"\"conf/vldb/RusinkiewiczKTWM95\",\"Towards a Cooperative Transaction Model - The Cooperative Activity Model\",\"M Rusinkiewicz, W Klas, T Tesch, J W�sch, P Muth\",\"VLDB\",1995",
		"\"<http://dbpedia.org/resource/Edison,_Marconi_&_Co.>\",\"Edison, Marconi & Co.\",\"Robert A. McGowan\"",
		"123,\"gggg,ffff\""};
		for(String ex:example) {
			String[] splits = separate(ex, ",", 3);
			
			for(String a : splits) {
				System.out.println("=>"+a);
			}
		}
		
	}
	
	public static String removeQuotes(String s) {
		int openApostrophe = s.indexOf("\"");
		if(openApostrophe == 0)
			s = s.substring(1);
		if(s.endsWith("\""))
			s = s.substring(0, s.length()-1);
		return s;
	}
	
}
