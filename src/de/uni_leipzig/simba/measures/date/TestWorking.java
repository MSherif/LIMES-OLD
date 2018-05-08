package de.uni_leipzig.simba.measures.date;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;

/**
 * FIXME Create JUnit Test
 * 
 * Test for the Date measure implementations...
 * @author Klaus Lyko
 *
 */
public class TestWorking {
	public static void main(String args[]) {
		basicTest();
	}
	
	public static void basicTest() {
		Logger logger = Logger.getLogger("LIMES"); 
		logger.setLevel(Level.DEBUG);
		KBInfo sI = new KBInfo();
		KBInfo tI = new KBInfo();
		sI.id = "sourceDates";
		tI.id = "targetDates";
		sI.var = "?x"; tI.var = "?y";
		sI.properties = Arrays.asList(new String[]{"date"});
		tI.properties = Arrays.asList(new String[]{"date"});

		Cache sC = new MemoryCache();
		Cache tC = new MemoryCache();
		
		for(int i = 0; i<=10; i++) {
			sC.addTriple("uri"+i, "date", ""+(2000+i));
		}
		for(int i = 0; i<=10; i++) {
			tC.addTriple("uri"+(2+i), "date", ""+(2002+i));
		}
		
//		System.out.println("sC:\n"+sC.toString());
//		System.out.println("tC:\n"+tC.toString());
		SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper( "simple", sI, tI, 
				sC, tC, new LinearFilter(), 1);
		Mapping m = mapper.getLinks("yearsim(x.date,y.date)", 0.01d);
		System.out.println("Mapping:\n"+m);
	}
}
