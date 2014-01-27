package sgc.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

/**
 * This class contains everything we need in order to work
 * with evolving synthetic graphs.
 * 
 * @author sbeis
 * @email sot.beis@gmail.com
 *
 */
public class SyntheticDataset {

	/**
	 * Reads the ground truth clusterings and stores them to a
	 * map where key corresponds to the node id and value
	 * corresponds to cluster id
	 */
	public Map<Integer, Integer> nodePartitions(String root) {
		Map<Integer, Integer> nodePartitions = new HashMap<Integer, Integer>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(root)));
			String line;
			int partitionCounter = 1;
			while((line = reader.readLine()) != null){
				String[] parts = line.split(" ");
				String[] nodes = parts[1].split(",");
				for(int i = 0; i < nodes.length; i++){
					String[] nodeParts = nodes[i].split("_");
					String nodeId = nodeParts[1];
					nodePartitions.put(Integer.parseInt(nodeId), partitionCounter);
				}
				partitionCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println(nodePartitions);
		return nodePartitions;
	}
	
	/**
	 * Reads a matrix in MatrixMarket format and stores to
	 * DenseMatrix64F structure.
	 * @param root - the root of the dataset
	 * @param xLines - the number of vetrical lines
	 * @param yLines - the number of horizontal lines
	 * @return - the matrix in a DenseMatrix64F format
	 */
	public DenseMatrix64F txtToMatrix(String root, int xLines, int yLines) {
		DenseMatrix64F adjacencyMatrix =  new DenseMatrix64F(xLines, yLines);
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(root)));
			String line;
			int lineCounter = 1;
			int erasedEdges = 0;
			double maxWeight = 0;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(" ");
				if(lineCounter > 2) {
					int srcNode = Integer.parseInt(parts[0]);
					int dstNode = Integer.parseInt(parts[1]);
					adjacencyMatrix.set(srcNode-1, dstNode-1, 1);
				}
				lineCounter++;
			}
			System.out.println("Total edges: "+lineCounter);
			System.out.println("Erased edges: "+erasedEdges);
			System.out.println("Max weight: "+maxWeight);
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return adjacencyMatrix;
	}

}
