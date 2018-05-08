/**
 * 
 */
package de.uni_leipzig.simba.mapper.atomic.hausdorff.parallelHr3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import de.uni_leipzig.simba.data.Point;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoIndex;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;

/**
 * @author sherif
 *
 */
public class ParallelPolygonGeoIndexer {
	public Set<Polygon>	input 		 = new HashSet<Polygon>();
	static float 	 	delta;
	public boolean	 	verbose 	 = false;
	public int 	 	maxThreadsNr = 4;
	private static Semaphore allDone;
	public  static GeoIndex  index = new GeoIndex();


	public ParallelPolygonGeoIndexer(Set<Polygon> input, float delta, int maxThreadsNr) {
		this.input = input;
		this.delta = delta;
		this.maxThreadsNr = maxThreadsNr;
	}

	public GeoIndex getIndex() {
		return index;
	}

	//----------------------------------------------------------------------------------------------------------
	class GeoIndexerTheard extends Thread {
		Set<Polygon> inputPolygons = new HashSet<Polygon>();
//		public GeoIndex subIndex = new GeoIndex();

		/**
		 * @param input
		 *@author sherif
		 */
		public GeoIndexerTheard(Set<Polygon> inputPolygons) {
			super();
			this.inputPolygons = inputPolygons;
		}

		public void assignSquares() {
			for (Polygon p : inputPolygons) {
				for (Point x : p.points) {
					int latIndex = (int) Math.floor(x.coordinates.get(0) / delta);
					int longIndex = (int) Math.floor(x.coordinates.get(1) / delta);
//					if (verbose) {
//						System.out.println(p.uri + ": (" + latIndex + "," + longIndex + ")");
//					}
					synchronized (index) {
						index.addPolygon(p, latIndex, longIndex);
					}
					
				}
			}
		}

		// Compute Squares for each subset of input polygons
		public void run() {
//			long startTime = System.currentTimeMillis();
			assignSquares();
//			synchronized(allDone){
				allDone.release();				
//				System.out.println(getName() + " time = "+ (System.currentTimeMillis() - startTime) + "ms");
//			}
		}
	}
	//----------------------------------------------------------------------------------------------------------

	// This is the key method -- launch all the threads,
	// wait for them to finish.
	public void run() {
		allDone = new Semaphore(0);
		// Make and start all the threads, keeping them in a list.
		List<GeoIndexerTheard> threads = new ArrayList<GeoIndexerTheard>();
		int subsetLength = (int) Math.ceil((float) input.size() / maxThreadsNr);

		Set<Polygon> subset;
		Iterator<Polygon> itr = input.iterator();
		while(itr.hasNext()){
			subset = new HashSet<Polygon>();
			for(int i = 0 ; i < subsetLength && itr.hasNext() ; i++){
				subset.add(itr.next());
			}
			GeoIndexerTheard t = new GeoIndexerTheard(subset);
			threads.add(t);
			t.start();
		}

		// Wait to finish (this strategy is an alternative to join())
		try {
			allDone.acquire(maxThreadsNr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
