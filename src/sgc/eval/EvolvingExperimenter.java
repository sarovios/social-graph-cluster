package sgc.eval;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixIO;

import au.com.bytecode.opencsv.CSVWriter;
import sgc.cluster.GraphScope;
import sgc.model.GraphSegment;
import sgc.utils.SyntheticDataset;
import sgc.utils.WriteToCSV;

/**
 * This class simulates the evolving experiments we made to 
 * evaluate the GraphScope algorithm.
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class EvolvingExperimenter {
	
	public static String initialization = "resume";
	
	/**
	 * Simulates the experiments with the synthetic dynamic datasets.
	 * The datasets proposed and described her: http://www.csi.ucd.ie/files/ucd-csi-2011-08.pdf.
	 * In order to performs the experiments with the other synthetic 
	 * dataset you have to set root with the proper value. All the 
	 * datasets are in the data folder, inlcuding the readme file.
	 */
	public void synthDatasetExp() throws IOException {
		int numberOfSnapshots = 10;
		int srcNodes = 1500;
		int dstNodes = 1000;
		String root = "data/data-synth/set1/synth1.t0";
		SyntheticDataset syntheticReader = new SyntheticDataset();
		GraphScope graphScope = new GraphScope();
		Evaluator evaluator = new Evaluator();
		WriteToCSV writeToCSV = new WriteToCSV();
		Map<Integer, Integer> predictedSrcPartitions = new HashMap<Integer, Integer>();
		Map<Integer, Integer> predictedDstPartitions = new HashMap<Integer, Integer>();
		Map<Integer, Integer> actualSrcPartitions = new HashMap<Integer, Integer>();
		Map<Integer, Integer> actualDstPartitions = new HashMap<Integer, Integer>();
		DenseMatrix64F newGraph = new DenseMatrix64F(1500, 1000);
		
		CSVWriter writer = new CSVWriter(new FileWriter("data/Experiments/staticExp1.resume.csv"), ',');
		String[] header = "snapshotID#segmentID#time#cost#srcNMI#dstNMI".split("#");
		writer.writeNext(header);
		
		int snapshotCounter = 1;
		int segmentCounter = 1;
		
		//first snapshot
		GraphSegment graphSegment = new GraphSegment(
				syntheticReader.txtToMatrix(root+1+".mtx", srcNodes, dstNodes));
		graphScope.run(graphSegment, null, initialization);
		predictedSrcPartitions = graphSegment.getSrcNodes();
		predictedDstPartitions = graphSegment.getDstNodes();
		actualSrcPartitions = syntheticReader.nodePartitions(root+1+".flist");
		actualDstPartitions = syntheticReader.nodePartitions(root+1+".clist");
		
		writeToCSV.partitionMapsToCSV(predictedSrcPartitions, "srcPartitions "+1);
		writeToCSV.partitionMapsToCSV(predictedDstPartitions, "dstPartitions "+1);
		writeToCSV.linesPerPartitionToCSV(graphSegment.getRowsPerPartition(), "rowsPerPartition "+1);
		writeToCSV.linesPerPartitionToCSV(graphSegment.getColsPerPartition(), "colsPerPartition "+1);
		writeToCSV.matrixToCSV(graphSegment.getGraph(), "synth1-snapshot "+1);
		double segmentCost = graphSegment.getEncodingCost();
		double estimatedTime = graphScope.getEstimatedTime();
		double similarity = graphScope.getSimilarity();
		double srcNMI = evaluator.evaluateWithNMI(actualSrcPartitions, predictedSrcPartitions);
		double dstNMI = evaluator.evaluateWithNMI(actualDstPartitions, predictedDstPartitions);
		String[] values1 = {String.valueOf(snapshotCounter), String.valueOf(segmentCounter),
				String.valueOf(estimatedTime), String.valueOf(segmentCost),
				String.valueOf(similarity), String.valueOf(srcNMI),
				String.valueOf(dstNMI)};
		writer.writeNext(values1);
		
		//the other snapshots
		for(int i = 1; i < numberOfSnapshots; i++) {
			predictedSrcPartitions.clear();
			predictedDstPartitions.clear();
			newGraph = syntheticReader.txtToMatrix(root+(i+1)+".mtx", srcNodes, dstNodes);
			graphScope.run(graphSegment, newGraph, initialization);
			if(graphScope.newSegment() == true) {
				segmentCounter++;
				graphSegment = graphScope.getNewGraphSegment();
			}
			predictedSrcPartitions.putAll(graphSegment.getSrcNodes());
			predictedDstPartitions.putAll(graphSegment.getDstNodes());
			actualSrcPartitions.putAll(syntheticReader.nodePartitions(root+(i+1)+".flist"));
			actualDstPartitions.putAll(syntheticReader.nodePartitions(root+(i+1)+".clist"));

			writeToCSV.partitionMapsToCSV(predictedSrcPartitions, "srcPartitions "+(i+1));
			writeToCSV.partitionMapsToCSV(predictedDstPartitions, "dstPartitions "+(i+1));
			writeToCSV.linesPerPartitionToCSV(graphSegment.getRowsPerPartition(), "rowsPerPartition "+(i+1));
			writeToCSV.linesPerPartitionToCSV(graphSegment.getColsPerPartition(), "colsPerPartition "+(i+1));
			writeToCSV.matrixToCSV(graphSegment.getGraph(), "synt1-snapshot "+(i+1));
			
			segmentCost = graphSegment.getEncodingCost();
			estimatedTime = graphScope.getEstimatedTime();
			similarity = graphScope.getSimilarity();
			srcNMI = evaluator.evaluateWithNMI(actualSrcPartitions, predictedSrcPartitions);
			dstNMI = evaluator.evaluateWithNMI(actualDstPartitions, predictedDstPartitions);
			String[] values = {String.valueOf(snapshotCounter), String.valueOf(segmentCounter),
					String.valueOf(estimatedTime), String.valueOf(segmentCost),
					String.valueOf(similarity), String.valueOf(srcNMI),
					String.valueOf(dstNMI)};
			writer.writeNext(values);
		}	
		writer.close();
	}

	/**
	 * Simulates the epxeriment with the real dataset. We have construct two
	 * real datasets. Both of them contain the activity that has been recorded
	 * at the Delicious. The first one covers the summer season of 2006 and the second
	 * second one the autum season of 2007. The datasets we construct come from
	 * the dataset described here: http://www.dai-labor.de/en/publication/359.
	 */
	public void deliciousExp() throws IOException {
		int numberOfSnapshots = 12;
		GraphScope graphScope = new GraphScope();
		WriteToCSV writeToCSV = new WriteToCSV();
		
		CSVWriter writer = new CSVWriter(new FileWriter("data/delicious.2006.summer/statistics"), ',');
		String[] header = "snapshotID#segmentID#time#cost#similariy".split("#");
		writer.writeNext(header);
		
		int snapshotCounter = 1;
		int segmentCounter = 1;
		
		//first snapshot
		GraphSegment graphSegment = new GraphSegment(MatrixIO.loadCSV(
				"data/delicious.2006.summer/input/1delicious.matrix"));
		
		//run algorithm
		System.out.println("GraphScope running for snaphot 1....");
		graphScope.run(graphSegment, null, initialization);
		System.out.println("End of running!");
		
		//write predicted values
		System.out.println("Writing results....");
		writeToCSV.partitionMapsToCSV(graphSegment.getSrcNodes(), 1+".bookmarks");
		writeToCSV.partitionMapsToCSV(graphSegment.getDstNodes(), 1+".tags");
		writeToCSV.linesPerPartitionToCSV(graphSegment.getRowsPerPartition(), 1+".rows");
		writeToCSV.linesPerPartitionToCSV(graphSegment.getColsPerPartition(), 1+".columns");
		writeToCSV.numberOfOnesToCSV(graphSegment.getNumberOfOnes(), 1+".delicious");
		writeToCSV.matrixToCSV(graphSegment.getGraph(), 1+".delicious");
		double segmentCost = graphSegment.getEncodingCost();
		double estimatedTime = graphScope.getEstimatedTime();
		double similarity = graphScope.getSimilarity();
		String[] values1 = {String.valueOf(snapshotCounter), String.valueOf(segmentCounter),
				String.valueOf(estimatedTime), String.valueOf(segmentCost),
				String.valueOf(similarity)};
		writer.writeNext(values1);
		writer.flush();
		System.out.println("End of writing");
		
		//the rest sequence of graph snapshost
		for(int i = 1; i < numberOfSnapshots; i++) {
			System.out.println("GraphScope running for snaphot "+i+"....");			
			graphScope.run(graphSegment, MatrixIO.loadCSV(
					"data/delicious.2006.summer/input/"+(i+1)+"delicious.matrix"), initialization);
			System.out.println("End of running!");
			if(graphScope.newSegment() == true) {
				System.out.println("Milestone detected. Create new segment");
				segmentCounter++;
				graphSegment = graphScope.getNewGraphSegment();
			}
			
			//write predicted values
			System.out.println("Writing results....");
			writeToCSV.partitionMapsToCSV(graphSegment.getSrcNodes(), (i+1)+".bookmarks");
			writeToCSV.partitionMapsToCSV(graphSegment.getDstNodes(), (i+1)+".tags");
			writeToCSV.linesPerPartitionToCSV(graphSegment.getRowsPerPartition(), (i+1)+".rows");
			writeToCSV.linesPerPartitionToCSV(graphSegment.getColsPerPartition(), (i+1)+".columns");
			writeToCSV.numberOfOnesToCSV(graphSegment.getNumberOfOnes(), (i+1)+".delicious");
			writeToCSV.matrixToCSV(graphSegment.getGraph(), (i+1)+".delicious");
			segmentCost = graphSegment.getEncodingCost();
			estimatedTime = graphScope.getEstimatedTime();
			similarity = graphScope.getSimilarity();
			String[] values = {String.valueOf(snapshotCounter), String.valueOf(segmentCounter),
					String.valueOf(estimatedTime), String.valueOf(segmentCost),
					String.valueOf(similarity)};
			writer.writeNext(values);
			writer.flush();
			System.out.println("End of writing");
			snapshotCounter++;
		}
		writer.close();
	}
}
