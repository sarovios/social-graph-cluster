package sgc.eval;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

import sgc.cluster.GraphScope;
import sgc.model.GraphSegment;
import sgc.utils.Calculations;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class simulates the experiments with the static graph benchmarks.
 * The evaluation is carried by comparing the ground truth clusterings with
 * the predicted clusterings. The comparison is made by calculating the
 * NMI (normalized mutual information). After the experiment we write the
 * statistics to a csv file.
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class StaticExperimenter {
	
	private static final String EXP1_OUTPATH =  "data/Experiments/entropy.staticExp1.csv";
	private static final String EXP2_OUTPATH = "data/Experiments/entropy.staticExp2.csv";
	private static final String EXP3_OUTPATH = "data/Experiments/entropy.staticExp3.csv";
	private static final int NUMBER_OF_RUNS = 1;
	private static final String INITIALIAZTION = "freshStart";
	
	/**
	 * Static experiment with fully symmetric graphs.
	 */
	public void experiment1() throws IOException {
		Calculations calculations = new Calculations();
		CSVWriter writer = new CSVWriter(new FileWriter(EXP1_OUTPATH),
				CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		String[] header = 
				"cost#nodesN#k_in#k_out#partSize#mean_sNMI#mean_dNMI#std_sNMI#std_dNMI#mean_estTime#std_estTime".split("#");
		writer.writeNext(header);
		int[] numberOfNodes = {500};
		double[] k_in = {0.01};
		double[] k_out = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1};
		int[] partitionsSize = {100};
		double[] srcPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] dstPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] estTime = new double[NUMBER_OF_RUNS];
		BenchmarkGenerator benchmarkGenerator = new BenchmarkGenerator();
		DenseMatrix64F graph = null;
		for(int i = 0; i < numberOfNodes.length; i++) {
			int numNodes = numberOfNodes[i];
			for(int j = 0; j < k_in.length; j++) {
				double kIn = k_in[j];
				for(int k = 0; k < k_out.length; k++) {
					double kOut = k_out[k];
					for(int l = 0 ; l < partitionsSize.length; l++) {
						int partSize = partitionsSize[l];
						System.out.println("Experiment with values: N: "+numNodes+" k_in: "+kIn+ 
								" k_out: "+kOut+" partSize: "+partSize);
						for(int z = 0; z < NUMBER_OF_RUNS; z++) {
							benchmarkGenerator.generateFullySymmetric(numNodes, partSize, kIn, kOut);
							graph = benchmarkGenerator.getAdjacencyMatrix();
							
							GraphSegment graphSegment = new GraphSegment(graph);
							GraphScope graphScope = new GraphScope();
							graphScope.run(graphSegment, null, INITIALIAZTION);
							Map<Integer, Integer> actualSrcPartitions = benchmarkGenerator.getSrcPartitions();
							Map<Integer, Integer> actualDstPartitions = benchmarkGenerator.getDstPartitions();
							Map<Integer, Integer> predictedSrcPartitions = graphSegment.getSrcNodes();
							Map<Integer, Integer> predictedDstPartitions = graphSegment.getDstNodes();
							
							Evaluator evaluator = new Evaluator();
							srcPartitionsNMI[z] = evaluator.evaluateWithNMI(actualSrcPartitions, predictedSrcPartitions);
							dstPartitionsNMI[z] = evaluator.evaluateWithNMI(actualDstPartitions, predictedDstPartitions);
							System.out.println("srcPartitionsNMI= "+srcPartitionsNMI[z]);
							System.out.println("dstPartitionsNMI= "+dstPartitionsNMI[z]);
							estTime[z] = graphScope.getEstimatedTime();
						}
						double meanSrcNMI = calculations.calculateMean(srcPartitionsNMI);
						double varSrcNMI = calculations.calculateVariance(meanSrcNMI, srcPartitionsNMI);
						double stdSrcNMI = calculations.calculateStdDeviation(varSrcNMI);
						double meanDstNMI = calculations.calculateMean(dstPartitionsNMI);
						double varDstNMI = calculations.calculateVariance(meanDstNMI, dstPartitionsNMI);
						double stdDstNMI = calculations.calculateStdDeviation(varDstNMI);
						double meanEstTime = calculations.calculateMean(estTime);
						double varEstTime = calculations.calculateVariance(meanEstTime, estTime);
						double stdEstTime = calculations.calculateStdDeviation(varEstTime);
						String[] values = {"entropy cost", Integer.toString(numNodes), 
								Double.toString(kIn), Double.toString(kOut), 
								Integer.toString(partSize), Double.toString(meanSrcNMI), 
								Double.toString(meanDstNMI), Double.toString(stdSrcNMI), 
								Double.toString(stdDstNMI), Double.toString(meanEstTime),
								Double.toString(stdEstTime)};
						writer.writeNext(values);
						writer.flush();
					}
				}
			}
		}
		writer.close();
	}
	
	/**
	 * Static experiment with not symmetric graphs.
	 */
	public void experiment2() throws IOException {
		Calculations calculations = new Calculations();
		CSVWriter writer = new CSVWriter(new FileWriter(EXP2_OUTPATH), ',');
		String[] header = "cost#srcN#dstN#p_Min#p_Max#k_in#k_out#mean_sNMI#mean_dNMI#std_sNMI#std_dNMI#mean_estTime#std_estTime".split("#");
		writer.writeNext(header);
		int[] numberOfSrcNodes = {500};
		int[] numberOfDstNodes = {800};
		double[] k_in = {0.01};
		double[] k_out = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1};
		int[] partSizesMin = {80};
		int[] partSizesMax = {700};
		double[] srcPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] dstPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] estTime = new double[NUMBER_OF_RUNS];
		BenchmarkGenerator benchmarkGenerator = new BenchmarkGenerator();
		DenseMatrix64F graph = null;
		for(int i = 0; i < numberOfSrcNodes.length; i++) {
			int srcNodes = numberOfSrcNodes[i];
			int dstNodes = numberOfDstNodes[i];
			for(int j = 0; j < k_in.length; j++) {
				double kIn = k_in[j];
				for(int k = 0; k < k_out.length; k++) {
					double kOut = k_out[k];
					for(int l = 0 ; l < partSizesMin.length; l++) {
						int pSizeMin = partSizesMin[l];
						int pSizeMax = partSizesMax[l];
						System.out.println("Experiment with values: srcN: "+srcNodes+" dstN: "+dstNodes+
								" k_in: "+kIn+ " k_out: "+kOut+" p_Min: "+pSizeMin+" p_Max: "+pSizeMax);
						for(int z = 0; z < NUMBER_OF_RUNS; z++) {
							benchmarkGenerator.generateNotSymmRandSizes(srcNodes, dstNodes, pSizeMin, pSizeMax, kIn, kOut);
							graph = benchmarkGenerator.getAdjacencyMatrix();
							
							GraphSegment graphSegment = new GraphSegment(graph);
							GraphScope graphScope = new GraphScope();
							graphScope.run(graphSegment, null, INITIALIAZTION);
							Map<Integer, Integer> actualSrcPartitions = benchmarkGenerator.getSrcPartitions();
							Map<Integer, Integer> actualDstPartitions = benchmarkGenerator.getDstPartitions();
							Map<Integer, Integer> predictedSrcPartitions = graphSegment.getSrcNodes();
							Map<Integer, Integer> predictedDstPartitions = graphSegment.getDstNodes();
							
							Evaluator evaluator = new Evaluator();
							srcPartitionsNMI[z] = evaluator.evaluateWithNMI(actualSrcPartitions, predictedSrcPartitions);
							dstPartitionsNMI[z] = evaluator.evaluateWithNMI(actualDstPartitions, predictedDstPartitions);
							System.out.println("srcPartitionsNMI= "+srcPartitionsNMI[z]);
							System.out.println("dstPartitionsNMI= "+dstPartitionsNMI[z]);
							estTime[z] = graphScope.getEstimatedTime();
						}
						double meanSrcNMI = calculations.calculateMean(srcPartitionsNMI);
						double varSrcNMI = calculations.calculateVariance(meanSrcNMI, srcPartitionsNMI);
						double stdSrcNMI = calculations.calculateStdDeviation(varSrcNMI);
						double meanDstNMI = calculations.calculateMean(dstPartitionsNMI);
						double varDstNMI = calculations.calculateVariance(meanDstNMI, dstPartitionsNMI);
						double stdDstNMI = calculations.calculateStdDeviation(varDstNMI);
						double meanEstTime = calculations.calculateMean(estTime);
						double varEstTime = calculations.calculateVariance(meanEstTime, estTime);
						double stdEstTime = calculations.calculateStdDeviation(varEstTime);
						String[] values = {"entropy", Integer.toString(srcNodes), 
								Integer.toString(dstNodes), Integer.toString(pSizeMin), 
								Integer.toString(pSizeMax), Double.toString(kIn), 
								Double.toString(kOut), Double.toString(meanSrcNMI), 
								Double.toString(meanDstNMI), Double.toString(stdSrcNMI), 
								Double.toString(stdDstNMI), Double.toString(meanEstTime),
								Double.toString(stdEstTime)};
						writer.writeNext(values);
						writer.flush();
					}
				}
			}
		}
		writer.close();
	}
	
	/**
	 * Experiment with assymetric graphs. The assymetry focus on the size 
	 * of the clusters since the number of source and destination nodes are
	 * equal and since the source and destination partitions that correspond 
	 * to a cluster are equal too. [A cluster is formed from a source and a destination
	 * partition].
	 */
	public void experiment3() throws IOException {
		Calculations calculations = new Calculations();
		CSVWriter writer = new CSVWriter(new FileWriter(EXP3_OUTPATH), ',');
		String[] header = 
				"cost#srcN#dstN#p_Num#k_in#k_out#mean_sNMI#mean_dNMI#std_sNMI#std_dNMI#mean_estTime#std_estTime".split("#");
		writer.writeNext(header);
		int[] numberOfSrcNodes = {500};
		int[] numberOfDstNodes = {500};
		double[] k_in = {0.01};
		double[] k_out = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1};
		int[] numberOfPartitions = {5};
		double[] srcPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] dstPartitionsNMI = new double[NUMBER_OF_RUNS];
		double[] estTime = new double[NUMBER_OF_RUNS];
		BenchmarkGenerator benchmarkGenerator = new BenchmarkGenerator();
		DenseMatrix64F graph = null;
		for(int i = 0; i < numberOfSrcNodes.length; i++) {
			int srcNodes = numberOfSrcNodes[i];
			int dstNodes = numberOfDstNodes[i];
			for(int j = 0; j < k_in.length; j++) {
				double kIn = k_in[j];
				for(int k = 0; k < k_out.length; k++) {
					double kOut = k_out[k];
					for(int l = 0 ; l < numberOfPartitions.length; l++) {
						int numPartitions = numberOfPartitions[l];
						System.out.println("Experiment with values: srcN: "+srcNodes+" dstN: "+dstNodes+
								" k_in: "+kIn+ " k_out: "+kOut+" p_Num: "+numPartitions);
						for(int z = 0; z < NUMBER_OF_RUNS; z++) {
							benchmarkGenerator.generateUnEqualClusters(srcNodes, dstNodes, 
									numPartitions, kIn, kOut);
							graph = benchmarkGenerator.getAdjacencyMatrix();
							
							GraphSegment graphSegment = new GraphSegment(graph);
							GraphScope graphScope = new GraphScope();
							graphScope.run(graphSegment, null, INITIALIAZTION);
							Map<Integer, Integer> actualSrcPartitions = benchmarkGenerator.getSrcPartitions();
							Map<Integer, Integer> actualDstPartitions = benchmarkGenerator.getDstPartitions();
							Map<Integer, Integer> predictedSrcPartitions = graphSegment.getSrcNodes();
							Map<Integer, Integer> predictedDstPartitions = graphSegment.getDstNodes();
							
							Evaluator evaluator = new Evaluator();
							srcPartitionsNMI[z] = evaluator.evaluateWithNMI(actualSrcPartitions, predictedSrcPartitions);
							dstPartitionsNMI[z] = evaluator.evaluateWithNMI(actualDstPartitions, predictedDstPartitions);
							System.out.println("srcPartitionsNMI= "+srcPartitionsNMI[z]);
							System.out.println("dstPartitionsNMI= "+dstPartitionsNMI[z]);
							estTime[z] = graphScope.getEstimatedTime();					
						}
						double meanSrcNMI = calculations.calculateMean(srcPartitionsNMI);
						double varSrcNMI = calculations.calculateVariance(meanSrcNMI, srcPartitionsNMI);
						double stdSrcNMI = calculations.calculateStdDeviation(varSrcNMI);
						double meanDstNMI = calculations.calculateMean(dstPartitionsNMI);
						double varDstNMI = calculations.calculateVariance(meanDstNMI, dstPartitionsNMI);
						double stdDstNMI = calculations.calculateStdDeviation(varDstNMI);
						double meanEstTime = calculations.calculateMean(estTime);
						double varEstTime = calculations.calculateVariance(meanEstTime, estTime);
						double stdEstTime = calculations.calculateStdDeviation(varEstTime);
						String[] values = {"entropy", Integer.toString(srcNodes), 
								Integer.toString(dstNodes),Integer.toString(numPartitions), 
								Double.toString(kIn), Double.toString(kOut), 
								Double.toString(meanSrcNMI), Double.toString(meanDstNMI), 
								Double.toString(stdSrcNMI), Double.toString(stdDstNMI), 
								Double.toString(meanEstTime), Double.toString(stdEstTime)};
						writer.writeNext(values);
						writer.flush();
					}
				}
			}
		}
		writer.close();
	}

}
