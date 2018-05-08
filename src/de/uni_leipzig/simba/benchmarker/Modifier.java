/**
 * 
 */
package de.uni_leipzig.simba.benchmarker;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author sherif
 *
 */
public abstract class Modifier {
	static Model baseModel = ModelFactory.createDefaultModel();
	static Model destroyedPropertiesModel = ModelFactory.createDefaultModel(); //destroyed properties model
	static Model destroyedModel = ModelFactory.createDefaultModel();
	static List<Property> properties = new ArrayList<Property>();
	
	abstract Model destroy(Model subModel);
	
	/**
	 * @return a sub model contains the destroyed Properties
	 */
	public static Model getDestroyedPropertiesModel() {
		for(Property p: properties){
			StmtIterator sItr = baseModel.listStatements(null, p,(RDFNode) null);
			while(sItr.hasNext()){
				Statement stmt = sItr.nextStatement();
				destroyedPropertiesModel.add(stmt);
			}
		}
		return destroyedPropertiesModel;
	}

	/**
	 * @param destroyedPropertiesModel the destroyedPropertiesModel to set
	 */
	public static void setDestroyedPropertiesModel(Model destroyedPropertiesModel) {
		Modifier.destroyedPropertiesModel = destroyedPropertiesModel;
	}


	/**
	 * @return the properties
	 */
	public List<Property> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void addProperty(Property p) {
		this.properties.add(p);
	}

	double destructionRatio = 0.5;
	long destroyedInstancesCount;
	boolean destroyProperty = false;


	public Model loadModel(String fileNameOrUri){
		Model model=ModelFactory.createDefaultModel();
		java.io.InputStream in = FileManager.get().open( fileNameOrUri );
		if (in == null) {
			throw new IllegalArgumentException(
					"File/URI: " + fileNameOrUri + " not found");
		}
		if(fileNameOrUri.endsWith(".ttl")){
			System.out.println("Opening Turtle file");
			model.read(in, null, "TTL");
		}else if(fileNameOrUri.endsWith(".rdf")){
			System.out.println("Opening RDFXML file");
			model.read(in, null);
		}else if(fileNameOrUri.endsWith(".nt")){
			System.out.println("Opening N-Triples file");
			model.read(in, null, "N-TRIPLE");
		}else{
			System.out.println("Content negotiation to get RDFXML from " + fileNameOrUri);
			model.read(fileNameOrUri);
		}

		System.out.println("loading "+ fileNameOrUri + " is done!!");
		System.out.println();
		return model;
	}


}
