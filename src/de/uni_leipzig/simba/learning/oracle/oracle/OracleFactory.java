package de.uni_leipzig.simba.learning.oracle.oracle;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.oracle.mappingreader.CSVMappingReader;
import de.uni_leipzig.simba.learning.oracle.mappingreader.MappingReader;
import de.uni_leipzig.simba.learning.oracle.mappingreader.XMLMappingReader;

public class OracleFactory {

    /** Creates an oracle based on the input type (i.e., the type of file within which the
     * oracle data is contained) and the type of oracle needed.
     * @param filePath Path to the file containing the data encapsulated by the oracle
     * @param inputType Type of the file
     * @param oracleType Type of oracle required
     * @return An oracle that contains the data found at filePath
     */
    public static Oracle getOracle(String filePath, String inputType, String oracleType) {
        MappingReader reader;
        Oracle oracle;
        System.out.println("Getting reader of type " + inputType);
        if (inputType.equalsIgnoreCase("csv")) //scan input types here
        {
            reader = new CSVMappingReader();
        } else if (inputType.equalsIgnoreCase("xml")) //scan input types here
        {
            reader = new XMLMappingReader();
        } else if (inputType.equalsIgnoreCase("tab")) //scan input types here
        {
            reader = new CSVMappingReader();
            ((CSVMappingReader) reader).setSeparator("\t");
        } else //default
        {
            reader = new CSVMappingReader();
        }

        System.out.println("Reading oracle data by using Reader of type " + reader.getType());
        //now readData
        Mapping m = reader.getMapping(filePath);

        //finally return the right type of oracle
        if (inputType.equals("simple")) //scan input types here
        {
            oracle = new SimpleOracle(m);
        } else //default
        {
            oracle = new SimpleOracle(m);
        }
//        oracle.loadData(m);
        return oracle;
    }
}
