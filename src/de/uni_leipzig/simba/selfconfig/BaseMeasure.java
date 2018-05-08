package de.uni_leipzig.simba.selfconfig;

/**
 * Provide common methods for pseudo measure implemenations
 * @author Klaus Lyko
 *
 */
public abstract class BaseMeasure implements Measure{
	boolean use1To1Mapping = false;

	/**
	 * @return the use1To1Mapping
	 */
	public boolean isUse1To1Mapping() {
		return use1To1Mapping;
	}

//	@Override
	public void setUse1To1Mapping(boolean use1To1Mapping) {
		this.use1To1Mapping = use1To1Mapping;
	}
}
