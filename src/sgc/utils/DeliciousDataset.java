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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixIO;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class contains everything we need to in orders to work
 * with the data that come from the delicious dataset which is 
 * descibed here: http://www.dai-labor.de/en/publication/359.
 * Morever there are functions for the bipartite (bookmark-tag) graph
 * creation.
 * 
 * @author sbeis
 *
 */
public class DeliciousDataset {
	
	/**
	 * Create  the inverted index. Necessary for the creation
	 * of the cocurrent tag graph.
	 */
	public Map<Integer, List<Integer>> createInvertedIndex(String snapshotPath, Map<String, Integer> tags, 
			Map<String, Integer> bookmarks) {
		Map<Integer, List<Integer>> invertedIndex = new HashMap<Integer, List<Integer>>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(snapshotPath)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				if(parts.length == 4) {
					String bookmark = parts[2];
					String tag = parts[3];
					if((tags.containsKey(tag)) && bookmarks.containsKey(bookmark)) {
						int tagId = tags.get(tag);
						int bookmarkId = bookmarks.get(bookmark);
						if(invertedIndex.containsKey(bookmarkId)) {
							List<Integer> indexedTags = invertedIndex.get(bookmarkId);
							if(!indexedTags.contains(tagId)) {
								indexedTags.add(tagId);
							}
						}
						else {
							List<Integer> indexedTags = new ArrayList<Integer>();
							invertedIndex.put(bookmarkId, indexedTags);
						}
					}
				}
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return invertedIndex;
	}
	
	/**
	 * Create co-current tag graph.
	 */
	public DenseMatrix64F createCoCurrentTagGraph(String snapshotPath, String topTagsPath, 
			String topBookmarkssPath) {
		Map<String, Integer> tags = getTopTags(topTagsPath);
		Map<String, Integer> bookmarks = getTopBookmarks(topBookmarkssPath);
		Map<Integer, List<Integer>> invertedIndex = createInvertedIndex(snapshotPath, tags, 
			bookmarks);
		DenseMatrix64F tagGraph = new DenseMatrix64F(1500, 1500);
		for(List<Integer> connectedTags : invertedIndex.values()) {
			for(int i = 0; i < connectedTags.size(); i++) {
				if(i > 0) {
					int tagId1 = connectedTags.get(i-1);
					int tagId2 = connectedTags.get(i);
					double value = tagGraph.get(tagId1-1, tagId2-1);
					if(value == 0) {
						tagGraph.add(tagId1-1, tagId2-1, 1);
					}
				}
			}
		}
		return tagGraph;
	}

