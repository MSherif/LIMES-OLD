package de.uni_leipzig.simba.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFCalculator;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.planner.CanonicalPlanner;
import de.uni_leipzig.simba.execution.planner.ExecutionPlanner;
import de.uni_leipzig.simba.execution.planner.HeliosPlanner;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser;
import de.uni_leipzig.simba.genetics.evaluation.basics.DataSetChooser.DataSets;
import de.uni_leipzig.simba.genetics.evaluation.basics.EvaluationData;
import de.uni_leipzig.simba.genetics.util.Pair;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import de.uni_leipzig.simba.specification.LinkSpec;
import de.uni_leipzig.simba.specification.Operator;

public class ThresholdPlotter {
	
	List<String> measures = new LinkedList<String>();
	
	public ThresholdPlotter() {
		measures.add("levensthein");
		measures.add("cosine");
		measures.add("trigrams");
	}
	
	private static List<Double> getThresholdDataPoints() {
		List<Double> points = new LinkedList();
		for(int i = 1; i<=10; i++) {
			points.add((i*0.1d));
		}
		return points;
	}
	
	private LinkSpec construct(EvaluationData evalData, String measure, String prop1, String prop2, Double threshold) {
		String sourceVar = evalData.getConfigReader().sourceInfo.var;
		if(sourceVar.startsWith("?")&& sourceVar.length()>=2)
			sourceVar = sourceVar.substring(1);
		String targetVar = evalData.getConfigReader().targetInfo.var;
		if(targetVar.startsWith("?")&& targetVar.length()>=2)
			targetVar = targetVar.substring(1);
		LinkSpec spec = new LinkSpec();
		spec.setAtomicFilterExpression(measure, sourceVar+"."+prop1, targetVar+"."+prop2);
		spec.threshold = threshold;
//		System.out.println(spec);
		return spec;
	}
	
	private LinkSpec constructComplex(LinkSpec atom1, LinkSpec atom2, Operator operator, EvaluationData evalData) {
		LinkSpec spec = new LinkSpec();
		spec.operator = operator;
		spec.addChild(atom1);
		spec.addChild(atom2);
		if(operator.equals(Operator.OR)) {
			spec.threshold = Math.min(atom1.threshold, atom2.threshold);
		}else {
			spec.threshold = Math.min(atom1.threshold, atom2.threshold);
		}
		spec.threshold = 0.0001d;
		return spec;
	}
	
	/**
	 * 
	 * @param data
	 * @throws IOException 
	 */
	public void plotAtomic(EvaluationData data) throws IOException {
		FileWriter fw = new FileWriter(createFile(data), true);
		ExecutionPlanner planner = new HeliosPlanner(data.getSourceCache(), data.getTargetCache());
		ExecutionEngine engine = new ExecutionEngine(data.getSourceCache(), 
				data.getTargetCache(), 
				data.getConfigReader().sourceInfo.var, 
				data.getConfigReader().targetInfo.var);
		for(Pair<String> pair : data.getPropertyMapping().stringPropPairs) {
			writeNewCaption(pair.toString(), fw);
			for(String measure : measures) {
				writeNewPlot(measure, fw);
				for(Double d : getThresholdDataPoints()) {
					LinkSpec spec = construct(data, measure, pair.a, pair.b, d);
					Mapping m = new Mapping();
					try {
						m = engine.runNestedPlan(planner.plan(spec));							
					} catch(Exception e) {
						System.out.println("Unable to execute spec "+spec);
						e.printStackTrace();
					}
					PRFCalculator prf = new PRFCalculator();
					double fScore = prf.fScore(m, data.getReferenceMapping());
					double prec = prf.precision(m, data.getReferenceMapping());
					double recall = prf.recall(m, data.getReferenceMapping());
					writeTableEntry(d, fScore, prec, recall, fw);
				}
			}
		}
	}
	
