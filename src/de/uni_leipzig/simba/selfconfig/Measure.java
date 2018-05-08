/*
 * Interface for PseudoFMeasures
 */
package de.uni_leipzig.simba.selfconfig;

import de.uni_leipzig.simba.data.Mapping;
import java.util.List;

/**
 *
 * @author ngonga
 */
public interface Measure {

    public double getPseudoFMeasure(List<String> sourceUris, List<String> targetUris,
            Mapping result, double beta);

    /**
     * Computes the pseudo-precision, which is basically how well the mapping
     * maps one single s to one single t
     *
     * @param sourceUris List of source uris
     * @param targetUris List of target uris
     * @param result Mapping of source to targer uris
     * @return Pseudo precision score
     */
    public double getPseudoPrecision(List<String> sourceUris, List<String> targetUris, Mapping result);

    /**
     * The assumption here is a follows. We compute how many of the s and t were
     * mapped.
     *
     * @param sourceUris Uris in source cache
     * @param targetUris Uris in target cache
     * @param result Mapping computed by our learner
     * @param Run mapping minimally and apply filtering. Compare the runtime of
     * both approaches
     * @return Pseudo recall
     */
    public double getPseudoRecall(List<String> sourceUris, List<String> targetUris,
            Mapping result);
    
    public String getName();
    
    /**
	 * Method to set using best 1-to-1 Mapping to compute peseudoMeasures.
	 * @param use1To1Mapping Set true to use best 1-to-1 Mapping to calculate pseudo measures. Set false otherwise (default).
	 */
    public void setUse1To1Mapping(boolean use1To1Mapping);
}
