package sgc.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.data.MatrixIterator;
import org.ejml.ops.MatrixIO;

import sgc.eval.Evaluator;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class handles the the writing utilities
 * 
 * @author smpeis
 * @email sot.beis@gmail.com
 *
 */
public class WriteToCSV {
	
	String ResultsPath = "data/data-synth/set1/results/"; //this is where the results after experiment is saved.
												   //you should change this depending which experiment you run.
		
	/**
	 * Write the partition sizes to a csv file
	 * attrinute = rows or columns (source or destination partitions)
	 */
	public void writePartitionsSize(String partitionsPath, String attribute, 
			String outputPath) {
		int numberOfSnapshots = 12;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			String[] header = {"Snapshot", "Partition", "Size"};
			writer.writeNext(header);
			int snapshotId = 1;
			for(int i = 0; i < numberOfSnapshots; i++) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(partitionsPath+(i+1)+"."+attribute+".list")));
				String line;
				int partitionId = 1;
				while((line = reader.readLine()) != null) {
					String partitionSize = line;
					String[] values = {String.valueOf(snapshotId), String.valueOf(partitionId),
							partitionSize};
					writer.writeNext(values);
					partitionId++;
				}
				reader.close();
				snapshotId++;
			}
			writer.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Write the NMI values to a csv file
	 */
	public void writeNMIvalues(String inputPath, String outputPath, String attribute) {
		try {
			Evaluator evaluator = new Evaluator();
			BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			String[] header = {"NMI"};
			writer.writeNext(header);
			for(int i = 1; i < 5; i++) {
				double nmi = evaluator.calculateNMIBetweenPartitions(inputPath+attribute+" "+i+".plist",
						inputPath+attribute+" "+(i+1)+".plist");
				String[] values = {String.valueOf(nmi)};
				writer.writeNext(values);
			}
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write to a csv file the nodes (label of nodes) per partition
	 */
	public void labelNodesPerPartition(String labelPath, String nodesPath, String title) {
		Map<Integer, String> labels = getLabels(labelPath);
		Map<Integer, Integer> nodesMap = getNodes(nodesPath);
		Map<Integer, List<Integer>> nodesPerPartition = mapNodesToPartitions(nodesMap);
		int counter = 0;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(title));
			for(Map.Entry<Integer, List<Integer>> entry : nodesPerPartition.entrySet()) {
				int partitionId = entry.getKey();
				List<Integer> nodes = entry.getValue();
				writer.write("Partition "+partitionId+": {");
				for(int node : nodes) {
					String label = labels.get(node);
					
					writer.write(label+",");
					counter++;
					if(label.equals(null)) {
						System.out.println();
					}
					System.out.println(label);
				}
				System.out.println(counter);
				counter = 0;
				writer.write("}");
				writer.newLine();
			}
			writer.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Assign each node to a list of integer that corresponds to each partition
	 */
	public Map<Integer, List<Integer>> mapNodesToPartitions(Map<Integer, Integer> partitions) {
		Map<Integer, List<Integer>> nodesPerPartition = new HashMap<Integer, List<Integer>>();
		for(Map.Entry<Integer, Integer> entry : partitions.entrySet()) {
			int nodeId = entry.getKey();
			int partitionId = entry.getValue();
			if(nodesPerPartition.containsKey(partitionId)) {
				nodesPerPartition.get(partitionId).add(nodeId);
			}
			else {
				List<Integer> nodes = new ArrayList<Integer>();
				nodes.add(nodeId);
				nodesPerPartition.put(partitionId, nodes);
			}
		}
		return nodesPerPartition;
	}

	
	/**
	 * Get the node assigment per partition
	 */
	public Map<Integer, Integer> getNodes(String root) {
		Map<Integer, Integer> nodes = new HashMap<Integer, Integer>();
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(root));
			String[] nextLine;
			int nodeCounter = 1;
			while((nextLine = reader.readNext()) != null) {
				//String nodeId = nextLine[0];
				String partitionId = nextLine[0];
				//int node = Integer.parseInt(nodeId);
				int partition = Integer.parseInt(partitionId);
				nodes.put(nodeCounter, partition);
				nodeCounter++;
			}
			reader.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return nodes;
	}

	
	/**
	 * Map label with their id
	 */
	public Map<Integer, String> getLabels(String root) {
		Map<Integer, String> labels = new HashMap<Integer,String>();
		int labelId = 1;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(root)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				labels.put(labelId, parts[0]);
				labelId++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return labels;
	}
	
	/**
	 * Writes the values of a distibution to a csv file
	 */
	public void writeDistributions(List<Double> dist, String title) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("data/"+title));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			for(double i : dist) {
				String[] value = {String.valueOf(i)};
				writer.writeNext(value);
			}
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write values in a way that gephi can visualize bipartite graphs
	 */
	public void writeFilesForGephi(int srcPartitionId, int dstPartitionId, String snapshotPath,
			List<Double> rowsPerPartition, List<Double> colsPerPartition,
			Map<String, Integer> tags, Map<String, Integer> bookmarks) {
		DeliciousDataset deliciousDataset = new DeliciousDataset();
		double sourceSize = rowsPerPartition.get(srcPartitionId-1);
		double destinationSize = colsPerPartition.get(dstPartitionId-1);
		
		//Get the subgraph that corresponds to the cluster
		DenseMatrix64F subGraph = new DenseMatrix64F((int)sourceSize, (int)destinationSize);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(snapshotPath)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				if(parts.length == 4) {
					String bookmark = parts[2];
					String tag = parts[3];
					if((bookmarks.containsKey(bookmark)) && (tags.containsKey(tag))) {
						int bookmarkId = bookmarks.get(bookmark);
						int tagId = tags.get(tag);
						double value = subGraph.get(bookmarkId-1, tagId-1);
						if(value == 0) {
							subGraph.set(bookmarkId-1, tagId-1, 1);
						}
					}
				}
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		//write the edge list
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("data/edge.list.csv"));
			CSVWriter writer = new CSVWriter(out);
			String[] header = {"Source","Target","Type","Id"};
			writer.writeNext(header);
			int edgeId = 1;
			for(int i = 0; i < subGraph.getNumRows(); i++) {
				for(int j = 0; j < subGraph.getNumCols(); j++) {
					double value = subGraph.get(i, j);
					if(value == 1) {
						String[] values = {String.valueOf(i), String.valueOf(j+subGraph.getNumRows()),
								"Undirected", String.valueOf(edgeId)};
						writer.writeNext(values);
						edgeId++;
					}
				}
			}
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		//write the node list
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("data/node.list.csv"));
			CSVWriter writer = new CSVWriter(out);
			String[] header = {"Nodes","Id","Label","Attribute","yPosition","xPosition"};
			writer.writeNext(header);
			int nodeId = 0;
			double xPosition = 0.5; 
			
			//write the nodes that correspond to a bookmark
			for(int i = 0; i < subGraph.getNumRows(); i++) {
				String label = deliciousDataset.getKeyByValue(bookmarks, i+1);
				String[] values = {label,String.valueOf(nodeId),label,"Bookmark","0",String.valueOf(xPosition)};
				writer.writeNext(values);
				nodeId++;
				xPosition = xPosition + 1;
			}
			
			xPosition = 0.5;
			nodeId++;
			//write the nodes tha correspond to a tag
			for(int i = 0; i < subGraph.getNumCols(); i++) {
				String label = deliciousDataset.getKeyByValue(tags, i+1);
				String[] values = {label,String.valueOf(nodeId),label,"Tag","10",String.valueOf(xPosition)};
				writer.writeNext(values);
				nodeId++;
				xPosition = xPosition + 1;
			}
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the matrix to a csv file as na edge list. The csv
	 * file is suitable for input to matlab,
	 */
	public void matrixToCSV(DenseMatrix64F matrix, String title) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter
					("data/"+title+".mtx"));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			
			//String[] header = {"Source","Target","Type"};
			//writer.writeNext(header);
			for(int i = 0; i < matrix.getNumRows(); i++) {
				for(int j = 0; j < matrix.getNumCols(); j++) {
					double value = matrix.get(i, j);
					if(value > 0) {
						String fromNode = String.valueOf(i+1);
						String toNode = String.valueOf(j+1);
						String weight = String.valueOf(value);
						String[] values = {fromNode, toNode, weight};
						writer.writeNext(values);
					}
				}
			}
			writer.close();
			out.close();
		} 
		catch (IOException e) { 
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public void numberOfOnesToCSV(DenseMatrix64F numberOfOnes, String title) throws IOException {
		MatrixIO.saveCSV(numberOfOnes, ResultsPath+title+".ones");
	}
	
	/**
	 * Writes the partitions to a CSV file. It has only one column
	 * corresponds to the partition id. The number of the row
	 * corresponds to the node id. The value of the path should be changed
	 * in order to save the file in the desired path 
	 */
	public void partitionMapsToCSV(Map<Integer, Integer> partitions, String title) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter
				(ResultsPath+title+".plist"));
		CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		Collection<Integer> nodes = partitions.keySet();
		List<Integer> nodesList = new ArrayList<Integer>(nodes);
		Collections.sort(nodesList);
		for(int nodeId : nodesList) {
			String partitionId = String.valueOf(partitions.get(nodeId));
			String[] values = {partitionId};
			writer.writeNext(values);
		}
		writer.close();
	}
	
	/**
	 * Writes to a CSV file how many nodes (lines) has a each
	 * partition. The value of the path should be changed
	 * in order to save the file in the desired path
	 */
	public void linesPerPartitionToCSV(DenseMatrix64F linesPerPartition, String title) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter
				(ResultsPath+title+".list"));
		CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		
		MatrixIterator iterator = linesPerPartition.iterator(true, 0, 0, linesPerPartition.getNumRows()-1, 
				linesPerPartition.getNumCols()-1);
		while(iterator.hasNext()) {
			String lines = String.valueOf(iterator.next());
			String[] values = {lines};
			writer.writeNext(values);
		}
		writer.close();
	}
	
}
