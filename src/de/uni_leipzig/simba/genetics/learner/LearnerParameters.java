package de.uni_leipzig.simba.genetics.learner;

import de.uni_leipzig.simba.genetics.util.PropertyMapping;
import de.uni_leipzig.simba.io.ConfigReader;

/**
 * Beanish class to set parameters to learn link specifications.
 * @author Klaus Lyko
 *
 */
public class LearnerParameters {

	private int populationSize = 20;
	private int generations = 20;
	private float mutationRate = 0.3f;
	private float crossoverRate = 0.3f;
	private float reproductionRate = 0.5f;
	private boolean preserveFittestIndividual = true;
	private int granularity = 2;
	private PropertyMapping propertyMapping;
	private ConfigReader configReader;
	
	/**
	 * Constructor as a specified ConfigReader and a PropertyMapping is required by the Learner.
	 * @param cR
	 * @param propMap
	 */
	public LearnerParameters(ConfigReader cR, PropertyMapping propMap) {
		this.setConfigReader(cR);
		this.setPropertyMapping(propMap);
	}
	
	/**
	 * @return the populationSize
	 */
	public int getPopulationSize() {
		return populationSize;
	}
	/**
	 * @param populationSize the populationSize to set
	 */
	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}
	/**
	 * @return the generations
	 */
	public int getGenerations() {
		return generations;
	}
	/**
	 * @param generations the generations to set
	 */
	public void setGenerations(int generations) {
		this.generations = generations;
	}
	/**
	 * @return the mutationRate
	 */
	public float getMutationRate() {
		return mutationRate;
	}
	/**
	 * @param mutationRate the mutationRate to set
	 */
	public void setMutationRate(float mutationRate) {
		this.mutationRate = mutationRate;
	}
	/**
	 * @return the reproductionRate
	 */
	public float getReproductionRate() {
		return reproductionRate;
	}
	/**
	 * @param reproductionRate the reproductionRate to set
	 */
	public void setReproductionRate(float reproductionRate) {
		this.reproductionRate = reproductionRate;
	}
	/**
	 * @return the crossoverRate
	 */
	public float getCrossoverRate() {
		return crossoverRate;
	}
	/**
	 * @param crossoverRate the crossoverRate to set
	 */
	public void setCrossoverRate(float crossoverRate) {
		this.crossoverRate = crossoverRate;
	}
	/**
	 * @return the preserveFittestIndividual
	 */
	public boolean isPreserveFittestIndividual() {
		return preserveFittestIndividual;
	}
	/**
	 * @param preserveFittestIndividual the preserveFittestIndividual to set
	 */
	public void setPreserveFittestIndividual(boolean preserveFittestIndividual) {
		this.preserveFittestIndividual = preserveFittestIndividual;
	}
	/**
	 * @return the granularity
	 */
	public int getGranularity() {
		return granularity;
	}
	/**
	 * @param granularity the granularity to set
	 */
	public void setGranularity(int granularity) {
		this.granularity = granularity;
	}
	/**
	 * @return the propertyMapping
	 */
	public PropertyMapping getPropertyMapping() {
		return propertyMapping;
	}
	/**
	 * @param propertyMapping the propertyMapping to set
	 */
	public void setPropertyMapping(PropertyMapping propertyMapping) {
		this.propertyMapping = propertyMapping;
	}
	/**
	 * @return the configReader
	 */
	public ConfigReader getConfigReader() {
		return configReader;
	}
	/**
	 * @param configReader the configReader to set
	 */
	public void setConfigReader(ConfigReader configReader) {
		this.configReader = configReader;
	}
}
