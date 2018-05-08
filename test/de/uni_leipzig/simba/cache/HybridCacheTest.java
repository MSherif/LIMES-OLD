package de.uni_leipzig.simba.cache;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Test;
import de.uni_leipzig.simba.io.ConfigReader;
/**
 * Tests the new HybridCache with methods to specify the parent folders.
 * @author Lyko
 *
 */
public class HybridCacheTest {
	@Test
	public void testFolderSpec() {
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead("Examples/GeneticEval/PublicationData.xml");
		try{
			HybridCache cache = HybridCache.getData(new File("C:/tmp/"), cR.sourceInfo);
			assertNotNull(cache);
			assertTrue(cache.size()>0);
			File f = new File("C:/tmp/");
			System.out.println("Comparing "+cache.getFolder().getAbsolutePath() +" - "+ f.getAbsolutePath());
			assertEquals(cache.getFolder().getAbsolutePath(), f.getAbsolutePath());
		}catch(Exception e) {
			assertTrue(false);
		}
		
	}
	
	@Test
	public void testNoFolderSpec() {
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead("Examples/GeneticEval/PublicationData.xml");
		try{
			HybridCache cache = HybridCache.getData(cR.sourceInfo);
			assertNotNull(cache);
			assertTrue(cache.size()>0);
			File f = new File("");
			assertEquals(cache.getFolder().getAbsolutePath(), f.getAbsolutePath());
		}catch(Exception e) {
			assertTrue(false);
		}
		
	}
}
