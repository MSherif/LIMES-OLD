package de.uni_leipzig.simba.learning.oracle.mappingreader;

import de.uni_leipzig.simba.data.Mapping;

public interface MappingReader {
	Mapping getMapping(String filePath);
        public String getType();
}
