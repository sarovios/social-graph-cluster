package sgc.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The above implementation is taken from:
 * https://code.google.com/p/ext-c/source/browse/trunk/src/nz/ac/vuw/ecs/kcassell/similarity/GoogleDistanceCalculator.java
 * This implements the Normalized Google Distance (NGD) as described in
 * R.L. Cilibrasi and P.M.B. Vitanyi, "The Google Similarity Distance",
 * IEEE Trans. Knowledge and Data Engineering, 19:3(2007), 370 - 383
 */

public class NormalizedGoogleDistance {
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		NormalizedGoogleDistance calculator = new NormalizedGoogleDistance();
		calculator.clearCache();
		String term1 = "dog";
		String term2 = "animal";
		Double distance2terms = calculator.calculateDistance(term1, term2);
		System.out.println("Distance from " + term1 + " to " + term2 +
				" = " + distance2terms);
	}
	
	private static final String SINDICE_SEARCH_SITE_PREFIX = 
			"http://api.sindice.com/v3/search?q=";
	private static final String FLICKR_SEARCH_SITE_PREFIX = 
			"http://api.flickr.com/services/rest/?method=flickr.photos.search" +
			"&api_key=" + //you should request your personal key from flickr
			"&format=json&text=";
                
	/** The file in the eclipse install directory containing a textual rep.
	 * of the cache.         
	*/
	protected static final String CACHE_FILE_NAME = "google.cache";

	static int counter = 0;

	/** 
	 * The logarithm of a number that is (hopefully) greater than or equal
	 *  to the (unpublished) indexed number of Google documents.
	 *  http://googleblog.blogspot.com/2008/07/we-knew-web-was-big.html
	 *  puts this at a trillion or more.  
	 */
	protected final static double logN = Math.log(1.0e12);

	Map<String, Double> cache = new HashMap<String, Double>();
        
	/** Holds the new terms we entered (these are also in the cache) 
	 */
	Map<String, Double> newCache = new HashMap<String, Double>();

	public NormalizedGoogleDistance() throws NumberFormatException, IOException {
    	cache = setupCache(CACHE_FILE_NAME);
	}

	public void clearCache() {
	cache = new HashMap<String, Double>();
	newCache = new HashMap<String, Double>();
	File cacheFile = new File(CACHE_FILE_NAME);
	cacheFile.delete();
	}
        
	protected Map<String, Double> setupCache(String filename) 
			throws NumberFormatException, IOException {
		File cacheFile = new File(filename);
		if (cacheFile.canRead()) {
			
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			Map<String, Double> cache = new HashMap<String, Double>();
			String line;
			while ((line = reader.readLine()) != null) {
				int lastSpaceIndex = line.lastIndexOf(' ');
				String token = line.substring(0, lastSpaceIndex);
				double count = Double.parseDouble(line.substring(lastSpaceIndex + 1));
				cache.put(token, count);
			}
			reader.close();
		}
		return cache;
	}

	/**
	* Adds the contents of newCache to the specified file
	* @param filename
	*/
	protected void updateCache(String filename) {
		if (counter++ >= 20) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(filename, true));
				for (Map.Entry<String, Double> entry : newCache.entrySet()) {
					writer.append(entry.getKey() + " " + entry.getValue() + "\n");
				}
				newCache = new HashMap<String, Double>();
				counter = 0;
			} 
			catch (IOException e) {
				// Things will just take longer
			} 
			finally {
				if (writer != null) {
					try {
						writer.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected double numResultsFromWeb(String term)
			throws JSONException, IOException {
		double result = 0;                
		if (cache.containsKey(term)) {
			result = cache.get(term);
		} 
		else {
			URL url = null;
			InputStream stream = null;
			try {
				url = makeQueryURL(term);
				URLConnection connection = url.openConnection();
//				connection.setConnectTimeout(2000);
				stream = connection.getInputStream();
				InputStreamReader inputReader = new InputStreamReader(stream);
				BufferedReader bufferedReader = new BufferedReader(inputReader);
				double count = getCountFromQuery(bufferedReader);
//				System.out.println(term + ":\t" + count + " hits");
				cache.put(term, count);
				newCache.put(term, count);
				updateCache(CACHE_FILE_NAME);
				result = count;
			}
			finally {
				if (stream != null) {
				}
			}
		}
		return result;
	}

	private double getCountFromQuery(BufferedReader reader)
			throws JSONException, IOException {
//	double count = getCountFromSindiceQuery(reader);
		double count = getCountFromFlickrQuery(reader);
	return count;
        }
        
	@SuppressWarnings("unused")
	private double getCountFromSindiceQuery(BufferedReader bufferedReader) 
			throws JSONException, IOException {
		String line;
		StringBuilder builder = new StringBuilder();
		while((line = bufferedReader.readLine()) != null) {
			builder.append(line);
		}
		String response = builder.toString();
		JSONObject json = new JSONObject(response);
		double count = 0;
		try {
				count = json.getDouble("totalResults");
		}
		catch (JSONException e) {
			count = 0;
		}
	return count;    	
	}
	
	private double getCountFromFlickrQuery(BufferedReader bufferedReader) 
    		throws IOException, JSONException {
    	String line;
		StringBuilder builder = new StringBuilder();
		while((line = bufferedReader.readLine()) != null) {
			builder.append(line);
		}
		String response = builder.toString();
		response = response.substring(0,response.length()-1);
		response = response.substring(14);
		JSONObject json = new JSONObject(response);
		JSONObject photos = json.getJSONObject("photos");
		double count = 0;
		try {
				count = photos.getDouble("total");
		}
		catch (JSONException e) {
			count = 0;
		}
	return count;    	
    }

	protected URL makeQueryURL(String term) throws MalformedURLException, IOException {
	String searchTerm = URLEncoder.encode(term, "UTF-8");
	URL url;
	String urlString = makeFlickrQueryString(searchTerm);
//	String urlString = makeSindiceQueryString(searchTerm);
	url = new URL(urlString);
	return url;
	}
        
	/**
	 * Buils a query string suitable from flickr
	* @param searchTerm
	*/
	private String makeFlickrQueryString(String searchTerm) {
		String urlString = FLICKR_SEARCH_SITE_PREFIX + searchTerm;
			return urlString;
	}
        
	/**
	* Builds a query string suitable for Sindice
	* @param searchTerm
	*/
	@SuppressWarnings("unused")
	private String makeSindiceQueryString(String searchTerm) {
		String urlString = SINDICE_SEARCH_SITE_PREFIX + searchTerm + "&format=json";
		return urlString;
	}

	/**
	* Calculates the normalized Google Distance (NGD) between the two terms
	* specified.  NOTE: this number can change between runs, because it is
	* based on the number of web pages found by Google, which changes.
	* @return a number from 0 (minimally distant) to 1 (maximally distant),
	*   unless an exception occurs in which case, it is negative
	*   (RefactoringConstants.UNKNOWN_DISTANCE)
	*/
	public Double calculateDistance(String term1, String term2) {
		double distance = 0;
		try {
			double min = numResultsFromWeb(term1);
			double max = numResultsFromWeb(term2);
			double both = numResultsFromWeb(term1 + " " + term2);
			// if necessary, swap the min and max
			if (max < min) {
				double temp = max;
				max = min;
				min = temp;
			}
			if (min > 0.0 && both > 0.0) {
				distance = (Math.log(max) - Math.log(both)) / (logN - Math.log(min));
			} 
			else {
				distance = 1.0;
			}                        
			// Counts change and are estimated, so there would be a possibility
			// of a slightly negative distance.
			if (distance < 0.0) {
				distance = 0.0;
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	return distance;
	}
}