	private void writeTableEntry(double threshold, double fScore, double prec, double recall, FileWriter fw) throws IOException {
		DecimalFormat df =  (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern( "#,###,###0.000" );
		fw.write(""+threshold+";"+df.format(fScore)+";"+df.format(prec)+";"+df.format(recall));
		fw.write(System.getProperty("line.separator"));
		fw.flush();
	}
	private void writeNewCaption(String caption, FileWriter fw) throws IOException {
		fw.write(caption);
		fw.write(System.getProperty("line.separator"));
		fw.flush();
	}
	private void writeNewPlot(String headline, FileWriter fw) throws IOException {
		fw.write(headline);
		fw.write(System.getProperty("line.separator"));
		fw.write("threshold;fScore;precision;recall");
		fw.write(System.getProperty("line.separator"));
		fw.flush();
//		fw.close();
	}
	
	private File createFile(EvaluationData data) {
		File f = new File("resources/thresholdPlots/"+"thresholdPlot_OR_"+data.getName()+".csv");
		return f;
	}
	
	public static void main(String args[]) throws Exception {
		
		DataSets[] all = {
				DataSets.PERSON1,
				DataSets.RESTAURANTS,
				DataSets.DBLPACM,
				DataSets.ABTBUY,
		};
		try {
			for(DataSets data : all) {

				ThresholdPlotter plotter = new ThresholdPlotter();
				plotter.plotComplex(DataSetChooser.getData(DataSets.DBLPACM));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void plotComplex(EvaluationData data) throws Exception{
		List<ThresholdPlotData> answers = new LinkedList();
		FileWriter fw = new FileWriter(createFile(data), false);
		ExecutionPlanner planner = new CanonicalPlanner();
		ExecutionEngine engine = new ExecutionEngine(data.getSourceCache(), 
				data.getTargetCache(), 
				data.getConfigReader().sourceInfo.var, 
				data.getConfigReader().targetInfo.var);
		List<Pair<String>> pairs = data.getPropertyMapping().stringPropPairs;
		for(int i = 0; i<pairs.size()-1; i++) {
			Pair<String> pair1 = pairs.get(i);
			for(int j = i+1; j<pairs.size(); j++)  {
				Pair<String> pair2 = pairs.get(Math.min(j, pairs.size()-1));
				writeNewCaption(pair1.toString() +" - "+pair2.toString(), fw);
				ThresholdPlotData dataPoints = new ThresholdPlotData();
//				ThresholdPlotData dataPoints2 = new ThresholdPlotData();
				dataPoints.name = "ENGINE: "+pair1.toString() +" - "+pair2.toString();
//				dataPoints2.name = "MAPPER: "+pair1.toString() +" - "+pair2.toString();
				List<Double> thresholds = getThresholdDataPoints();
				for(Double t1 : thresholds) {
					for(Double t2 : thresholds) {
						LinkSpec atom1 = construct(data, "levensthein",  pair1.a, pair1.b, t1);
						LinkSpec atom2 = construct(data, "levensthein",  pair2.a, pair2.b, t2);
						LinkSpec and = constructComplex(atom1, atom2, Operator.OR, data);
						String and2 = and.operator.toString()+"("+atom1.getFilterExpression()+"|"+atom1.threshold+","+atom2.getFilterExpression()+"|"+atom2.threshold+")";
//						System.out.println(and2);
						Mapping m = new Mapping();
//						Mapping m2 = new Mapping();
//						SetConstraintsMapper sCMFull = SetConstraintsMapperFactory.getMapper( "simple",  data.getConfigReader().sourceInfo, data.getConfigReader().targetInfo, 
//								data.getSourceCache(), data.getTargetCache(), new LinearFilter(), 2);
						try {

							m = engine.runNestedPlan(planner.plan(and));
//							System.out.println("Execute Mapper on: "+and2);
//							m2 = sCMFull.getLinks(and2, and.threshold);
						} catch(Exception e) {
							System.out.println("Unable to execute spec "+and);
							e.printStackTrace();
						}
						PRFCalculator prf = new PRFCalculator();
						double fScore = prf.fScore(m, data.getReferenceMapping());
//						double fScore2 = prf.computeFScore(m2, data.getReferenceMapping());
						double prec = prf.precision(m, data.getReferenceMapping());
						double recall = prf.recall(m, data.getReferenceMapping());
						dataPoints.addEntry(t1, t2, fScore);
//						dataPoints2.addEntry(t1, t2, fScore2);
					}
				}
				answers.add(dataPoints.copy());
//				answers.add(dataPoints2.copy());
				
				dataPoints.writeData(fw);
				dataPoints.printData();
//				dataPoints2.writeData();
			}
			
		}
//		for(ThresholdPlotData plot : answers) {
//			plot.writeData(fw);
//		}
		fw.close();
	}
	
}
