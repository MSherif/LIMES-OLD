package de.uni_leipzig.simba.learning.refinement.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.genetics.evaluation.ExampleOracleTrimmer;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;

public class TrainingDataHandler {
	static final String folder = "resources/trainingData/";
	
	private static File getFile(EvaluationData data, float perc, int exampleNumber) {
		String name = data.getName()+"_"+perc+"_map"+exampleNumber+".ser";
		return new File(folder+name);
	}


	/**
	 * Loading training data of size perc and the exampleNumber, if it doesn't exists its creted.
	 * @param data
	 * @param perc
	 * @param exampleNumber
	 * @param createNew if true a new example is created
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Mapping getTrainingData(EvaluationData data, float perc, int exampleNumber, boolean createNew) throws IOException, ClassNotFoundException {
		if(getFile(data,perc,exampleNumber).exists() && !createNew) {
			return readMappingFromFile(data,perc,exampleNumber);
		} else {
			return createData(data,perc,exampleNumber);
		}
	}
	
	private static Mapping createData(EvaluationData data, float perc, int exampleNumber) throws IOException {
		int size = (int)(data.getReferenceMapping().map.keySet().size()*perc);
		Mapping map = ExampleOracleTrimmer.getRandomTrainingData(data.getReferenceMapping(), size);
		FileOutputStream fileOut = new FileOutputStream(getFile(data,perc,exampleNumber), false);
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
	    out.writeObject(map);
		out.close();
		fileOut.close();
		return map;
	}
	
	private static Mapping readMappingFromFile(EvaluationData data, float perc, int exampleNumber) throws IOException, ClassNotFoundException {
		Mapping map;
		
		File file = getFile(data,perc,exampleNumber);
		System.out.println("Attempting to read Mapping "+file.getName()+"...");
		FileInputStream fileIn = new FileInputStream(file);
	    ObjectInputStream in = new ObjectInputStream(fileIn);
	    map = (Mapping) in.readObject();
	    in.close();
	    fileIn.close();
	    System.out.println("Succesful read Mapping of size "+map.size()+" out of file "+file.getName());
	    return map;
	}
	
	
	/** Creates a random Mapping of learning data*/
	public static Mapping getStartData(EvaluationData data, int inquerieSize) {
		int count = 0;
		Mapping ref = data.getReferenceMapping();
		Mapping startData = new Mapping();
		for(String s : ref.map.keySet()) {
			for(String t : ref.map.get(s).keySet()) {
				startData.add(s, t, 1d); count++;
				break;
			}
			if(count>=inquerieSize)
				return startData;
		}
		return startData;
	}
	
	
//----------------- creation
	public static void create() {
		DataSets allEvalData[] = {
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPACM,				
				DataSets.DBPLINKEDMDB,
				DataSets.DBLPSCHOLAR,
		}; 
		int[] examples = new int[]{1,2,3,4,5};
		float[] percents = new float[]{0.3f,0.5f};
		for(DataSets ds : allEvalData) {
			for(float f:percents) {
				for(int ex:examples) {
					try {
						getTrainingData(DataSetChooser.getData(ds),f,ex,true);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	
	public static void main(String args[]) {
		DataSets allEvalData[] = {
				DataSets.PERSON1,
				DataSets.PERSON2,
				DataSets.RESTAURANTS,
				DataSets.ABTBUY,
				DataSets.AMAZONGOOGLE,
				DataSets.DBLPACM,				
				DataSets.DBPLINKEDMDB,
				DataSets.DBLPSCHOLAR,
		}; 
		int[] examples = new int[]{1,2,3,4,5};
		float[] percents = new float[]{0.3f,0.5f};
		for(DataSets ds : allEvalData) {
			for(float f:percents) {
				for(int ex:examples) {
					try {
						getTrainingData(DataSetChooser.getData(ds), f, ex, false);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}
