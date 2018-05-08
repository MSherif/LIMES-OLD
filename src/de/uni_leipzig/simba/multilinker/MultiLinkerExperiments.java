/**
 * 
 */
package de.uni_leipzig.simba.multilinker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.io.ConfigReader;

/**
 * @author sherif
 *
 */
public class MultiLinkerExperiments {

	void multiLinkPeel() throws IOException{
		String datasetPath="/mypartition2/musicDatasets/multilinkingPeelTest/";
		String peelSpec = datasetPath + "peelSpecs.xml";
		String[] datasetFiles = {"peel_0.ttl", "peel_1.ttl", "peel_2.ttl", "peel_3.ttl", "peel_4.ttl"};
		
		// fill caches
		List<Cache> caches = new ArrayList<Cache>();
		ConfigReader cR = new ConfigReader();
		cR.validateAndRead(peelSpec);
		for (int i = 0; i < datasetFiles.length; i++) {
			cR.sourceInfo.endpoint = datasetPath + datasetFiles[i];
			cR.sourceInfo.id = datasetFiles[i].substring(0, datasetFiles[i].lastIndexOf("."));
			caches.add(HybridCache.getData(cR.getSourceInfo()));
		}
		
//		MultiLinker.multiLinkDataset(caches, 10, "/home/sherif/Desktop/MultilinkingResults/PeelMultiLinkerLog.txt");
		(new MultiLinker()).multiLinkDataset(caches, 10, true);
	}
	
	public static void main(String args[]) throws IOException {
		MultiLinkerExperiments mle= new MultiLinkerExperiments();
//		mle.multiLinkToyData();
		mle.multiLinkPeel();

	}
}
