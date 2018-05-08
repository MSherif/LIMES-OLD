 /**
 * 
 */
package de.uni_leipzig.simba.benchmarker;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Sherif
 *
 */
public class BenchmarkGenerator extends Modifier{

	/**
	 * @param basemodel
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroy (Model m, Map<? extends Modifier, Double> modefiersAndRates){
		baseModel= m;
		return destroy(modefiersAndRates);
	}


	Model destroy (Map<? extends Modifier, Double> modefiersAndRates){
		return destroy (modefiersAndRates, 0d);
	}
	
	/**
	 * @param modefiersAndRates
	 * @return destroyed model
	 * @author Sherif
	 */
	Model destroy (Map<? extends Modifier, Double> modefiersAndRates, double startPointRatio){
		Model inputModel;
		if(properties.size()==0){   // If the modifier properties are not set then divide the whole Model
			inputModel=baseModel;
		}else{						//Else if the properties is set then divide based on the properties Model
			Modifier.getDestroyedPropertiesModel();
			inputModel=destroyedPropertiesModel;
		}

		long inputModelOffset = (long) Math.floor(startPointRatio*inputModel.size());
		if(startPointRatio > 0){
			// Add the skipped portion of the input Model (form beginning to startPoint)
			Model InputModelStartPortion = getSubModel(inputModel, inputModelOffset, 0);
			destroyedModel.add(InputModelStartPortion); 
//			inputModel.remove(InputModelStartPortion);
		}

		for(Entry<? extends Modifier, Double> mod2rat: modefiersAndRates.entrySet() ){
			Modifier modifer = mod2rat.getKey();
			Double   rate    = mod2rat.getValue();
			long subModelSize = (long) (inputModel.size()*rate);
			
			if((inputModelOffset+subModelSize) > inputModel.size()){
				System.out.println("The sum of modifiers rates is grater than 100% ... exit with error");
				System.exit(1);
			}
			
			Model subModel = getSubModel(inputModel, subModelSize, inputModelOffset);
			modifer.destroy(subModel);
			inputModelOffset += subModelSize;
		}
		
		// Add the rest of the input Model (form current inputModelOffset to the end)
		Model InputModelRest = getSubModel(inputModel, inputModel.size()-inputModelOffset,inputModelOffset);
		destroyedModel.add(InputModelRest); //tell end

		// If  there some properties add the rest of the undestroyed base Model
		if(properties.size()>0){
			baseModel.remove(destroyedPropertiesModel);
			destroyedModel.add(baseModel); 
		}
		return destroyedModel;
	}



	/**
	 * @param subModelSize
	 * @param baseModelOffset
	 * @return A sub model of the fromModel with size subModelSize and starting from offset baseModelOffset
	 * @author Sherif
	 */
	private Model getSubModel(Model inputModel, long subModelSize, long baseModelOffset) {
		Model subModel = ModelFactory.createDefaultModel();

		StmtIterator sItr = inputModel.listStatements();

		//skip statements tell the offset reached
		for(int i=0 ; i<baseModelOffset ; i++){
			sItr.nextStatement();		
		}

		//Copy the sub-model
		for(int i=0 ; i<subModelSize ; i++){
			Statement stat = sItr.nextStatement();	
			subModel.add(stat);
		}
		return subModel;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.simba.benchmarker.Modifier#destroy(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	Model destroy(Model subModel) {
		// TODO Auto-generated method stub
		return null;
	}



	/**
	 * @param fileNameOrUri
	 * @return Base model loaded from file or URI
	 * @author Sherif
	 */
	public Model loadBaseModel(String fileNameOrUri){
		baseModel=ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(
					"File/URI: " + fileNameOrUri + " not found");
		}
		if(fileNameOrUri.endsWith(".ttl")){
			System.out.println("Opening Turtle file");
			baseModel.read(in, null, "TTL");
		}else if(fileNameOrUri.endsWith(".rdf")){
			System.out.println("Opening RDFXML file");
			baseModel.read(in, null);
		}else if(fileNameOrUri.endsWith(".nt")){
			System.out.println("Opening N-Triples file");
			baseModel.read(in, null, "N-TRIPLE");
		}else{
			System.out.println("Content negotiation to get RDFXML from " + fileNameOrUri);
			baseModel.read(fileNameOrUri);
		}

		System.out.println("loading "+ fileNameOrUri + " is done!!");
		System.out.println();
		return baseModel;
	}


	/**
	 * this is just a test function
	 * @return Map of Modifiers and associated rates
	 * @author sherif
	 */
	private Map<? extends Modifier, Double> getModefiersAndRates(){
		Map<Modifier, Double> modefiersAndRates= new HashMap<Modifier, Double>();
		
		Modifier abbreviationModifier= ModifierFactory.getModifier("abbreviation");
		modefiersAndRates.put(abbreviationModifier, 0.033d);
		
		
		Modifier misspelingModifier= ModifierFactory.getModifier("misspelling");
		modefiersAndRates.put(misspelingModifier, 0.033d);
		
//		Modifier acronymModifier= ModifierFactory.getModifier("acronym");
//		modefiersAndRates.put(acronymModifier, 0.5d);
		
		Modifier permutationModifier= ModifierFactory.getModifier("permutation");
		modefiersAndRates.put(permutationModifier, 0.033d);
		
//		Modifier splitModifier= ModifierFactory.getModifier("split");
//		((SplitModifier) splitModifier).addSplitProperty(RDFS.label);
//		((SplitModifier) splitModifier).addSplitProperty(RDFS.comment);
//		modefiersAndRates.put(splitModifier, 0.5d);
		
//		Modifier mergeModifier= ModifierFactory.getModifier("merge");
//		((MergeModifier) mergeModifier).setMergeProperty(RDFS.label);
//		modefiersAndRates.put(mergeModifier, 1d);

		return modefiersAndRates;
	}

	
	void bmPeel(String inputFileName, String outputFilename){
		loadBaseModel(inputFileName);
		System.out.println("----- Base Model -----");
		System.out.println("Size: "+baseModel.size());
//		Modifier.baseModel.write(System.out, "N-TRIPLE");
		System.out.println();
		properties.add(RDFS.label);
		properties.add(FOAF.name);
		Map<? extends Modifier, Double> modefiersAndRates= getModefiersAndRates();
		destroy(modefiersAndRates,0.51);
		System.out.println();
		System.out.println("----- Destroyed Model -----");
		System.out.println("Size: "+ destroyedModel.size());
		FileWriter outFile;
		try {
			outFile = new FileWriter(outputFilename);
			destroyedModel.write(outFile, "N-TRIPLE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	public static void main(String args[]){
		BenchmarkGenerator benchmarker= new BenchmarkGenerator();
		benchmarker.bmPeel(args[0], args[1]);
//		benchmarker.loadBaseModel(args[0]);
//		System.out.println("----- Base Model -----");
//		System.out.println("Size: "+baseModel.size());
//		Modifier.baseModel.write(System.out, "N-TRIPLE");
//		System.out.println();
//		properties.add(FOAF.name);
//		Map<? extends Modifier, Double> modefiersAndRates= benchmarker.getModefiersAndRates();
//		benchmarker.destroy(modefiersAndRates);
//		System.out.println();
//		System.out.println("----- Destroyed Model -----");
//		System.out.println("Size: "+ destroyedModel.size());
//		Modifier.destroyedModel.write(System.out, "N-TRIPLE");
	}
}























