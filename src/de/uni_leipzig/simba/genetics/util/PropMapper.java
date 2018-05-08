package de.uni_leipzig.simba.genetics.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import de.uni_leipzig.simba.io.ConfigReader;
/**
 * Just a little helper method to get PropertyMappings of the evaluation datasets.
 * @author Klaus Lyko
 *
 */
public class PropMapper {
	
	/**
	 * We basically decide by the name of the config file.
	 * @param configFile
	 * @return
	 */
	public static PropertyMapping getPropertyMapping(String configFile) {		
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(configFile);
		return  getPropertyMapping(cR, configFile);
		
	}
	
	public static PropertyMapping getPropertyMapping(ConfigReader cR, String name) {
		
		PropertyMapping pM = new PropertyMapping();

		if(!name.substring(name.lastIndexOf("/")+1).startsWith("dbpedia-linkedmdb") 
				&& !name.substring(name.lastIndexOf("/")+1).startsWith("dailymed-drugbank")) { 
			int max = Math.max(cR.sourceInfo.properties.size(), cR.targetInfo.properties.size())-1;		
			for(int i = 0; i<max; i++) {
				pM.addStringPropertyMatch(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i));
			}
			pM.addDatePropertyMatch(cR.sourceInfo.properties.get(max), cR.targetInfo.properties.get(max));
		}
		else {
			int max = Math.max(cR.sourceInfo.properties.size(), cR.targetInfo.properties.size()-1);		
			for(int i = 0; i<max; i++) {
				pM.addStringPropertyMatch(cR.sourceInfo.properties.get(i), cR.targetInfo.properties.get(i));
			}                        
		}
		System.out.println("PM: "+pM);
		return pM;
	}
	
	/**
	 * Get propertyMapping from a file with the same name as the LIMES config file without ".xml" concateneted with
	 * "propertymatch". Content should be lines <i>Source Property \t Target Property \t {"number", "string"}.
	 * E.g. "dbo:name	refs:label	string".
	 * @param folder Path of the folder
	 * @param name Name of the LIMES config file.
	 * @return PropertyMapping read from the file.
	 */
	@SuppressWarnings("finally")
	public static PropertyMapping getPropertyMappingFromFile(String folder, String name) {
		if(name.indexOf("/")>0)
			name = name.substring(name.lastIndexOf("/"));
		String filename = name.substring(0, name.lastIndexOf("."));
		filename += "propertymatch";
		
		PropertyMapping pM = new PropertyMapping();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(folder+filename));
			String s = reader.readLine();
			String split[];
			while(s != null && s.length()>0) {
				split = s.split("\t");
				if(split[2].equalsIgnoreCase("number"))
					pM.addNumberPropertyMatch(split[0], split[1]);
				if(split[2].equalsIgnoreCase("string"))
					pM.addStringPropertyMatch(split[0], split[1]);
				if(split[2].equalsIgnoreCase("date"))
					pM.addDatePropertyMatch(split[0], split[1]);
				if(split[2].equalsIgnoreCase("pointset"))
					pM.addPointsetPropertyMatch(split[0], split[1]);
				s = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			System.out.println(pM);
			return pM;
		}
	}
}
