package sgc.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;

public class BenchmarkGenerator {
	
//	static int edgeCounter = 1;
	private final int MAX_SOURCE_NODES = 500;
	private final int MAX_DESTINATION_NODES = 800;
	
	DenseMatrix64F adjacencyMatrix;
	Map<Integer, Integer> srcPartitions;
	Map<Integer, Integer> dstPartitions;
	List<Integer> srcPartitionsSizes;
	List<Integer> dstPartitionsSizes;
	
	/**
	 * Generates graphs with unequal clusters. The source and destination partition
	 * of the corresponding cluster is always equal. You must tweak in order to 
	 * find proper converge values. Suggested values below:
	 * @param srcNodesNum = 500
	 * @param dstNodesNum = 500
	 * @param numberOfPartitions = 5
	 * @param k_in
	 * @param k_out
	 */
	public void generateUnEqualClusters(int srcNodesNum, int dstNodesNum, int numberOfPartitions, 
			double k_in, double k_out) {
		Random random = new Random();
		this.adjacencyMatrix = new DenseMatrix64F(srcNodesNum, dstNodesNum);
		this.srcPartitions = new HashMap<Integer, Integer>();
		this.dstPartitions = new HashMap<Integer, Integer>();
		this.srcPartitionsSizes = new ArrayList<Integer>(numberOfPartitions);
		this.dstPartitionsSizes = new ArrayList<Integer>(numberOfPartitions);
		//initialize source partitions sizes
		int srcPartitionsCounter = numberOfPartitions - 1;
		int srcLeftNodes = srcNodesNum;
		double mFactor = (double)numberOfPartitions / 100;
		for(int i = 0; i < numberOfPartitions; i++) {
			double srcPartitionSize = Math.ceil(mFactor*srcNodesNum);
			if(srcPartitionsCounter == 0) {
				srcPartitionSize = srcLeftNodes;
			}
			this.srcPartitionsSizes.add((int)srcPartitionSize);
			srcLeftNodes -= srcPartitionSize;
			mFactor += mFactor;
			srcPartitionsCounter--;
		}
		System.out.println(this.srcPartitionsSizes);
		
		//initialize destination partitions sizes
		int dstPartitionsCounter = numberOfPartitions - 1;
		int dstLeftNodes = dstNodesNum;
		mFactor = (double)numberOfPartitions / 100;
		for(int i = 0; i < numberOfPartitions; i++) {
			double dstPartitionSize = Math.ceil(mFactor*dstNodesNum);
				if(dstPartitionsCounter == 0) {
						dstPartitionSize = dstLeftNodes;
				}
				this.dstPartitionsSizes.add((int)dstPartitionSize);
				dstLeftNodes -= dstPartitionSize;
				mFactor += mFactor;
				dstPartitionsCounter--;
		}
		System.out.println(this.dstPartitionsSizes);
		
		//add ones to partitions with propability 1-k_in
		int xIndex = 0;
		int yIndex = 0;
		for(int z = 0; z < numberOfPartitions; z++) {
			int xOffset = this.srcPartitionsSizes.get(z);
			int yOffset = this.dstPartitionsSizes.get(z);
			for(int i = xIndex; i < xIndex + xOffset; i++) {
				for(int j = yIndex; j < yIndex + yOffset; j++ ) {
					if(random.nextDouble() > k_in) {
						this.adjacencyMatrix.add(i, j, 1);
					}
					this.dstPartitions.put(j+1, z+1);
				}
				this.srcPartitions.put(i+1, z+1);
			}
			xIndex += xOffset;
			yIndex += yOffset;
		}
		
		int yOffset = this.srcPartitionsSizes.get(0);
		int xOffset = this.dstPartitionsSizes.get(0);
		int counter = 0;
		int partitionsCounter = 1;
		for(int j = xOffset; j < dstNodesNum; j++) {
			for(int i = 0; i < yOffset; i++) {
				if(random.nextDouble() < k_out) {
					this.adjacencyMatrix.add(i, j, 1);
				}
			}
			counter++;
			if(counter == this.dstPartitionsSizes.get(partitionsCounter)) {
				yOffset += this.srcPartitionsSizes.get(partitionsCounter);
				counter = 0;
				partitionsCounter++;
			}
		}
		
		//add noise with propability k_out (lower diagonal)
		yOffset = this.srcPartitionsSizes.get(0);
		xOffset = this.dstPartitionsSizes.get(0);
		counter = 0;
		partitionsCounter = 1;
		for(int i = yOffset; i < yIndex; i++) {
			for(int j = 0; j < xOffset; j++) {
				if(random.nextDouble() < k_out) {
					this.adjacencyMatrix.add(i, j, 1);
				}
			}
			counter++;
			if(counter == this.srcPartitionsSizes.get(partitionsCounter)) {
				xOffset += this.dstPartitionsSizes.get(partitionsCounter);
				counter = 0;
				partitionsCounter++;
			}
		}
		//MatrixVisualization.show(this.adjacencyMatrix, "");
	}
	
