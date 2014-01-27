package sgc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import sgc.cluster.GraphCost;
import sgc.utils.Calculations;

/**
 * This class models a graph segment. A graph segment is
 * a set of graph snapshot that are similar with each other 
 * (similarity in terms of encoding cost). This class also has 
 * structures that model the node (source/destination) partitions.
 * 
 * @author smpeis
 * @email sot.beis@gmail.com
 *
 */
public class GraphSegment {
	
	int segmentId;
	int k;
	int l;
	int segmentSize;
	int numOfSrcNodes;
	int numOfDstNodes;
	double encodingCost;
	DenseMatrix64F numberOfOnes;
	DenseMatrix64F rowsPerPartition;
	DenseMatrix64F colsPerPartition;
	DenseMatrix64F blocksSize; 
	List<DenseMatrix64F> graphSnapshots = new ArrayList<DenseMatrix64F>();
	DenseMatrix64F graph;
	Map<Integer, Integer> srcNodes = new HashMap<Integer, Integer>();
	Map<Integer, Integer> dstNodes = new HashMap<Integer, Integer>();
	
	public GraphSegment(int k, int l, DenseMatrix64F graph, DenseMatrix64F rowsPerPartition, 
			DenseMatrix64F colsPerPartition, Map<Integer, Integer> srcNodes, 
			Map<Integer, Integer> dstNodes) {
		Calculations calculations = new Calculations();
		this.graph = new DenseMatrix64F(graph);
		this.k = k;
		this.l = l;
		this.segmentSize = 1;
		this.numOfSrcNodes = graph.getNumRows();
		this.numOfDstNodes = graph.getNumCols();
		this.blocksSize = calculations.calcBlocksSize(segmentSize, rowsPerPartition, colsPerPartition);
		this.rowsPerPartition = new DenseMatrix64F(rowsPerPartition);
		this.colsPerPartition = new DenseMatrix64F(colsPerPartition);
		this.srcNodes = new HashMap<Integer, Integer>(srcNodes);
		this.dstNodes = new HashMap<Integer, Integer>(dstNodes);
		this.numberOfOnes = calculations.calcNumberOfOnes(k, l, graph, srcNodes, dstNodes);
		initializeCost();
	}
	
	/**
	 * Fresh-start constructor
	 */
	public GraphSegment(DenseMatrix64F graph) {
		this.graph = new DenseMatrix64F(graph);
		this.k = 1;
		this.l = 1;
		this.segmentSize = 1;
		this.numOfSrcNodes = graph.getNumRows();
		this.numOfDstNodes = graph.getNumCols();
		this.blocksSize = new DenseMatrix64F(1,1);
		this.blocksSize.add(0, 0, graph.getNumRows() * graph.getNumCols());
		mapNodes(this.numOfSrcNodes, this.srcNodes);
		mapNodes(this.numOfDstNodes, this.dstNodes);
		this.rowsPerPartition= new DenseMatrix64F(1,1);
		this.rowsPerPartition.add(0, 0, graph.getNumRows());
		this.colsPerPartition = new DenseMatrix64F(1,1);
		this.colsPerPartition.add(0, 0, graph.getNumCols());
		this.numberOfOnes = new DenseMatrix64F(1,1);
		this.numberOfOnes.add(0, 0, CommonOps.elementSum(graph));
		initializeCost();
	}
	
	/**
	 * Resume constructor
	 */
	public GraphSegment(GraphSegment lastGraphSegment, DenseMatrix64F graph) {
		Calculations calculations = new Calculations();
		this.graph = graph;
		this.k = lastGraphSegment.getK();
		this.l = lastGraphSegment.getL();
		this.segmentSize = 1;
		this.numOfSrcNodes = lastGraphSegment.getNumberOfSrcNodes();
		this.numOfDstNodes = lastGraphSegment.getNumberOfDstNodes();
		this.rowsPerPartition = new DenseMatrix64F(lastGraphSegment.getRowsPerPartition());
		this.colsPerPartition = new DenseMatrix64F(lastGraphSegment.getColsPerPartition());
		this.blocksSize = lastGraphSegment.getBlocksSize().copy();
		this.srcNodes.putAll(lastGraphSegment.getSrcNodes());
		this.dstNodes.putAll(lastGraphSegment.getDstNodes());
		this.numberOfOnes = calculations.calcNumberOfOnes(this.k, this.l, graph, this.srcNodes, this.dstNodes);
		initializeCost();
	}
	
	
	/**
	 * This function is used only by the fresh start constructor and initializes
	 * nodes map
	 * @param index
	 * @param nodes
	 */
	private void mapNodes(int index, Map<Integer, Integer> nodes) {
		for(int i = 1; i < index+1; i++) {
			nodes.put(i, 1);
		}
	}
	
	private void initializeCost() {
		GraphCost graphCost = new GraphCost();
		this.encodingCost = graphCost.segmentEncodingCost(this.numOfSrcNodes, this.numOfDstNodes, this.k, 
				this.l, this.segmentSize, this.numberOfOnes, this.rowsPerPartition, this.colsPerPartition);
	}
		
