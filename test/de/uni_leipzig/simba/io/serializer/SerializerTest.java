package de.uni_leipzig.simba.io.serializer;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.io.Serializer;
import de.uni_leipzig.simba.io.SerializerFactory;

public class SerializerTest {
	@Test
	public void testFolderSetting() {
		try{
			Mapping m = new Mapping();
			m.add("a", "b", 1);
			m.add("a", "c", 2d);
			for(String serialtype : new String[]{"ttl", "nt", "tab"}) {
				Serializer serial = SerializerFactory.getSerializer(serialtype);
				serial.setFolderPath(new File("C:/tmp/cache/"));
				String fileName = "testSerial."+serial.getFileExtension();
				serial.writeToFile(m, "owl:sameAs", fileName);
				assertNotNull(serial.getFile(fileName));
				serial.getFile(fileName).delete();
			}
		} catch(Exception e) {
			assertTrue(false);
		}		
	}
	@Test
	public void testNoFolderSetting() {
		try{
			Mapping m = new Mapping();
			m.add("a", "b", 1);
			m.add("a", "c", 2d);
			for(String serialtype : new String[]{"ttl", "nt", "tab"}) {
				Serializer serial = SerializerFactory.getSerializer(serialtype);
				String fileName = "testSerial."+serial.getFileExtension();
				serial.writeToFile(m, "owl:sameAs", fileName);
				assertNotNull(serial.getFile(fileName));
				serial.getFile(fileName).delete();
			}
		} catch(Exception e) {
			assertTrue(false);
		}		
	}
}
