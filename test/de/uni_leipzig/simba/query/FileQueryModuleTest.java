package de.uni_leipzig.simba.query;

import static org.junit.Assert.*;

import org.junit.Test;

import de.uni_leipzig.simba.cache.MemoryCache;
import de.uni_leipzig.simba.io.KBInfo;

public class FileQueryModuleTest
{
	static public KBInfo createKBInfo(String endpoint, String graph, int pageSize, String id,String type)
	{
		KBInfo kb = new KBInfo();
		kb.endpoint=endpoint;
		kb.graph=graph;
		kb.pageSize=pageSize;
		kb.id=id;
		kb.type=type;
		return kb;
	}	
		
	@Test
	public void testFileQueryModuleWithFile()
	{
		KBInfo kb = createKBInfo("resources/Persons1/person11.nt","",-1,"persons11","N3");
		new FileQueryModule(kb);
	}

	@Test
	public void testFileQueryModuleWithResource()
	{
		KBInfo kb = createKBInfo("Persons1/person11.nt","",-1,"persons11","N3");
		new FileQueryModule(kb);
	}

}
