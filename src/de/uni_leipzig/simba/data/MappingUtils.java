package de.uni_leipzig.simba.data;

import com.google.common.collect.Range;

public class MappingUtils {
    /**
     * Create a new mapping that only contains those entries of the given mapping, whose
     * score is contained in the given range.
     *
     * @param mapping
     * @param range
     * @return
     */
    public static Mapping extractByThresholdRange(Mapping mapping, Range<Double> range) {
        Mapping result = new Mapping();
        for (String key : mapping.map.keySet()) {
            for (String value : mapping.map.get(key).keySet()) {
                Double val = mapping.map.get(key).get(value);
                boolean isContained = range.contains(val);
                if(isContained) {
                    result.add(key, value, mapping.map.get(key).get(value));
                }
            }
        }
        return result;
    }
}
