package sgc.cluster;

import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import sgc.utils.Calculations;

/**
 * This class implements the ReGroup function of GraphScope algorithm.
 * ReGroup's job is to assign each node (source/destination) to the
 * best partition (source/destination) in terms of minimum cost.
 * 
 * @author sbeis
 *
 */
public class ReGroup {
	
	double cost;
	double costTreshold = 0.001;
	
	public void coCluster(int numOfRowPartitions, int numOfColPartitions, int numberOfSrcNodes, int numberOfDstNodes, 
			int segmentSize, DenseMatrix64F yLinesPerPartition, DenseMatrix64F xLinesPerPartition, 
			DenseMatrix64F numberOfOnes, DenseMatrix64F graph, Map<Integer, Integer> yNodes, 
			Map<Integer, Integer> xNodes) {
		GraphCost graphCost = new GraphCost();
		Calculations calculations = new Calculations();
		this.cost = graphCost.segmentEncodingCost(numberOfSrcNodes, numberOfDstNodes, numOfRowPartitions, 
				numOfColPartitions, segmentSize, numberOfOnes, yLinesPerPartition, xLinesPerPartition);
		double newCost;
		DenseMatrix64F newYLinesPerPartition = new DenseMatrix64F(yLinesPerPartition);
		DenseMatrix64F newXLinesPerPartition = new DenseMatrix64F(xLinesPerPartition);
		DenseMatrix64F newNumberOfOnes = new DenseMatrix64F(numberOfOnes);		
		Map<Integer, Integer> newYNodes = new HashMap<Integer, Integer>(yNodes);
		Map<Integer, Integer> newXNodes = new HashMap<Integer, Integer>(xNodes);
		boolean searchK;
		boolean noChanges = false;
		boolean zeroValues = false;
		while(!noChanges) {
			//Update destination partitions
			searchK = false;
			if(!zeroValues) {
				iterateOverNodes(numOfRowPartitions, segmentSize, newYLinesPerPartition, newXLinesPerPartition, 
						newNumberOfOnes, graph, newYNodes, newXNodes, searchK);
				zeroValues = calculations.checkForZeros(newYLinesPerPartition);
				zeroValues = calculations.checkForZeros(newXLinesPerPartition);
			}
			searchK = true;
			//Update source partitions
			if(!zeroValues) {
				CommonOps.transpose(newNumberOfOnes);
				CommonOps.transpose(newYLinesPerPartition);
				CommonOps.transpose(newXLinesPerPartition);
				iterateOverNodes(numOfColPartitions, segmentSize, newXLinesPerPartition, newYLinesPerPartition, 
						newNumberOfOnes, graph, newXNodes, newYNodes, searchK);
				CommonOps.transpose(newNumberOfOnes);
				CommonOps.transpose(newYLinesPerPartition);
				CommonOps.transpose(newXLinesPerPartition);
				zeroValues = calculations.checkForZeros(newXLinesPerPartition);
				zeroValues = calculations.checkForZeros(newYLinesPerPartition);
			}
			if(!zeroValues) {
				newCost = graphCost.segmentEncodingCost(numberOfSrcNodes, numberOfDstNodes, numOfRowPartitions, 
						numOfColPartitions, segmentSize, newNumberOfOnes, newYLinesPerPartition, 
						newXLinesPerPartition);
				if(newCost < this.cost) {
					numberOfOnes.set(newNumberOfOnes);
					yLinesPerPartition.set(newYLinesPerPartition);
					xLinesPerPartition.set(newXLinesPerPartition);
					yNodes.putAll(newYNodes);
					xNodes.putAll(newXNodes);
					this.cost = newCost;
				}
				else {
					noChanges = true;
				}
			}
			else {
				noChanges = true;
			}
		}
	}
	
	public void iterateOverNodes(int numOfRowPartitions, int segmentSize, DenseMatrix64F yLinesPerPartition, 
			DenseMatrix64F xLinesPerPartition, DenseMatrix64F numberOfOnes, DenseMatrix64F graph, 
			Map<Integer, Integer> yNodes, Map<Integer, Integer> xNodes, boolean searchK) {
		Calculations calculations = new Calculations();
		//int numOfColPartitions = xLinesPerPartition.getNumCols();
		DenseMatrix64F blocksSize = calculations.calcBlocksSize(segmentSize, yLinesPerPartition, xLinesPerPartition);		
		DenseMatrix64F pOnes = calculations.calcPropability(numberOfOnes, blocksSize);
		DenseMatrix64F pZeros = calculations.calcZeroPropability(pOnes);
		DenseMatrix64F entropyOnes = calculations.entropyBits(pOnes);
		DenseMatrix64F entropyZeros = calculations.entropyBits(pZeros);
		int xNodePartitionId = 0;
		int minEntropyPartitionId = 0;
		for(Map.Entry<Integer, Integer> xNode : xNodes.entrySet()) {
			int xNodeId = xNode.getKey();
			xNodePartitionId = xNode.getValue();
			DenseMatrix64F currentColOnes = calculations.calcColOnes(xNodeId, numOfRowPartitions, 
					graph, yNodes, searchK);
			DenseMatrix64F crossEntropy = calculations.calcCrossEntropy(entropyOnes, entropyZeros, currentColOnes,
					yLinesPerPartition);
			double minEntropy = CommonOps.elementMin(crossEntropy);
			minEntropyPartitionId = calculations.findPartitionId(crossEntropy, minEntropy);
			if(minEntropyPartitionId != xNodePartitionId) {
				xNodes.put(xNodeId, minEntropyPartitionId);
				numberOfOnes.set(calculations.updateMatrix(minEntropyPartitionId, xNodePartitionId, currentColOnes, 
						numberOfOnes));
				xLinesPerPartition.set(calculations.updateMatrix(minEntropyPartitionId, xNodePartitionId, 
						new DenseMatrix64F(new double[][] {{1}}), xLinesPerPartition));				
			}
		}
	}
	
	public double getCost() {
		return this.cost;
	}
}
