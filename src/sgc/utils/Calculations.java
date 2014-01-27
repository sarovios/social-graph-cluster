package sgc.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.data.MatrixIterator;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SpecializedOps;

/**
 * This class contains all the calculations the GraphScope
 * algorithm need. There are some are necessary functions
 * for the evaluation and some other stuff
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class Calculations {
				
	/**
	 * Caclulates the entropy of each block in a matrix
	 */
	public DenseMatrix64F calcBlocksEntropy(DenseMatrix64F blocksSize, DenseMatrix64F numberOfOnes) {
		DenseMatrix64F blocksEntropy = new DenseMatrix64F(blocksSize.getNumRows(), blocksSize.getNumCols());
		DenseMatrix64F numOfOnes = numberOfOnes.copy();
		DenseMatrix64F numberOfZeros = calcNumberOfZeros(numOfOnes, blocksSize);
		DenseMatrix64F pOnes = calcPropability(numOfOnes, blocksSize);
		DenseMatrix64F pZeros = calcPropability(numberOfZeros, blocksSize);
		CommonOps.elementMult(numOfOnes, entropyBits(pOnes));
		CommonOps.elementMult(numberOfZeros, entropyBits(pZeros));
		CommonOps.add(numOfOnes, numberOfZeros, blocksEntropy);
		return blocksEntropy;
	}
		
	/**
	 * Calculates the cross entropy
	 * @param entropyOnes
	 * @param entropyZeros
	 * @param numOfColOnes
	 * @param yLinesPerPartition
	 * @return matrix with dimensions [1, numOfColPartitions]
	 */
	public DenseMatrix64F calcCrossEntropy(DenseMatrix64F entropyOnes, DenseMatrix64F entropyZeros, 
			DenseMatrix64F numberOfColOnes, DenseMatrix64F yLinesPerPartition) {
		DenseMatrix64F crossEntropy = new DenseMatrix64F(1, entropyOnes.getNumCols());
		DenseMatrix64F numOfColOnes = numberOfColOnes.copy();
		DenseMatrix64F numOfColZeros = calcNumberOfZeros(numOfColOnes, yLinesPerPartition);		
		CommonOps.transpose(numOfColOnes);
		CommonOps.transpose(numOfColZeros);
		CommonOps.mult(numOfColOnes, entropyOnes, crossEntropy);
		CommonOps.multAdd(numOfColZeros, entropyZeros, crossEntropy);
		return crossEntropy;
	}	
	
	public DenseMatrix64F calcPropability(DenseMatrix64F values, DenseMatrix64F blocksSize) {
		DenseMatrix64F pValues = new DenseMatrix64F(blocksSize.getNumRows(), blocksSize.getNumCols());
		CommonOps.elementDiv(values, blocksSize, pValues);
		return pValues;
	}
	
	public DenseMatrix64F calcNumberOfZeros(DenseMatrix64F numberOfOnes, DenseMatrix64F blocksSize) {
		DenseMatrix64F numberOfZeros = new DenseMatrix64F(numberOfOnes);
		CommonOps.changeSign(numberOfZeros);
		CommonOps.addEquals(numberOfZeros, blocksSize);
		return numberOfZeros;
	}
	
	public DenseMatrix64F calcZeroPropability(DenseMatrix64F pOnes) {
		DenseMatrix64F pZeros = new DenseMatrix64F(pOnes);
		CommonOps.changeSign(pZeros);
		CommonOps.add(pZeros, 1);
		return pZeros;
	}
	
	/**
	 * Calculates the size of each block in a segment
	 * @param segmentSize
	 * @param yLinesPerPartition
	 * @param xLinesPerPartition
	 * @return DenseMatrix64F with dimensions [numOfRowPartitions, numOfColPartitions]
	 */
	public DenseMatrix64F calcBlocksSize(int segmentSize, DenseMatrix64F yLinesPerPartition, 
			DenseMatrix64F xLinesPerPartition) {
		DenseMatrix64F blocksSize = new DenseMatrix64F(yLinesPerPartition.getNumRows(), 
				xLinesPerPartition.getNumCols());
		CommonOps.mult(segmentSize, yLinesPerPartition, xLinesPerPartition, blocksSize);
		return blocksSize;
	}
		
	/**
	 * Calculates the number of ones in each block
	 * @param k
	 * @param l
	 * @param graph
	 * @param srcNodes
	 * @param dstNodes
	 * @return matrix with dimensions [numOfRowPartitions, numOfColPartitions]
	 */
	public DenseMatrix64F calcNumberOfOnes(int k, int l, DenseMatrix64F graph, Map<Integer, Integer> srcNodes,
			Map<Integer, Integer> dstNodes) {
		DenseMatrix64F[] rows = SpecializedOps.splitIntoVectors(graph, false);
		DenseMatrix64F numOfOnes = new DenseMatrix64F(k ,l);
		for(Map.Entry<Integer, Integer> srcEntry : srcNodes.entrySet()) {
			int srcNodeId = srcEntry.getKey();
			int srcPartitionId = srcEntry.getValue();
			DenseMatrix64F currentRow = rows[srcNodeId-1];
			for(Map.Entry<Integer, Integer> dstEntry : dstNodes.entrySet()){
				int dstNodeId = dstEntry.getKey();
				int dstPartitionId = dstEntry.getValue();
				double value = currentRow.get(dstNodeId-1);
				if(value > 0) {
					numOfOnes.add(srcPartitionId-1, dstPartitionId-1, 1);
				}
			}
		}
		return numOfOnes;
	}
		
	/**
	 * Calculates the number of ones in from a column
	 * @param index
	 * @param numberOfPartitions
	 * @param graphSnapshot
	 * @param yNodes
	 * @param searchK
	 * @return matrix with dimensions [numOfRowPartitions,1]
	 */
	public DenseMatrix64F calcColOnes(int columnId, int numOfRowPartitions, DenseMatrix64F graphSnapshot, 
			Map<Integer, Integer> yNodes, boolean searchK) {
		DenseMatrix64F graph = new DenseMatrix64F(graphSnapshot);
		if(searchK) {
			CommonOps.transpose(graph);
		}
		DenseMatrix64F numOfColOnes = new DenseMatrix64F(numOfRowPartitions, 1);
		double value;
		for(Map.Entry<Integer, Integer> yNode : yNodes.entrySet()) {
			int yNodeId = yNode.getKey();
			int yNodePartitionId = yNode.getValue();
			if(yNodes.size() != graph.getNumRows()) {
				value = graph.get(columnId-1, yNodeId-1);
			}
			else {
				value = graph.get(yNodeId-1, columnId-1);
			}
			if(value > 0) {
				numOfColOnes.add(yNodePartitionId-1, 0, 1);
			}
		}
		return numOfColOnes;
	}
	
	/**
	 * Given the entropy value this function finds the id of
	 * the partitions tha corresponds to this value
	 */
	public int findPartitionId(DenseMatrix64F entropyMatrix, double entropy) {
		int partitionId = 0;
		boolean idFound = false;
		int index = entropyMatrix.getNumCols()-1;
		MatrixIterator iter = entropyMatrix.iterator(false, 0, 0, 0, index);
		while(!idFound) {
			if(iter.next() == entropy) {
				partitionId = iter.getIndex();
				idFound = true;
			}
		}
		return partitionId+1;
	}
	
	public boolean checkForZeros(DenseMatrix64F matrix) {
		boolean hasZero = false;
		MatrixIterator iter = matrix.iterator(true, 0, 0, matrix.getNumRows()-1, matrix.getNumCols()-1);
		while(iter.hasNext()) {
			if(iter.next() == 0) {
				hasZero = true;
			}
		}
		return hasZero;
	}
	
	public DenseMatrix64F updateMatrix(int minEntropyPartitionId, int currentPartitionId, 
			DenseMatrix64F currentColValues, DenseMatrix64F initialMatrix) {
		DenseMatrix64F matrix = new DenseMatrix64F(initialMatrix);
		int yIndex = matrix.getNumRows();
		DenseMatrix64F currentPartitionOnes = CommonOps.extract(matrix, 0, yIndex, currentPartitionId-1,
				currentPartitionId);
		CommonOps.subEquals(currentPartitionOnes, currentColValues);
		DenseMatrix64F minEntropyPartitionOnes = CommonOps.extract(matrix, 0, yIndex, minEntropyPartitionId-1,
				minEntropyPartitionId);
		CommonOps.addEquals(minEntropyPartitionOnes, currentColValues);
		CommonOps.insert(minEntropyPartitionOnes, matrix, 0, minEntropyPartitionId-1);
		CommonOps.insert(currentPartitionOnes, matrix, 0, currentPartitionId-1);
		return matrix;
	}
	
	/**
	 * Returns the the nodes assigned to a spicified partition
	 * @param partitionId
	 * @param nodes
	 * @return List<Integer>
	 */
	public List<Integer> getPartition(int partitionId, Map<Integer, Integer> nodes) {
		List<Integer> partition = new ArrayList<Integer>();
		for(Map.Entry<Integer, Integer> entry : nodes.entrySet()) {
			int nodeId = entry.getKey();
			int partId = entry.getValue();
			if(partId == partitionId) {
				partition.add(nodeId);
			}
		}
		return partition;
	}
	
	/**
	 * 
	 * @param nodesMap - the key corresponds to the node id and the value to the partition id
	 * 					 node belongs to
	 * @return map, the key is an integer that corresponds to the partition id and the value is a
	 * 	       list of integers that corresponds to the nodes that belong to the aforementioned
	 * 		   partition.
	 */
	public Map<Integer, List<Integer>> mapPartitions(Map<Integer, Integer> nodesMap) {
		Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
		for(Map.Entry<Integer, Integer> entry : nodesMap.entrySet()) {
			int nodeId = entry.getKey();
			int partitionId = entry.getValue();
			if(!partitions.containsKey(partitionId)) {
				List<Integer> nodesList = new ArrayList<Integer>();
				nodesList.add(nodeId);
				partitions.put(partitionId, nodesList);
			}
			else {
				partitions.get(partitionId).add(nodeId);
			}
		}
		return partitions;
	}
	
	/**
	 * @param matrix
	 * @return -log2 of matrix, defining -log2(0) \approx Inf
	 */
	public DenseMatrix64F entropyBits(DenseMatrix64F matrix) {
		DenseMatrix64F mat = new DenseMatrix64F(matrix);
		int stopCol = mat.getNumCols();
		int stopRow = mat.getNumRows();
		double tiny = Math.exp(-700);
		MatrixIterator iter = mat.iterator(false, 0, 0, stopRow-1, stopCol-1);
		while(iter.hasNext()) {
			double value = iter.next();
			iter.set(-log2(value + tiny));
		}
		return mat;
	}
		
	/**
	 * Computes the base 2 logarithm of all the elements from a matrix
	 * @param propability
	 */
	public DenseMatrix64F encodingBits(DenseMatrix64F matrix) {
		DenseMatrix64F mat = new DenseMatrix64F(matrix);
		int stopCol = mat.getNumCols();
		int stopRow = mat.getNumRows();
		MatrixIterator iter = mat.iterator(false, 0, 0, stopRow-1, stopCol-1);
		while(iter.hasNext()) {
			double value = iter.next();
			iter.set(encodingBits(value));
		}
		return mat;
	}
	
	/**
	 * @param x
	 * @return Return log2 of x, defining log2(0) = 0 and rounding up
	 */
	public double encodingBits(double x) {
		double encodingBits;
		if(x == 0) {
			encodingBits = 0;
		}
		else {
			encodingBits = Math.ceil(log2(x));
		}
		return encodingBits;
	}
	
	/**
	 * Computes the base 2 logarithm of a value, log(0) = Inf
	 * @param x
	 */
	public double log2(double x) {
		if(x < 1) {
			return -Math.abs((Math.log(x)/Math.log(2)));
		}
		else {
			return Math.abs((Math.log(x)/Math.log(2)));
		}
	}
	
	public void eraseZeroPartitions(Map<Integer, Integer> nodes) {
		Map<Integer, Integer> newNodes = new HashMap<Integer, Integer>();
		for(Map.Entry<Integer, Integer> entry : nodes.entrySet()) {
			int nodeId = entry.getKey();
			int partitionId = entry.getValue();
			if(partitionId != 0) {
				newNodes.put(nodeId, partitionId);
			}
		}
		nodes.clear();
		nodes.putAll(newNodes);
	}
	
	public int logstar(double x) {
		int numberOfBits = 0;
	    while(x > 1) {
	    	numberOfBits = numberOfBits + 1;
	    	x = log2(x);
	    }
	    return numberOfBits;
	}
	
	public DenseMatrix64F logstar(DenseMatrix64F matrix) {
		DenseMatrix64F mat = matrix.copy();
		int stopCol = mat.getNumCols();
		int stopRow = mat.getNumRows();
		MatrixIterator iter = mat.iterator(false, 0, 0, stopRow-1, stopCol-1);
		while(iter.hasNext()) {
			double value = iter.next();
			iter.set(logstar(value));
		}
		return mat;
	}
	
	public int countMembers(Map<Integer, Integer> map, int partitionId) {
		int membersNum = 0;
		for(Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if(entry.getValue() == partitionId) {
				membersNum = membersNum + 1;
			}
		}
		return membersNum;
	}
	
	public void initializePartitions(Map<Integer, Integer> partitions) {
		for(int i = 0; i < partitions.size(); i++) {
			partitions.put(i+1, 0);
		}
	}
	
	public int listSum(List<Integer> list) {
		int sum = 0;
		for(int i : list) {
			sum += i;
		}
		return sum;
	}
	
	public int listSumUptoID(List<Integer> list, int id) {
		int sum = 0;
		for(int i = 0; i < id; i++) {
			sum += list.get(i);
		}
		return sum;
	}
	
	public double calculateMean(double[] data) {
		double sum = 0.0;
		double size = data.length;
        for(double a : data) {
        	sum += a;
        }
        return sum/size;
	}
	
	public double calculateMean(List<Double> data) {
		double sum = 0.0;
		double size = data.size();
		for(double a : data) {
			sum += a;
		}
		return sum/size;
	}
	
	public double calculateVariance(double mean, double[] data) {
		double size = data.length;
		double temp = 0;
        for(double a :data) {
        	temp += (mean-a)*(mean-a);
        }
        return temp/size;
	}
	
	public double calculateVariance(double mean, List<Double> data) {
		double size = data.size();
		double temp = 0;
		for(double a : data) {
			temp += (mean-a)*(mean-a);
		}
		return temp/size;
	}
	
	public double calculateStdDeviation(double var) {
		return Math.sqrt(var);
	}

}
