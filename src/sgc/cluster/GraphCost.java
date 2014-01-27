package sgc.cluster;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import sgc.utils.Calculations;

/**
 * This class implements all the cost computation function
 * for the GraphScope algorithm.
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class GraphCost {
	
	public double segmentEncodingCost(int numberOfSrcNodes, int numberOfDstNodes, int k, int l, int segmentSize,
			DenseMatrix64F numberOfOnes, DenseMatrix64F rowsPerPartition, 
			DenseMatrix64F colsPerPartition) {
		Calculations calculations = new Calculations();
		DenseMatrix64F numOfOnes = numberOfOnes.copy();
		double partitionEncodingCost = partitionEncodingCost(k, l, numberOfSrcNodes, numberOfDstNodes,
				rowsPerPartition, colsPerPartition);
		DenseMatrix64F blocksSize = calculations.calcBlocksSize(segmentSize, rowsPerPartition, colsPerPartition);		
		double graphEncodingCost = graphEncodingCost(numOfOnes, blocksSize);
		double edgesEncodingCost = edgesEncodingCost(blocksSize);
		double segmentEncodingCost = calculations.logstar(segmentSize) + edgesEncodingCost + 
				partitionEncodingCost + graphEncodingCost;
		return segmentEncodingCost;
	}
	
	public double partitionEncodingCost(int k, int l, int numberOfSrcNodes, int numberOfDstNodes, 
			DenseMatrix64F rowsPerPartition, DenseMatrix64F colsPerPartition) {
		Calculations calculations = new Calculations();
		double partitionCost = 0;
		partitionCost = calculations.logstar(k) + calculations.logstar(l);
		//Encoding cost for source partitions
		DenseMatrix64F sourcePartitionsEncoding = new DenseMatrix64F(k ,1);
		for(int i = 0; i < k; i++) {
			double rowsSum = 0;
			int yIndex = k - i;
			rowsSum = CommonOps.elementSum(CommonOps.extract(rowsPerPartition, 0, yIndex, 0, 1));
			rowsSum = rowsSum - k + i + 1;
			sourcePartitionsEncoding.add(i, 0, rowsSum);
		}
		partitionCost = partitionCost + CommonOps.elementSum(calculations.encodingBits(sourcePartitionsEncoding));

		//Encoding cost for destination partitions
		DenseMatrix64F dstPartitionsEncoding = new DenseMatrix64F(1, l);
		for(int i = 0; i < l; i++) {
			double colsSum = 0;
			int xIndex = l - i;
			colsSum = CommonOps.elementSum(CommonOps.extract(colsPerPartition, 0, 1, 0, xIndex));
			colsSum = colsSum - l + i + 1;
			dstPartitionsEncoding.add(0, i, colsSum);
		}
		partitionCost = partitionCost + CommonOps.elementSum(calculations.encodingBits(dstPartitionsEncoding));

		return partitionCost;
	}
		
	public double graphEncodingCost(DenseMatrix64F numberOfOnes, DenseMatrix64F blocksSize) {
		Calculations calculations = new Calculations();
		double graphEncodingCost = 0;
		DenseMatrix64F blocksEntropy = calculations.calcBlocksEntropy(blocksSize, numberOfOnes);
		graphEncodingCost =  Math.ceil(CommonOps.elementSum(blocksEntropy));
		return graphEncodingCost;
	}
	
	public double edgesEncodingCost(DenseMatrix64F blocksSize) {
		Calculations calculations = new Calculations();
		double edgesEncodingCost = 0;
		DenseMatrix64F bSizes = blocksSize.copy();
		CommonOps.add(bSizes, 1);
		bSizes = calculations.encodingBits(bSizes);
		edgesEncodingCost = CommonOps.elementSum(bSizes);
		return edgesEncodingCost;
	}
}