	public void addGraph(DenseMatrix64F graph, DenseMatrix64F numberOfOnes) {
		GraphCost graphCost = new GraphCost();
		this.segmentSize++;
		this.graphSnapshots.add(graph);
		CommonOps.addEquals(this.graph, graph);
		CommonOps.addEquals(this.numberOfOnes, numberOfOnes);
		this.encodingCost = graphCost.segmentEncodingCost(this.numOfSrcNodes, this.numOfDstNodes, this.k, this.l, 
				this.segmentSize, this.numberOfOnes, this.rowsPerPartition, this.colsPerPartition);
	}
	
//	public void addGraph(DenseMatrix64F graph, DenseMatrix64F numberOfOnes, DenseMatrix64F rowsPerPartition, 
//			DenseMatrix64F colsPerPartition) {
//		GraphCost graphCost = new GraphCost();
//		this.segmentSize++;
//		this.graphSnapshots.add(graph);
//		CommonOps.addEquals(this.numberOfOnes, numberOfOnes);
//		//CommonOps.addEquals(this.rowsPerPartition, rowsPerPartition);
//		//CommonOps.addEquals(this.colsPerPartition, colsPerPartition);
//		this.encodingCost = graphCost.segmentEncodingCost(this.numOfSrcNodes, this.numOfDstNodes, this.k, this.l, 
//				this.segmentSize, this.numberOfOnes, this.rowsPerPartition, this.colsPerPartition);
//	}
	
	public void removeGraph(DenseMatrix64F graph, DenseMatrix64F numberOfOnes) {
		GraphCost graphCost = new GraphCost();
		this.segmentSize--;
		//this.graphSnapshots.remove(this.graphSnapshots.size()-1);
		CommonOps.subEquals(this.graph, graph);
		CommonOps.subEquals(this.numberOfOnes, numberOfOnes);
		this.encodingCost = graphCost.segmentEncodingCost(this.numOfSrcNodes, this.numOfDstNodes, this.k, this.l, 
				this.segmentSize, this.numberOfOnes, this.rowsPerPartition, this.colsPerPartition);
	}
	
//	public void removeGraph(DenseMatrix64F numberOfOnes, DenseMatrix64F rowsPerPartition, 
//			DenseMatrix64F colsPerPartition) {
//		GraphCost graphCost = new GraphCost();
//		this.segmentSize--;
//		this.graphSnapshots.remove(this.graphSnapshots.size()-1);
//		CommonOps.subEquals(this.numberOfOnes, numberOfOnes);
//		//CommonOps.subEquals(this.rowsPerPartition, rowsPerPartition);
//		//CommonOps.subEquals(this.colsPerPartition, colsPerPartition);
//		this.encodingCost = graphCost.segmentEncodingCost(this.numOfSrcNodes, this.numOfDstNodes, this.k, this.l, 
//				segmentSize, this.numberOfOnes, this.rowsPerPartition, this.colsPerPartition);
//	}
	
	public int getSegmentId() {
		return segmentId;
	}
	
	public void setSegmentId(int segmentId) {
		this.segmentId = segmentId;
	}
	
	public int getL() {
		return this.l;
	}
	
	public void setL(int l) {
		this.l = l;
	}
	
	public int getK() {
		return this.k;
	}
	
	public void setK(int k) {
		this.k = k;
	}
	
	public int getNumberOfSrcNodes() {
		return this.numOfSrcNodes;
	}
	
	public void setNumberOfSrcNodes(int numberOfSrcNodes) {
		this.numOfSrcNodes = numberOfSrcNodes;
	}
	
	public int getNumberOfDstNodes() {
		return this.numOfDstNodes;
	}
	
	public void setNumberOfDstNodes(int numberOfDstNodes) {
		this.numOfDstNodes = numberOfDstNodes;
	}
	
	public int getSegmentSize() {
		return segmentSize;
	}
	
	public DenseMatrix64F getGraph() {
		return this.graph;
	}
		
	public DenseMatrix64F getBlocksSize() {
		return this.blocksSize;
	}
	
	public DenseMatrix64F getNumberOfOnes() {
		return this.numberOfOnes;
	}
				
	public List<DenseMatrix64F> getGraphSnapshots() {
		return this.graphSnapshots;
	}
	
	public DenseMatrix64F getRowsPerPartition() {
		return this.rowsPerPartition;
	}
	
	public DenseMatrix64F getColsPerPartition() {
		return this.colsPerPartition;
	}
	
	public Map<Integer, Integer> getSrcNodes() {
		return this.srcNodes;
	}
	
	public Map<Integer, Integer> getDstNodes() {
		return this.dstNodes;
	}
		
	public double getEncodingCost() {
		return this.encodingCost;
	}
	
	public void setEncodingCost(double encodingCost) {
		this.encodingCost = encodingCost;
	}
	
	
}