	double getRandomWeight(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	/**
	 * Generates fully assymetric graphs. You have to tweak in order to find
	 * proper converge values. Suggested values are below
	 * @param srcNodesNum = 500
	 * @param dstNodesNum = 800
	 * @param partitionsSizeMin = 80
	 * @param partitionsSizeMax = 700
	 * @param k_in
	 * @param k_out
	 */
	public void generateNotSymmRandSizes(int srcNodesNum, int dstNodesNum,int partitionsSizeMin, int partitionsSizeMax,
			double k_in, double k_out) {
		Random random = new Random();
		this.adjacencyMatrix = new DenseMatrix64F(MAX_SOURCE_NODES, MAX_DESTINATION_NODES);
		this.srcPartitions = new HashMap<Integer, Integer>();
		this.dstPartitions = new HashMap<Integer, Integer>();
		
		//initialize partitionsNum
		int partitionsNum  = 0;
		if(srcNodesNum < dstNodesNum) {
			partitionsNum = (int) Math.round(((srcNodesNum/partitionsSizeMin) + 
					(dstNodesNum/partitionsSizeMax)) * 0.5);
		}
		else {
			partitionsNum = (int) Math.round(((dstNodesNum/partitionsSizeMin) + 
					(srcNodesNum/partitionsSizeMax)) * 0.5);
		}
		System.out.println(partitionsNum);
		
		//initialize source and destination partition sizes
		List<Integer> srcPartitionsSizes = initializePartitionsSizes(partitionsNum, srcNodesNum,
				partitionsSizeMin, partitionsSizeMax);
		System.out.println(srcPartitionsSizes);
		
		List<Integer> dstPartitionsSizes = initializePartitionsSizes(partitionsNum, dstNodesNum,
				partitionsSizeMin, partitionsSizeMax);
		System.out.println(dstPartitionsSizes);
		

		//add ones to partitions with propability 1-k_in
		int xIndex = 0;
		int yIndex = 0;
		for(int z = 0; z < partitionsNum; z++) {
			int xOffset = srcPartitionsSizes.get(z);
			int yOffset = dstPartitionsSizes.get(z);
			for(int i = xIndex; i < xIndex + xOffset; i++) {
				for(int j = yIndex; j < yIndex + yOffset; j++ ) {
					if(random.nextDouble() > k_in) {
						this.adjacencyMatrix.add(i, j, 1);
					}
					this.dstPartitions.put(j+1, z+1);
				}
				this.srcPartitions.put(i+1, z+1);
			}
			xIndex += xOffset;
			yIndex += yOffset;
		}
		
		//add noise with propability k_out (upper diagonal)
		int xOffset = srcPartitionsSizes.get(0);
		int yOffset = dstPartitionsSizes.get(0);
		int counter = 0;
		int partitionsCounter = 1;
		for(int j = yOffset; j < dstNodesNum; j++) {
			for(int i = 0; i < xOffset; i++) {
				if(random.nextDouble() < k_out) {
					this.adjacencyMatrix.add(i, j, 1);
				}
			}
			counter++;
			if(counter == dstPartitionsSizes.get(partitionsCounter)) {
				xOffset += srcPartitionsSizes.get(partitionsCounter);
				counter = 0;
				partitionsCounter++;
			}
		}
		if(srcNodesNum != MAX_SOURCE_NODES) {
			for(int i = srcNodesNum; i < 800; i++) {
				this.srcPartitions.put(i+1, 0);
			}
		}
		if(dstNodesNum != MAX_DESTINATION_NODES) {
			for(int i = dstNodesNum; i < 1000; i++) {
				this.dstPartitions.put(i+1, 0);
			}
		}
		//add noise with propability k_out (lower diagonal)
		xOffset = srcPartitionsSizes.get(0);
		yOffset = dstPartitionsSizes.get(0);
		counter = 0;
		partitionsCounter = 1;
		for(int i = xOffset; i < srcNodesNum; i++) {
			for(int j = 0; j < yOffset; j++) {
				if(random.nextDouble() < k_out) {
					this.adjacencyMatrix.add(i, j, 1);
				}
			}
			counter++;
			if(counter == srcPartitionsSizes.get(partitionsCounter)) {
				yOffset += dstPartitionsSizes.get(partitionsCounter);
				counter = 0;
				partitionsCounter++;
			}
		}
	}
	
	public List<Integer> initializePartitionsSizes(int numOfPartitions, int numOfNodes, int partitionsSizeMin, 
			int partitionsSizeMax) {
		Random random = new Random();
		List<Integer> partitionsSizes = null;
		boolean done = false;
		while(!done) {
			partitionsSizes = new ArrayList<Integer>(numOfPartitions);
			int nodesLeft = numOfNodes;
			for(int i = 0; i < numOfPartitions; i++) {
				int partitionSize = random.nextInt(partitionsSizeMax - partitionsSizeMin + 1) + partitionsSizeMin;
				if(i == numOfPartitions - 1) {
					partitionSize = nodesLeft;
				}
				partitionsSizes.add(partitionSize);
				nodesLeft -= partitionSize;
			}
			int lastPartition = partitionsSizes.get(numOfPartitions - 1);
			if((lastPartition <= partitionsSizeMax) && (lastPartition >= partitionsSizeMin)) {
				done = true;
			}
		}
		return partitionsSizes;
	}
	
	/**
	 * Generates fully symmetric graphs. The sizes of the partitions
	 * are given and fixed. The number of partitionSize param 
	 * should be multipe integer of the numOfNodes param
	 * @param numOfNodes
	 * @param partitionSize
	 * @param k_in
	 * @param k_out
	 */
	public void generateFullySymmetric(int numOfNodes, int partitionSize, double k_in, double k_out) {
		Random random = new Random();
		this.adjacencyMatrix = new DenseMatrix64F(numOfNodes, numOfNodes);
		this.srcPartitions = new HashMap<Integer, Integer>();
		this.dstPartitions = new HashMap<Integer, Integer>();
		int xIndex = 0;
		int yIndex = 0;
		int srcPartitionId = 1;
		int dstPartitionId = 1;		
		while((xIndex < numOfNodes) && (yIndex < numOfNodes)) {
			for(int i = xIndex; i <  xIndex+partitionSize; i++) {
				for(int j = yIndex; j < yIndex+partitionSize; j++) {
					if(random.nextDouble() > k_in) {
						this.adjacencyMatrix.add(i, j, 1);
					}
					this.dstPartitions.put(j+1, dstPartitionId);
				}
				this.srcPartitions.put(i+1, srcPartitionId);
			}
			srcPartitionId++;
			dstPartitionId++;
			xIndex += partitionSize;
			yIndex += partitionSize;
		}
		int offset = partitionSize;
		int counter = 0;
		for(int j = partitionSize ; j < numOfNodes; j++) {
			for(int i = 0; i < offset; i++) {
				if(random.nextDouble() < k_out) {
					this.adjacencyMatrix.add(i, j, 1);
					this.adjacencyMatrix.add(j, i, 1);
				}
			}
			counter++;
			if(counter == partitionSize) {
				offset += partitionSize;
				counter = 0;
			}
		}
	}
	
	public DenseMatrix64F getAdjacencyMatrix() {
		return this.adjacencyMatrix;
	}
	
	public Map<Integer, Integer> getSrcPartitions() {
		return this.srcPartitions;
	}
	
	public Map<Integer, Integer> getDstPartitions() {
		return this.dstPartitions;
	}
	
	public List<Integer> getSrcPartitionsSizes() {
		return this.srcPartitionsSizes;
	}
	
	public List<Integer> getDstPartitionsSizes() {
		return this.dstPartitionsSizes;
	}
}