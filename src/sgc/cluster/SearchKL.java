
package sgc.cluster;

import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SpecializedOps;
import org.ejml.simple.SimpleMatrix;

import sgc.utils.Calculations;

/**
 * This class implements the SearchKL function of the GraphScope algorithm.
 * SearchKL finds the optimum number of source and destination partitions.
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class SearchKL {
	
	static final double epsilon = 0.01;
	
	int numberOfPartitions;
	double cost;
		
	public void merge(int numOfRowPartitions, int numOfColPartitions, int numOfYNodes, int numOfXNodes, 
			int segmentSize, double cost, DenseMatrix64F numberOfOnes,
			DenseMatrix64F yLinesPerPartition, DenseMatrix64F xLinesPerPartition, 
			Map<Integer, Integer> xNodes, boolean searchK) {
		this.numberOfPartitions = numOfColPartitions;
		this.cost = cost;
		GraphCost graphCost = new GraphCost();
		double minMergedCost = Double.POSITIVE_INFINITY;
		int mergedNumOfColPartitions = this.numberOfPartitions - 1;
		int partitionId1 = 0;
		int partitionId2 = 0;
		DenseMatrix64F mergedNumberOfOnes = new DenseMatrix64F(numberOfOnes);
		DenseMatrix64F mergedXLinesPerPartition = new DenseMatrix64F(xLinesPerPartition);
		//find the partitions that gives the smallest encoding cost by merging them
		if(mergedNumOfColPartitions > 0) {
			for(int i = 0; i < this.numberOfPartitions; i++) {
				for(int j = i+1; j < this.numberOfPartitions; j++) {
					mergeCols(i, j, mergedNumberOfOnes);
					mergeCols(i, j, mergedXLinesPerPartition);
					double mergedCost = graphCost.segmentEncodingCost(numOfYNodes, numOfXNodes, numOfRowPartitions, 
							mergedNumOfColPartitions, segmentSize, mergedNumberOfOnes,
							yLinesPerPartition, mergedXLinesPerPartition);
					if(mergedCost < minMergedCost) {
						minMergedCost = mergedCost;
						partitionId1 = i;
						partitionId2 = j;
					}
					mergedNumberOfOnes.setReshape(numberOfOnes);
					mergedXLinesPerPartition.setReshape(xLinesPerPartition);
				}
			}
			if(minMergedCost < this.cost) {
				System.out.println("merge");
				this.numberOfPartitions = mergedNumOfColPartitions;
				this.cost = minMergedCost;
				mergeCols(partitionId1, partitionId2, numberOfOnes);
				mergeCols(partitionId1, partitionId2, xLinesPerPartition);
				//add one so array id correspond to partitionid
				mergeNodesMap(xNodes, partitionId1+1, partitionId2+1); 
			}
		}
	}
	
	public void split(int numOfRowPartitions, int numOfColPartitions, int numberOfYNodes, int numberOfXNodes, 
			int segmentSize, DenseMatrix64F numberOfOnes,  DenseMatrix64F yLinesPerPartition, 
			DenseMatrix64F xLinesPerPartition, DenseMatrix64F graph, 
			Map<Integer, Integer> yNodes, Map<Integer, Integer> xNodes, 
			boolean searchK) {
		Calculations calculations = new Calculations();
		this.numberOfPartitions = numOfColPartitions;

		//Calculate the entropy of the segment initially
		DenseMatrix64F blocksSize = calculations.calcBlocksSize(segmentSize, yLinesPerPartition, 
				xLinesPerPartition);
		DenseMatrix64F entropyPerBlock = calculations.calcBlocksEntropy(blocksSize, numberOfOnes);
		
		//Calculate entropy per node of each column partition
		DenseMatrix64F averageEntropy = new DenseMatrix64F(1, this.numberOfPartitions);
		CommonOps.sumCols(entropyPerBlock, averageEntropy);
		
		//Find the parition with the max entropy
		double maxAverageEntropy = CommonOps.elementMax(averageEntropy);
		
		//find the id of the partition with the maximum entropy
		int maxEntropyPartitionId = calculations.findPartitionId(averageEntropy, maxAverageEntropy);
		
		//find the partition with the maximum entropy
		List<Integer> maxEntropyPartition = calculations.getPartition(maxEntropyPartitionId, xNodes);
		
		//Find maxEntropyPartition number of ones 
		DenseMatrix64F maxEntropyPartOnes = CommonOps.extract(numberOfOnes, 0, numOfRowPartitions,
				maxEntropyPartitionId-1, maxEntropyPartitionId);
		
		//Compute the dimensions and number of columns of the max entropy partitions after 
		//the removal of one column
		DenseMatrix64F maxEntropyPartBlocksSize = new DenseMatrix64F(numOfRowPartitions, 1);
		CommonOps.scale(xLinesPerPartition.get(maxEntropyPartitionId-1)-1, yLinesPerPartition, 
				maxEntropyPartBlocksSize);		
		CommonOps.scale(segmentSize, maxEntropyPartBlocksSize);
		DenseMatrix64F leftoverOnes = new DenseMatrix64F(numOfRowPartitions, 1);
		double splittedNumCols = xLinesPerPartition.get(maxEntropyPartitionId-1)-1;
		int numberOfColsAdded = 1;
		boolean partNumChanged = false;
		if(splittedNumCols != 0) {
			for(int currentColId : maxEntropyPartition) {
				DenseMatrix64F currentColOnes = calculations.calcColOnes(currentColId, numOfRowPartitions, 
						graph, yNodes, searchK);
				CommonOps.sub(maxEntropyPartOnes, currentColOnes, leftoverOnes);
				DenseMatrix64F newEntropy = calculations.calcBlocksEntropy(maxEntropyPartBlocksSize, leftoverOnes);				
				double newAverageEntropy = CommonOps.elementSum(newEntropy) / splittedNumCols;
				if(newAverageEntropy < maxAverageEntropy - epsilon) {
					if(!partNumChanged) {
						this.numberOfPartitions = this.numberOfPartitions +1;
						SimpleMatrix colsPerPart = SimpleMatrix.wrap(xLinesPerPartition);
						SimpleMatrix splittedColsPerPartition = new SimpleMatrix(1, this.numberOfPartitions);
						splittedColsPerPartition = splittedColsPerPartition.combine(0, 0, colsPerPart);
						xLinesPerPartition.setReshape(splittedColsPerPartition.getMatrix());
						SimpleMatrix numOnes = SimpleMatrix.wrap(numberOfOnes);
						SimpleMatrix splittedNumOnes = new SimpleMatrix(1, this.numberOfPartitions);
						splittedNumOnes = splittedNumOnes.combine(0, 0, numOnes);
						numberOfOnes.setReshape(splittedNumOnes.getMatrix());
						partNumChanged = true;
					}
					splitLinesPerPartition(this.numberOfPartitions, numberOfColsAdded, 
							maxEntropyPartitionId, xLinesPerPartition);
					numberOfColsAdded++;
					splitNumberOfOnes(this.numberOfPartitions, maxEntropyPartitionId, 
							numberOfOnes, leftoverOnes, currentColOnes);
					//Update values realated to the maxEntropyPartition
					maxEntropyPartOnes = leftoverOnes.copy();
					CommonOps.subEquals(maxEntropyPartBlocksSize, yLinesPerPartition);
					splittedNumCols = splittedNumCols - 1;
					maxAverageEntropy = newAverageEntropy;
					xNodes.put(currentColId, this.numberOfPartitions);					
				}
			}
		}
	}
	
	/**
	 * Merges nodes of the map the correspond to the input id
	 * @param xNodes
	 * @param partitionId1
	 * @param partitionId2
	 */
	public void mergeNodesMap(Map<Integer, Integer> xNodes, int partitionId1, int partitionId2) {
		for(Map.Entry<Integer, Integer> entry : xNodes.entrySet()) {
			int partitionId = entry.getValue();
			int nodeId = entry.getKey();
			if(partitionId == partitionId2) {
				xNodes.put(nodeId, partitionId1);
			}
			else if(partitionId > partitionId2) {
				xNodes.put(nodeId, partitionId-1);
			}
		}
	}
	
	/**
	 * Rearanges the matrix after the merge of two columns
	 * @param colToMergeId1
	 * @param colToMergeId2
	 * @param initialMatrix
	 */
	public void mergeCols(int colToMergeId1, int colToMergeId2, DenseMatrix64F initialMatrix) {
		DenseMatrix64F matrix = new DenseMatrix64F(initialMatrix.getNumRows(), initialMatrix.getNumCols()-1);
		DenseMatrix64F[] columns = SpecializedOps.splitIntoVectors(initialMatrix, true);
		DenseMatrix64F col1 = columns[colToMergeId1];
		DenseMatrix64F col2 = columns[colToMergeId2];
		CommonOps.addEquals(col1, col2);
		CommonOps.insert(col1, matrix, 0, colToMergeId1);
		for(int i = 0; i < initialMatrix.getNumCols(); i++) {
			if((i == colToMergeId1) || (i == colToMergeId2)) {
				//do nothing
			}
			else if(i < colToMergeId2) {
				CommonOps.insert(columns[i], matrix, 0, i);
			}
			else {
				CommonOps.insert(columns[i], matrix, 0, i-1);
			}
		}
		initialMatrix.setReshape(matrix);
	}
	
	public void splitLinesPerPartition(int numOfColPartitions, int numberOfColsAdded, int maxEntropyPartitionId,
			DenseMatrix64F xLinesPerPartition) {
		xLinesPerPartition.set(0, numOfColPartitions-1, numberOfColsAdded);
		double tempValue = xLinesPerPartition.get(maxEntropyPartitionId-1);
		xLinesPerPartition.set(maxEntropyPartitionId-1, tempValue-1);
	}
	
	public void splitNumberOfOnes(int numOfColPartitions, int maxEntropyPartitionId, DenseMatrix64F numberOfOnes,
			DenseMatrix64F leftoverOnes, DenseMatrix64F currentColOnes) {
		CommonOps.insert(leftoverOnes, numberOfOnes, 0, maxEntropyPartitionId-1);
		DenseMatrix64F newPartitionCol = CommonOps.extract(numberOfOnes, 0, numberOfOnes.getNumRows(), 
				numOfColPartitions-1, numOfColPartitions);
		CommonOps.addEquals(newPartitionCol, currentColOnes);
		CommonOps.insert(newPartitionCol, numberOfOnes, 0, numOfColPartitions-1);
	}
	
	public int getNumberOfPartitions() {
		return this.numberOfPartitions;
	}
	
	public double getCost() {
		return this.cost;
	}
}