package sgc.cluster;

import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import sgc.model.GraphSegment;

/**
 * This class implements the main function of the GraphScope algorithm
 * (http://dl.acm.org/citation.cfm?id=1281266). The main job of this function
 * is to check if a new snapshot is similar with a previous. If yes then both 
 * are stored to the same segment. Otherwise a new segment is created.
 * 
 * @author sbeis
 *
 */
public class GraphScope {
	
	static final double costThreshold = -0.001;
	static final double similarityThreshold = 0.1;
	static final String resumeInit = "resume";
	static final String freshStartInit = "freshStart";
	
	int k;
	int l;
	double similarity = 0;
	double afterSearchCost;
	long estimatedTime;
	boolean newSegment;
	GraphSegment newGraphSegment = null;
	
	public void run(GraphSegment initialGraphSegment, DenseMatrix64F newGraph, String initialization) {
		long startTime = System.nanoTime();
		int numberOfSrcNodes = initialGraphSegment.getNumberOfSrcNodes();
		int numberOfDstNodes = initialGraphSegment.getNumberOfDstNodes();
		this.k = initialGraphSegment.getK();
		this.l = initialGraphSegment.getL();
		int segmentSize;
		DenseMatrix64F numberOfOnes;
		DenseMatrix64F rowsPerPartition = initialGraphSegment.getRowsPerPartition();
		DenseMatrix64F colsPerPartition = initialGraphSegment.getColsPerPartition();
		Map<Integer, Integer> srcNodes = initialGraphSegment.getSrcNodes();
		Map<Integer, Integer> dstNodes = initialGraphSegment.getDstNodes();
		double initialCost = initialGraphSegment.getEncodingCost();
		this.newSegment = false;
		if(newGraph == null) {
			segmentSize = initialGraphSegment.getSegmentSize();
			numberOfOnes = initialGraphSegment.getNumberOfOnes();
			searchForPartitions(this.k, this.l, numberOfSrcNodes, numberOfDstNodes, segmentSize, 
					initialCost, numberOfOnes, rowsPerPartition, colsPerPartition, 
					initialGraphSegment.getGraph(), srcNodes, dstNodes);
			this.k = getK();
			this.l = getL();
			initialGraphSegment.setK(this.k);
			initialGraphSegment.setL(this.l);
			initialGraphSegment.setEncodingCost(this.afterSearchCost);
			System.out.println();
		}
		else {			
			//initialize new graph as a segment with equal values as the previous segment
			this.newGraphSegment = new GraphSegment(initialGraphSegment, newGraph);
			double newCost = this.newGraphSegment.getEncodingCost();

			//merge segments in order to see if we have cost reduction
			initialGraphSegment.addGraph(newGraph, this.newGraphSegment.getNumberOfOnes());
			double mergedCost = initialGraphSegment.getEncodingCost();
			this.similarity = mergedCost - initialCost - newCost;
			if(mergedCost - initialCost < newCost) {
				System.out.println("Merge new graph to the old segment");
				segmentSize = initialGraphSegment.getSegmentSize();
				numberOfOnes = initialGraphSegment.getNumberOfOnes();
				searchForPartitions(this.k, this.l, numberOfSrcNodes, numberOfDstNodes, segmentSize, mergedCost, 
						numberOfOnes, rowsPerPartition, colsPerPartition, initialGraphSegment.getGraph(), 
						srcNodes, dstNodes);
				this.k = getK();
				this.l = getL();
				initialGraphSegment.setK(this.k);
				initialGraphSegment.setL(this.l);
				initialGraphSegment.setEncodingCost(this.afterSearchCost);
				System.out.println();
			}
			else {
				System.out.println("Create new segment");
				initialGraphSegment.removeGraph(newGraph, this.newGraphSegment.getNumberOfOnes());
				if(initialization == resumeInit) {
					//do nothing
				}
				else if(initialization == freshStartInit) {
					this.newGraphSegment = new GraphSegment(newGraph);
				}
				this.k = this.newGraphSegment.getK();
				this.l = this.newGraphSegment.getL();
				segmentSize = this.newGraphSegment.getSegmentSize();
				rowsPerPartition = this.newGraphSegment.getRowsPerPartition();
				colsPerPartition = this.newGraphSegment.getColsPerPartition();
				numberOfOnes = this.newGraphSegment.getNumberOfOnes();
				srcNodes = this.newGraphSegment.getSrcNodes();
				dstNodes = this.newGraphSegment.getDstNodes();
				newCost = this.newGraphSegment.getEncodingCost();
				searchForPartitions(this.k, this.l, numberOfSrcNodes, numberOfDstNodes, segmentSize, newCost, 
						numberOfOnes, rowsPerPartition, colsPerPartition, this.newGraphSegment.getGraph(), 
						srcNodes, dstNodes);
				this.newSegment = true;
				this.k = getK();
				this.l = getL();
				this.newGraphSegment.setK(this.k);
				this.newGraphSegment.setL(this.l);
				this.newGraphSegment.setEncodingCost(this.afterSearchCost);
				System.out.println();
			}
		}
		this.estimatedTime = System.nanoTime() - startTime;
		System.out.println();
	}
	
	
	public void searchForPartitions(int k, int l, int numberOfSrcNodes, int numberOfDstNodes, int segmentSize, 
			double cost, DenseMatrix64F numberOfOnes, DenseMatrix64F rowsPerPartition, 
			DenseMatrix64F colsPerPartition,  DenseMatrix64F graph, 
			Map<Integer, Integer> srcNodes, Map<Integer, Integer> dstNodes) {
		SearchKL searchKL = new SearchKL();
		ReGroup reGroup = new ReGroup();
		this.l = l;
		this.k = k;
		this.afterSearchCost = cost;
		int newL = l;
		int newK = k;
		double newCost = cost;
		double costBeforeSplit = 0;
		boolean searchK;
		DenseMatrix64F newNumberOfOnes = new DenseMatrix64F(numberOfOnes);
		DenseMatrix64F newColsPerPartition = new DenseMatrix64F(colsPerPartition);
		DenseMatrix64F newRowsPerPartition = new DenseMatrix64F(rowsPerPartition);
		Map<Integer, Integer> newSrcNodes = new HashMap<Integer, Integer>(srcNodes);
		Map<Integer, Integer> newDstNodes = new HashMap<Integer, Integer>(dstNodes);
		boolean noChange = false;
		while(!noChange) {
			newL = this.l;
			newK = this.k;
			cost = this.afterSearchCost;
			newCost = this.afterSearchCost;
			newNumberOfOnes.setReshape(numberOfOnes);
			newColsPerPartition.setReshape(colsPerPartition);
			newRowsPerPartition.setReshape(rowsPerPartition);
			newSrcNodes.putAll(srcNodes);
			newDstNodes.putAll(dstNodes);
			//MERGE
			boolean noMerge = false;
			while(!noMerge) {
				//try to merge destination partitions
				searchK = false;
				if(this.l > 1) {
					System.out.println("Try to merge destination partitions");
					System.out.println("k= "+newK+" l= "+newL);
					searchKL.merge(newK, newL, numberOfSrcNodes, numberOfDstNodes, segmentSize, newCost, 
							newNumberOfOnes, newRowsPerPartition, newColsPerPartition, 
							newDstNodes, searchK);
					newL = searchKL.getNumberOfPartitions();
					newCost = searchKL.getCost();
				}
				searchK = true;
				//try to merge source partitions
				if(this.k > 1) {
					System.out.println("Try to merge source partitions");
					System.out.println("k= "+newK+" l= "+newL);
					CommonOps.transpose(newNumberOfOnes);
					CommonOps.transpose(newRowsPerPartition);
					CommonOps.transpose(newColsPerPartition);
					searchKL.merge(newL, newK, numberOfDstNodes, numberOfSrcNodes, segmentSize, newCost, 
							newNumberOfOnes, newColsPerPartition, newRowsPerPartition, 
							newSrcNodes, searchK);
					newK = searchKL.getNumberOfPartitions();
					newCost = searchKL.getCost();
					CommonOps.transpose(newNumberOfOnes);
					CommonOps.transpose(newRowsPerPartition);
					CommonOps.transpose(newColsPerPartition);			
				}
				if(newCost < cost) {
					cost = newCost;
				}
				else {
					noMerge = true;
					this.k = newK;
					this.l = newL;
					numberOfOnes.setReshape(newNumberOfOnes);
					rowsPerPartition.setReshape(newRowsPerPartition);
					colsPerPartition.setReshape(newColsPerPartition);
					srcNodes.putAll(newSrcNodes);
					dstNodes.putAll(newDstNodes);
				}
			}
			
			//SPLIT
			costBeforeSplit = cost;
			boolean noSplit = false;
			while(!noSplit) {
				//try to split destination partitions
				newNumberOfOnes = new DenseMatrix64F(numberOfOnes);
				newColsPerPartition = new DenseMatrix64F(colsPerPartition);
				newRowsPerPartition = new DenseMatrix64F(rowsPerPartition);
				newSrcNodes = new HashMap<Integer, Integer>(srcNodes);
				newDstNodes = new HashMap<Integer, Integer>(dstNodes);
				searchK = false;
				System.out.println("Try to split destination partitions");
				System.out.println("k= "+this.k+" l= "+this.l);
				searchKL.split(this.k, this.l, numberOfSrcNodes, numberOfDstNodes, segmentSize, newNumberOfOnes, 
						newRowsPerPartition, newColsPerPartition, graph, newSrcNodes, 
						newDstNodes, searchK);
				newL = searchKL.getNumberOfPartitions();
				if(newL > this.l) {
					System.out.println("l increased");
					reGroup.coCluster(this.k, newL, numberOfSrcNodes, numberOfDstNodes, segmentSize, 
							newRowsPerPartition, newColsPerPartition, newNumberOfOnes, graph, 
							newSrcNodes, newDstNodes);
					newCost = reGroup.getCost();
					if(newCost < cost) {
						System.out.println("Cost decreased");
						this.l = newL;
						numberOfOnes.setReshape(newNumberOfOnes);
						rowsPerPartition.setReshape(newRowsPerPartition);
						colsPerPartition.setReshape(newColsPerPartition);
						srcNodes.putAll(newSrcNodes);
						dstNodes.putAll(newDstNodes);
						cost = newCost;
					}
				}
				//try to split source partitions
				newNumberOfOnes = new DenseMatrix64F(numberOfOnes);
				newColsPerPartition = new DenseMatrix64F(colsPerPartition);
				newRowsPerPartition = new DenseMatrix64F(rowsPerPartition);
				newSrcNodes = new HashMap<Integer, Integer>(srcNodes);
				newDstNodes = new HashMap<Integer, Integer>(dstNodes);
				CommonOps.transpose(newNumberOfOnes);
				CommonOps.transpose(newRowsPerPartition);
				CommonOps.transpose(newColsPerPartition);
				searchK = true;
				System.out.println("Try to split source partitions");
				System.out.println("k= "+this.k+" l= "+this.l);
				searchKL.split(this.l, this.k, numberOfDstNodes, numberOfSrcNodes, segmentSize, newNumberOfOnes, 
						newColsPerPartition, newRowsPerPartition, graph, newDstNodes, newSrcNodes, searchK);
				newK = searchKL.getNumberOfPartitions();
				CommonOps.transpose(newNumberOfOnes);
				CommonOps.transpose(newRowsPerPartition);
				CommonOps.transpose(newColsPerPartition);
				if(newK > this.k) {
					System.out.println("k increased");
					reGroup.coCluster(newK, this.l, numberOfSrcNodes, numberOfDstNodes, segmentSize, 
							newRowsPerPartition, newColsPerPartition, newNumberOfOnes, graph, 
							newSrcNodes, newDstNodes);
					newCost = reGroup.getCost();
					if(newCost < cost) {
						System.out.println("Cost decreased");
						this.k = newK;
						numberOfOnes.setReshape(newNumberOfOnes);
						rowsPerPartition.setReshape(newRowsPerPartition);
						colsPerPartition.setReshape(newColsPerPartition);
						srcNodes.putAll(newSrcNodes);
						dstNodes.putAll(newDstNodes);
						cost = newCost;
					}
				}
				if(cost < costBeforeSplit) {
					costBeforeSplit = cost;
				}
				else {
					noSplit = true;
				}
			}
			if(costBeforeSplit < this.afterSearchCost) {
				this.afterSearchCost = costBeforeSplit;
			}
			else {
				noChange = true;
			}
		}
		
	}
	
	public int getK() {
		return this.k;
	}
	
	public int getL() {
		return this.l;
	}
	
	public double getCost() {
		return this.afterSearchCost;
	}
	
	public double getSimilarity() {
		return this.similarity;
	}
	public double getEstimatedTime() {
		return (double)this.estimatedTime;
	}
	
	public boolean newSegment() {
		return this.newSegment;
	}
	
	public GraphSegment getNewGraphSegment() {
		return this.newGraphSegment;
	}
}