/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_leipzig.simba.io;

import de.uni_leipzig.simba.io.serializer.CSVSerializer;
import de.uni_leipzig.simba.io.serializer.NtSerializer;
import de.uni_leipzig.simba.io.serializer.TabSeparatedSerializer;
import de.uni_leipzig.simba.io.serializer.TtlSerializer;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class SerializerFactory {
    public static Serializer getSerializer(String name)
    {
        Logger logger = Logger.getLogger("LIMES");
        logger.info("Getting serializer with name "+name);
        if(name==null) return new NtSerializer();
        if(name.toLowerCase().trim().startsWith("tab")) return new TabSeparatedSerializer();
        if(name.toLowerCase().trim().startsWith("csv")) return new CSVSerializer();
        if(name.toLowerCase().trim().startsWith("ttl") || name.toLowerCase().trim().startsWith("turtle")) return new TtlSerializer();
        if(name.toLowerCase().trim().startsWith("nt") || name.toLowerCase().trim().startsWith("turtle")) return new NtSerializer();
        else 
        {
            logger.info("Serializer with name "+name+" not found. Using .nt as default format.");
            return new NtSerializer();
        }
    }
    /**
     * Get all available serializer.
     * @return Array of Serializers.
     */
    public static Serializer[] getAllSerializers() {
    	return new Serializer[] {getSerializer("nt"), getSerializer("csv"), getSerializer("tab"), getSerializer("ttl")}; 
    }
}
