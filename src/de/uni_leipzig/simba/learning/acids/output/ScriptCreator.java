package de.uni_leipzig.simba.learning.acids.output;

import java.io.IOException;
import java.util.ArrayList;

import de.uni_leipzig.simba.learning.acids.data.Property;
import de.uni_leipzig.simba.learning.acids.data.Resource;

public interface ScriptCreator {
	
	public void create(ArrayList<Resource> sources, ArrayList<Resource> targets, 
			ArrayList<Property> props, double[] w_linear, double theta, 
			ArrayList<String> perfectMapping) throws IOException;

}
