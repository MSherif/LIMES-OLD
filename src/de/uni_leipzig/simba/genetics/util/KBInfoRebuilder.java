package de.uni_leipzig.simba.genetics.util;

import java.util.HashMap;
import java.util.Set;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;

public class KBInfoRebuilder {
	
	public static String[] atomicProcesses = {"cleaniri","nolang","lowercase","removebraces","regularAlphabet"};
	public static String[] chainedFunctions = {"regularAlphabet->lowercase", "removebraces->lowercase"};
	
	
	public KBInfo addChainedPrerprocessingFunctions(KBInfo kbinfo, String property){
		if (!kbinfo.properties.contains(property))
			return kbinfo;
		
        if (!kbinfo.functions.containsKey(property)) {
            kbinfo.functions.put(property, new HashMap<String, String>());
        }

        for(String process : chainedFunctions)
        	kbinfo.functions.get(property).put(property+"["+process+"]", process);

		return kbinfo;
	}
	
	/**
	 * Adds all atomic processes for the given property as new properties with the given name appended by "[preprocess]"
	 * @param info
	 * @param property
	 * @return
	 */
	public KBInfo addAtomicStringPreprocesses(KBInfo kbinfo, String property) {
		if (!kbinfo.properties.contains(property))
			return kbinfo;
		
        if (!kbinfo.functions.containsKey(property)) {
            kbinfo.functions.put(property, new HashMap<String, String>());
        }

        for(String process : atomicProcesses)
        	kbinfo.functions.get(property).put(property+"["+process+"]", process);

		return kbinfo;
		
	}
	
	/**
	 * Function to determine the best preprocessing solution to create additional properties based
	 * on chained preprocessing function. Consider the following example:
	 * Cache c already holds a property <i>name[nolang]</i> and we want to add the property <i>name[nolang->lowercase]</i>.
	 * It would be more efficient to simply take the property <i>name[nolang]</i> as source and additionally
	 * process it with the neccessary function(s) <i>lowercase</i>.
	 * This method tries to discover these cases.<br>
	 * @param c
	 * @param property
	 * @param functionChain
	 * @return String array:<ul><li>0=best source property<li>1=function chain still to do<li>2=proposed new name of the property.</ul>
	 */
	public static String[] getBestProcessingSolution(Cache c, String property, String functionChain) {
		String[] solution = {property, functionChain, property+"["+functionChain+"]"};//default
		Set<String> availableProps = c.getAllProperties();
		if(!functionChain.contains("->"))
			return solution;
		// if prop already exists
		if(availableProps.contains(solution[2])) {
			solution[0] = solution[2];
			solution[1] ="";
			return solution;
		}
		String[] chain = functionChain.split("->");
		for(int i = chain.length-1; i>0; i--) {
			//iterate backwards
			String process = "";		
			String possiblePreprocessed = chain[0];
			for(int a=1; a<i; a++) // construct possibly preprocessed properzy names
				possiblePreprocessed += "->" + chain[a];
			for(int b=i; b<chain.length-1; b++)
				process += chain[b]+"->";
			process += chain[chain.length-1];
			if(availableProps.contains(property+"["+possiblePreprocessed+"]")) {
				solution[0] = property+"["+possiblePreprocessed+"]";
				solution[1] = process;
				return solution;
			}
		}
		return solution;
	}
	
	public static void testChaining(Cache c, String property, String functionChain) {
		if(!functionChain.contains("->"))
			return;
		
	}
	
	public static void main(String args[]) {
		EvaluationData param =  DataSetChooser.getData(DataSets.DBLPACM);
		ConfigReader cR = param.getConfigReader();
		KBInfo info = cR.sourceInfo;
		KBInfo info2 = info;
		System.out.println(info);
		KBInfoRebuilder b = new KBInfoRebuilder();
		for(String prop : info.properties) {
			info2 = b.addAtomicStringPreprocesses(info2, prop);
		}
		System.out.println(info2);
		info2.id = info.id+"rebuild";
		Cache c = HybridCache.getData(info2);
		System.out.println(c.getAllProperties());
		for(String prop : info.properties) {
			String newProp = "lowercase->cleaniri";
			String[] sol = getBestProcessingSolution(c, prop, newProp);
//			for(String out : sol) {
//				System.out.print(out+"\t");
//			}
			c = c.addProperty(sol[0], sol[2], sol[1]);
//			System.out.println();
		}
		
		String sol[] = getBestProcessingSolution(c, "authors", "lowercase->cleaniri->nolang");
		for(String out : sol) {
			System.out.print(out+"\t");
		}
	}
}