	/**
	 * Count how many of the tags and bookmarks appear in every snapshot. Also count the 
	 * edges of each snapshot
	 */
	public void countTopResourcesPerSnapshot() {
		Map<String, Integer> tags = new HashMap<String, Integer>();
		Map<String, Integer> bookmarks = new HashMap<String, Integer>();
		int idCounter = 1;
		try {
			CSVReader reader = new CSVReader(new FileReader(
					"data/delicious.2007.autum/dataset/top1500.tags"));
			String[] nextLine;
			while((nextLine = reader.readNext()) != null) {
				tags.put(nextLine[0], idCounter);
				idCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		try {
			CSVReader reader = new CSVReader(new FileReader(
					"data/delicious.2007.autum/dataset/top3000.bookmarks"));
			String[] nextLine;
			while((nextLine = reader.readNext()) != null) {
				bookmarks.put(nextLine[0], idCounter);
				idCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		int numberOfSnapshots = 12;
		List<String> checkedTags = new ArrayList<String>();
		List<String> checkedBookmarks = new ArrayList<String>();
		List<Integer> numberOfTags = new ArrayList<Integer>();
		List<Integer> numberOfBookmarks = new ArrayList<Integer>();
		for(int i = 0; i < numberOfSnapshots; i++) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream("data/delicious.2007.autum/dataset/snapshot "+(i+1))));
				String line;
				int tagCounter = 0;
				int bookmarkCounter = 0;
				while((line = reader.readLine()) != null) {
					String[] parts = line.split("\t");
					if(parts.length == 4) {
						String bookmark = parts[2];
						String tag = parts[3];
						if(!tag.contains(",")) {
							if( (tags.containsKey(tag)) && (!checkedTags.contains(tag)) ) {
								tagCounter++;
								checkedTags.add(tag);
							}
							if((bookmarks.containsKey(bookmark)) && (!checkedBookmarks.contains(bookmark))) {
								bookmarkCounter++;
								checkedBookmarks.add(bookmark);
							}
						}
					}
				}
				numberOfTags.add(tagCounter);
				numberOfBookmarks.add(bookmarkCounter);
				System.out.println("Snapshot "+(i+1)+" has tags : "+tagCounter);
				System.out.println("Snapshot "+(i+1)+" has bookmarks : "+bookmarkCounter);
				tagCounter = 0;
				bookmarkCounter = 0;
				checkedTags.clear();
				checkedBookmarks.clear();
				reader.close();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		try {
			int snapshots = 12;
			BufferedWriter out = new BufferedWriter(new FileWriter("data/resourcesPerSnapshot"));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			String[] header = {"Snapshot","bookmarks", "tags","edges"};
			writer.writeNext(header);
			for(int i = 0; i < snapshots; i++) {
				DenseMatrix64F graph = MatrixIO.loadCSV("data/delicious.2007.autum/input/"+(i+1)+"delicious.matrix");
				double edges = CommonOps.elementSum(graph);
				System.out.println("Edges of snapshot "+(i+1)+" ="+edges);
				String[] values = {String.valueOf(i+1), String.valueOf(numberOfBookmarks.get(i)), 
						String.valueOf(numberOfTags.get(i)), String.valueOf(edges)};
				writer.writeNext(values);
			}
			writer.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This function finds out which are the most frequent bookmarks
	 * and writes them to a csv file. Depending on how many bookmarks
	 * you want you set the value numberOfBookmarks.
	 */
	public void writeMostFrequentBookmarks(Map<String, Integer> bookmarkCount) {
		int numberOfBookmarks = 3000;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("data/delicious.2005/top3000.bookmarks"));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			System.out.println("Sorting bookmarks....");
			Map<String, Integer> sortedByCountTags = sortByValue(bookmarkCount);
			System.out.println("End of sorting....");
			int counter = 1;
			System.out.println("Writing most "+numberOfBookmarks+" frequent bookmarks....");
			for(Map.Entry<String, Integer> entry : sortedByCountTags.entrySet()) {
				String tag = entry.getKey();
				int count = entry.getValue();
				String[] values = {tag,String.valueOf(count)};
				writer.writeNext(values);
				if(counter == numberOfBookmarks) {
					break;
				}
				counter++;
			}			
			System.out.println("End of writing");
			writer.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
		
	/**
	 * This function finds out which are the most frequent tags
	 * and writes them to a csv file. Depending on how many t
	 * you want you set the value numberOfBookmarks.
	 */
	public void writeMostFrequentTags(Map<String, Integer> tagCount) {
		int numberOfTags = 1500;
		try {
			System.out.println("Writing most "+numberOfTags+" frequent tags....");
			BufferedWriter out = new BufferedWriter(new FileWriter("data/delicious.2005/top1500.tags"));
			CSVWriter writer = new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			Map<String, Integer> sortedByCountTags = sortByValue(tagCount);
			int counter = 1;
			for(Map.Entry<String, Integer> entry : sortedByCountTags.entrySet()) {
				String tag = entry.getKey();
				int count = entry.getValue();
				String[] values = {tag,String.valueOf(count)};
				writer.writeNext(values);
				if(counter == numberOfTags) {
					break;
				}
				counter++;
			}
			System.out.println("End of writing");
			writer.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * This function counts the how many times a bookmark appear
	 * in the full dataset (all the snapshots).
	 */
	public Map<String, Integer> countBookmarks() {
		int numberOfSnapshots = 12;
		Map<String, Integer> bookmarkCount  = new HashMap<String, Integer>();
		for(int i = 0; i < numberOfSnapshots; i++) {
			try {
				System.out.println("Count bookmarks of snapshot "+(i+1)+"....");
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream("data/delicious.2007.autum/dataset/snapshot "+(i+1))));
				String line;
				bookmarkCount.clear();
				while((line = reader.readLine()) != null) {
					String[] parts = line.split("\t");
					if(parts.length == 4) { //we count bookmarks that form a triplet (user-bookmark-tag)
						String bookmark = parts[2];
						if(bookmark.length() < 80) { //we vount bookmarks that have at least 80 chars
							String tag = parts[3];
							if(!tag.contains(",")) { //we don't count if the tag in the triplet has "," char
								if(bookmarkCount.containsKey(bookmark)) {
									int count = bookmarkCount.get(bookmark);
									count++;
									bookmarkCount.put(bookmark, count);
								}
								else {
									bookmarkCount.put(bookmark, 1);
								}
							}
						}
					}
				}
				System.out.println("End of counting");
				System.out.println("Unique bookmarks  from snapshot"+(i+1)+"= "+bookmarkCount.size());
				reader.close();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		System.out.println("Unique bookmarks = "+bookmarkCount.size());
		return bookmarkCount;
	}
	
	/**
	 * This function counts the how many times a tag appear
	 * in the full dataset (all the snapshots).
	 */
	public Map<String, Integer> countTags() {
		int numberOfSnapshots = 12;
		Map<String, Integer> tagCount = new HashMap<String, Integer>();
		for(int i = 0; i < numberOfSnapshots; i++) {
			try {
				System.out.println("Count Tags of snapshot "+(i+1)+"...");
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream("data/delicious.2007.autum/dataset/snapshot "+(i+1))));
				String line;
				tagCount.clear();
				while((line = reader.readLine()) != null) {
					String[] parts = line.split("\t");
					if(parts.length == 4) { //we count bookmarks that form a triplet (user-bookmark-tag)
						String bookmark = parts[2];
						if(bookmark.length() < 80) { //we vount bookmarks that have at least 80 chars
							String tag = parts[3];
							if(!tag.contains(",")) { //we don't count if the tag in the triplet has "," char
								if(tagCount.containsKey(tag)) {
									int count = tagCount.get(tag);
									count++;
									tagCount.put(tag, count);
								}
								else {
									tagCount.put(tag, 1);
								}
							}
						}
					}
				}
				System.out.println("End of counting");
				System.out.println("Unique tags from snapshot "+(i+1)+"= "+tagCount.size());
				reader.close();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		System.out.println("Unique tags = "+tagCount.size());
		return tagCount;
	}
		
	/**
	 * Create bookmark - tag bipartite graph. We use only the resources that
	 * belong to the most frequent ones.
	 */
	public DenseMatrix64F createGraph(int snapshot, Map<String, Integer> topBookmarks, 
			Map<String, Integer> topTags) {
		DenseMatrix64F graph = new DenseMatrix64F(3000, 1500);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("data/delicious.2006.summer/dataset/snapshot "+snapshot)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				if(parts.length == 4) {
					String bookmark = parts[2];
					String tag = parts[3];
					if((topBookmarks.containsKey(bookmark)) && (topTags.containsKey(tag))) {
						int bookmarkId = topBookmarks.get(bookmark);
						int tagId = topTags.get(tag);
						double value = graph.get(bookmarkId-1, tagId-1);
						if(value == 0) {
							graph.set(bookmarkId-1, tagId-1, 1);
						}
					}
				}
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return graph;
	}
	
	/**
	 * Get tag labels from specific partition.
	 */
	public Map<String, Integer> getLabelsFromPartition(String labelsPath, int partitionNo) {
		Map<String, Integer> labels = new HashMap<String, Integer>();
		String[] labelStringParts = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(labelsPath)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(": ");
				String partitionIdString = parts[0];
				String[] partitionIdStringParts = partitionIdString.split(" ");
				int partitionId = Integer.parseInt(partitionIdStringParts[1]);
				if(partitionId == partitionNo) {
					String labelString = parts[1];
					labelString = labelString.substring(1);
					labelString = labelString.substring(0,labelString.length()-1);
					labelString = labelString.substring(0,labelString.length()-1);
					labelStringParts = labelString.split(",");
					break;
				}
			}
			reader.close();
			for(int i = 0; i < labelStringParts.length; i++) {
				String label = labelStringParts[i];
				labels.put(label, (i+1));
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return labels;
	}
	
	/**
	 * Read the top tags from the csv file
	 * @return the top tags stores in a map
	 */
	public Map<String, Integer> getTopTags(String topTagsPath) {
		Map<String, Integer> topTags = new HashMap<String, Integer>();
		int topTagsCounter = 1;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(topTagsPath)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				String tag = parts[0];
				topTags.put(tag, topTagsCounter);
				topTagsCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return topTags;
		
	}
	
	/**
	 * Read the top bookmarks from the csv file
	 * @return the top bookmarks stores in a map
	 */
	public Map<String, Integer> getTopBookmarks(String topBookmarksPath) {
		Map<String, Integer> topBookmarks = new HashMap<String, Integer>();
		int topBookmarkCounter = 1;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(topBookmarksPath)));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				String bookmark = parts[0];
				topBookmarks.put(bookmark, topBookmarkCounter);
				topBookmarkCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return topBookmarks;
	}
	
	/**
	 * Union datasets
	 */
	public void mergeDatasets() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("data/delicious.2007.autum/delicious.autum"));
			for(int i = 1; i < 4; i++) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						new FileInputStream("/Users/smpeis/Desktop/snapshot "+i)));
				String line;
				while((line = reader.readLine()) != null) {
					writer.write(line);
					writer.newLine();
				}
				reader.close();
			}
			writer.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * The dataset was splitted in months. We use this function
	 * to split every month into weeks. We then write every sub dataset
	 * to a csv file. It's obvious you have to set the proper values
	 * depending how you want to split the dataset.
	 */
	public void splitDataset() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("data/delicious.2007.autum/delicious.autum")));
			BufferedWriter writer1 = new BufferedWriter(new FileWriter
					("data/delicious.2007.autum/snapshot 9"));
			BufferedWriter writer2 = new BufferedWriter(new FileWriter
					("data/delicious.2007.autum/snapshot 10"));
			BufferedWriter writer3 = new BufferedWriter(new FileWriter
					("data/delicious.2007.autum/snapshot 11"));
			BufferedWriter writer4 = new BufferedWriter(new FileWriter
					("data/delicious.2007.autum/snapshot 12"));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				String date = parts[0];
				String[] dateParts = date.split("-");
				String check = dateParts[1] + "-" + dateParts[2];
				if((check.equals("10-29")) || (check.equals("10-30")) || (check.equals("10-31")) || 
						(check.equals("11-01")) || (check.equals("11-02")) || (check.equals("11-03")) ||
						(check.equals("11-04"))) {
					writer1.write(line);
					writer1.newLine();
				}
				else if((check.equals("11-05")) || (check.equals("11-06")) || (check.equals("11-07")) || 
						(check.equals("11-08")) || (check.equals("11-09")) || (check.equals("11-10")) ||
						(check.equals("11-11"))) {
					writer2.write(line);
					writer2.newLine();
				}
				else if((check.equals("11-12")) || (check.equals("11-13")) || (check.equals("11-14")) || 
						(check.equals("11-15")) || (check.equals("11-16")) || (check.equals("11-17")) ||
						(check.equals("11-18"))) {
					writer3.write(line);
					writer3.newLine();
				}
				else if((check.equals("11-19")) || (check.equals("11-20")) || (check.equals("11-21")) || 
						(check.equals("11-22")) || (check.equals("11-23")) || (check.equals("11-24")) ||
						(check.equals("11-25"))) {
					writer4.write(line);
					writer4.newLine();
				}
			}
			writer1.close();
			writer2.close();
			writer3.close();
			writer4.close();
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Returns the key of a map given a value.
	 */
	public String getKeyByValue(Map<String, Integer> map, int value) {
		String key = null;
		for(Map.Entry<String, Integer> entry : map.entrySet()) {
			if(value == entry.getValue()) {
				key = entry.getKey();
			}
		}
		return key;
	}
	
	/**
	 * Reads the file when the number of nodes per partition is writen.
	 * It stores them to a list.
	 */
	public List<Double> readlinesPerPartition(String linesPerPartitionPath) {
		List<Double> linesPerPartition = new ArrayList<Double>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(linesPerPartitionPath)));
			String line;
			while((line = reader.readLine()) != null) {
				double numberOfLines = Double.parseDouble(line);
				linesPerPartition.add(numberOfLines);
			}
			reader.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return linesPerPartition;
	}
	
	/**
	 * Sorts the map by value
	 */
	public Map<String, Integer> sortByValue(Map<String, Integer> map) {
		ValueComparator bvc =  new ValueComparator(map);
        TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
        sortedMap.putAll(map);
        return sortedMap;
	}
}

class ValueComparator implements Comparator<String> {

    Map<String, Integer> base;
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }
    
    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) > base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}

