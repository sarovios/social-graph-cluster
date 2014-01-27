package sgc.eval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

import sgc.utils.Calculations;
import sgc.utils.DeliciousDataset;
import sgc.utils.WriteToCSV;

/**
 * This class contains all the necessary functions for the
 * evaluation of the algorithm results. 
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */

public class Evaluator {
	
	static public void main(String[] args) throws IOException {
		Evaluator evaluator = new Evaluator();		
		evaluator.evaluateRealData();
	}
	
	/**
	 * Calculates the NMI beetweent two partitions
	 */
	public double calculateNMIBetweenPartitions(String partitionPath1, String partitionPath2) {
		DeliciousDataset deliciousDataset = new DeliciousDataset();
		List<Double> partitionList1 = deliciousDataset.readlinesPerPartition(partitionPath1);
		List<Double> partitionList2 = deliciousDataset.readlinesPerPartition(partitionPath2);
		Map<Integer, Integer> partitions1 = new HashMap<Integer, Integer>();
		Map<Integer, Integer> partitions2 = new HashMap<Integer, Integer>();
		int nodeCounter = 1;
		for(double partitionId : partitionList1) {
			partitions1.put(nodeCounter, (int)partitionId);
			nodeCounter++;
		}
		nodeCounter = 1;
		for(double partitionId : partitionList2) {
			partitions2.put(nodeCounter, (int)partitionId);
			nodeCounter++;
		}
		return evaluateWithNMI(partitions1, partitions2);
		
	}
	
	/**
	 * For the evaluation of the tag partition we use Normalized Google
	 * Distance in order to make two distibutions the Fsame that corresponds
	 * to the similarity of the tags in the same partition and the Fdiff
	 * distribution that corresponds to the similarity of tags that connect 
	 * with each other but they don't belong to the same partition. Since we
	 * work we bipartite graphs the tags connect only with bookmarks (in our case study
	 * which has to do with the delicious). So in order to build the Fdiff we
	 * first construct the coccurrent tag graph.
	 * 
	 */
	public void evaluateRealData() throws NumberFormatException, IOException {
		DeliciousDataset deliciousDataset = new DeliciousDataset();
		WriteToCSV writeToCSV = new WriteToCSV(); 
		System.out.println("Getting tags...");
		Map<String, Integer> tags = deliciousDataset.getTopTags("data/delicious.2006.summer/dataset/top1500.tags");
		System.out.println("Done!");
		System.out.println("Creating tagGraph...");
		DenseMatrix64F tagGraph = deliciousDataset.createCoCurrentTagGraph(
				"data/delicious.2006.summer/dataset/snapshot 12",
				"data/delicious.2006.summer/dataset/top1500.tags", 
				"data/delicious.2006.summer/dataset/top3000.bookmarks");
		System.out.println("Done!");
		
		for(int i = 1; i < 13; i++) {
			System.out.println("### Partition "+i+"###");
			System.out.println("Getting tags from the same partition...");
			List<Integer> tagsFromTheSamePartition = getTagsFromTheSamePartition(
					"data/delicious.2006.summer/entropy.results/12.tags.plist", i);
			System.out.println("Done!");
			System.out.println("Calculating Fsame...");
			List<Double> Fsame = calculateFsame(tagsFromTheSamePartition, tags);
			System.out.println("Done!");
			System.out.println("Calculating Fdiff...");
			List<Double> Fdiff = calculateFdiff(tagsFromTheSamePartition, tagGraph, tags);
			System.out.println("Done!");
			writeToCSV.writeDistributions(Fsame, i+".Fsame.dist");
			writeToCSV.writeDistributions(Fdiff, i+".Fdiff.dist");
		}
	}
	
	/**
	 * Calculate Fdiff distribution
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public List<Double> calculateFdiff(List<Integer> tagsFromTheSamePartition, DenseMatrix64F tagGraph,
			Map<String, Integer> tags) 
			throws NumberFormatException, IOException {
		NormalizedGoogleDistance ngd = new NormalizedGoogleDistance(); 
		ngd.clearCache();
		DeliciousDataset deliciousDataset = new DeliciousDataset();
		List<Double> Fdiff = new ArrayList<Double>();
		for(int tagId1 : tagsFromTheSamePartition) {
			for(int tagId2 = 1; tagId2 <= tagGraph.getNumCols(); tagId2++) {
				if(!tagsFromTheSamePartition.contains(tagId2)) {
					double value = tagGraph.get(tagId1-1, tagId2-1);
					if(value == 1) {
						String term1 = deliciousDataset.getKeyByValue(tags, tagId1);
						String term2 = deliciousDataset.getKeyByValue(tags, tagId2);
						double ngdDistance = ngd.calculateDistance(term1, term2);
						System.out.println(term1 + " from " + term2 + " has distance= "+ngdDistance);
						Fdiff.add(ngdDistance);
					}
				}
			}
		}
		return Fdiff;
	}
	
	/**
	 * Returns the tags from the same partition
	 */
	public List<Integer> getTagsFromTheSamePartition(String tagsPerPartitionPath, int partitionId) {
		List<Integer> tagsFromSamePartition = new ArrayList<Integer>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tagsPerPartitionPath)));
			String line;
			int tagId = 1;
			while((line = reader.readLine()) != null) {
				if(partitionId == Integer.parseInt(line)) {
					tagsFromSamePartition.add(tagId);
				}
				tagId++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return tagsFromSamePartition;
	}
	
	/**
	 * Calculate Fsame distribution
	 */
	public List<Double> calculateFsame(List<Integer> tagsFromSamePartition, Map<String, Integer> tags) 
			throws NumberFormatException, IOException{
		NormalizedGoogleDistance ngd = new NormalizedGoogleDistance(); 
		ngd.clearCache();
		DeliciousDataset deliciousDataset = new DeliciousDataset();
		List<Double> Fsame = new ArrayList<Double>();
		for(int i = 0; i < tagsFromSamePartition.size(); i++) {
			for(int j = i+1; j < tagsFromSamePartition.size(); j++) {
				int tagId1 = tagsFromSamePartition.get(i);
				int tagId2 = tagsFromSamePartition.get(j);
				String term1 = deliciousDataset.getKeyByValue(tags, tagId1);
				String term2 = deliciousDataset.getKeyByValue(tags, tagId2);
				double ngdDistance = ngd.calculateDistance(term1, term2);
				System.out.println(term1 + " from " + term2 + " has distance= "+ngdDistance);
				Fsame.add(ngdDistance);
			}
		}
		return Fsame;
	}
	
	/**
	 * Evaluate synthetic data with NMI
	 */
	public double evaluateWithNMI(Map<Integer, Integer> actualPartitions, Map<Integer, Integer> predictedPartitions) {
		Metrics metrics = new Metrics();
		Calculations calculations = new Calculations();
		Map<Integer, List<Integer>> actualPart = calculations.mapPartitions(actualPartitions);
		Map<Integer, List<Integer>> predictedPart = calculations.mapPartitions(predictedPartitions);
		int numberOfNodes = actualPartitions.size();
		double metric = metrics.normalizedMutualInformation(numberOfNodes, actualPart, predictedPart);
		return metric;
	}
	
}
