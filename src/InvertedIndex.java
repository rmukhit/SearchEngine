import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * InvertedIndex class that created InvertedIndex that stores all information about word
 */
public class InvertedIndex{
	private Map<String, Set<TreeMap<String, Set<Integer>>>> invertedIndex;
	private WorkQueue queue;
	ReadWriteLock lock;
	private Map<String, String> urls;
	private int limit;
	
	/** Port used by socket. For web servers, should be port 80. */
	public final int DEFAULT_PORT = 80;

	/** Version of HTTP used and supported. */
	public final String version = "HTTP/1.1";

	/** Valid HTTP method types. */
	public enum HTTP {OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT};


	/**
	 * constructor
	 * @throws IOException 
	 */
	public InvertedIndex(File dir, WorkQueue queue) throws IOException {
		this.invertedIndex = new TreeMap<String, Set<TreeMap<String, Set<Integer>>>> ();
		this.queue = queue;
		this.lock = new ReadWriteLock();
		searchFiles(dir);
		queue.finish();
		queue.shutdown();
	}
	
	public InvertedIndex(String urlString, int limit) throws URISyntaxException, IOException {
		this.invertedIndex = new TreeMap<String, Set<TreeMap<String, Set<Integer>>>> ();
		this.queue = new WorkQueue();
		this.urls = new HashMap<>();
		this.limit = limit;	
		this.lock = new ReadWriteLock();
		
		URL url = new URL(urlString);
		urls.put(url.getPath(), null);
		
		queue.execute(new MultithreadedInvertedIndexURL(urlString));
		queue.finish();
		queue.shutdown();

	}
	
	public List<String> fetchLines(URL url, String request) throws UnknownHostException, IOException {
		ArrayList<String> lines = new ArrayList<>();
		int port = url.getPort() < 0 ? DEFAULT_PORT : url.getPort();

		try (
				Socket socket = new Socket(url.getHost(), port);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				PrintWriter writer = new PrintWriter(socket.getOutputStream());
		) {
			writer.println(request);
			writer.flush();

			String line = null;

			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}

		return lines;
	}

	/**
	 * Crafts a minimal HTTP/1.1 request for the provided method.
	 *
	 * @param url
	 *            - url to fetch
	 * @param type
	 *            - HTTP method to use
	 *
	 * @return HTTP/1.1 request
	 *
	 * @see {@link HTTP}
	 */
	public String craftHTTPRequest(URL url, HTTP type) {
		String host = url.getHost();
		String resource = url.getFile().isEmpty() ? "/" : url.getFile();

		// The specification is specific about where to use a new line
		// versus a carriage return!
		return String.format("%s %s %s\r\n" + "Host: %s\r\n" + "Connection: close\r\n" + "\r\n", type.name(), resource,
				version, host);
	}

	/**
	 * Fetches the HTML for the specified URL (without headers).
	 *
	 * @param url
	 *            - url to fetch
	 * @return HTML as a single {@link String}, or null if not HTML
	 *
	 * @throws UnknownHostException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public String fetchHTML(String url) throws UnknownHostException, MalformedURLException, IOException {
		URL target = new URL(url);
		String request = craftHTTPRequest(target, HTTP.GET);
		List<String> lines = fetchLines(target, request);

		int start = 0;
		int end = lines.size();

		// Determines start of HTML versus headers.
		while (!lines.get(start).trim().isEmpty() && start < end) {
			start++;
		}

		// Double-check this is an HTML file.
		Map<String, String> fields = parseHeaders(lines.subList(0, start + 1));
		String type = fields.get("Content-Type");

		if (type != null && type.toLowerCase().contains("html")) {
			return String.join(System.lineSeparator(), lines.subList(start + 1, end));
		}

		return null;
	}

	/**
	 * Helper method that parses HTTP headers into a map where the key is the
	 * field name and the value is the field value. The status code will be
	 * stored under the key "Status".
	 *
	 * @param headers
	 *            - HTTP/1.1 header lines
	 * @return field names mapped to values if the headers are properly
	 *         formatted
	 */
	public Map<String, String> parseHeaders(List<String> headers) {
		Map<String, String> fields = new HashMap<>();

		if (headers.size() > 0 && headers.get(0).startsWith(version)) {
			fields.put("Status", headers.get(0).substring(version.length()).trim());

			for (String line : headers.subList(1, headers.size())) {
				String[] pair = line.split(":", 2);

				if (pair.length == 2) {
					fields.put(pair[0].trim(), pair[1].trim());
				}
			}
		}

		return fields;
	}


	private class MultithreadedInvertedIndexURL implements Runnable {
		String link;

		public MultithreadedInvertedIndexURL(String link) {
			this.link = link;
		}
		

		@Override
		public void run() {
			
			try {

				String inspectedString = fetchHTML(link);
				URL url = new URL(link);
				String absolute = url.getProtocol() + "://" + url.getAuthority();
				Map<String, String[]> clearStrings = FileCleaner.clearHTML(inspectedString, absolute + url.getPath());
				addToInvertedIndex(clearStrings);
				Pattern linkPattern = Pattern.compile("a .*?href=\"(.*?)\"");
				Matcher linkMatcher = linkPattern.matcher(inspectedString);
				String insideUrlString = null;
				synchronized(urls) {
					while (linkMatcher.find()) {
						if (urls.size() < limit) {
							insideUrlString = linkMatcher.group(1);
							URL insideUrl = clean(new URL(url, insideUrlString));
							if (insideUrl.getPath().toLowerCase().endsWith((".html")) || insideUrl.getPath().toLowerCase().endsWith((".htm"))) {
								if (!urls.containsKey(insideUrl.getPath())) {
									urls.put(insideUrl.getPath(), null);
									queue.execute(new MultithreadedInvertedIndexURL(absolute + insideUrl.getPath()));
								}				
							}
						}
					}
					return;
				}	
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			

		}
	}
	
	public URL clean(URL url) {
		try {
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), null).toURL();
		}
		catch (MalformedURLException | URISyntaxException e) {
			return url;
		}
	}

	/*
	 * MultithreadedInvertedIndex private class
	 */
	private class MultithreadedInvertedIndex implements Runnable {
		String html;
		String path;

		public MultithreadedInvertedIndex(String html, String path) {
			this.html = html;
			this.path = path;
		}
		

		@Override
		public void run() {
			try {
				Map<String, String[]> clearStrings = FileCleaner.clearHTML(html, path);
				addToInvertedIndex(clearStrings);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	
	/**
	 * Function that recursively travels through all the directories and searches for html files
	 *
	 * @param dir
	 *            path to directory or a single file
	 * @return list of html files in the given directory 
	 * @throws IOException 
	 */
	public List<File> searchFiles(File dir) throws IOException {
		List<File> htmlFiles = new ArrayList<File>();
		// check if the dir is directory or a file
		if (dir.isDirectory()) {
			// if it is a directory, then recursively travel and add all html files to List htmlFiles
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					htmlFiles.addAll(searchFiles(file));
				} else {
					if (file.getName().toLowerCase().endsWith((".html")) || file.getName().toLowerCase().endsWith((".htm"))) {
						String html = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
						queue.execute(new MultithreadedInvertedIndex(html, file.getPath()));
					}         
				}
			}	
			// if it is a file then check it has html extension, if so add to htmlFiles
		} else {
			if (dir.getName().toLowerCase().endsWith((".html")) || dir.getName().toLowerCase().endsWith((".htm"))) {
				String html = new String(Files.readAllBytes(dir.toPath()), StandardCharsets.UTF_8);
				queue.execute(new MultithreadedInvertedIndex(html, dir.getPath()));
			}  
		}
		return htmlFiles;
	}
	

	/**
	 * Method that adds all the words, its' positions and paths to invertedIndex nested structure.
	 *
	 * @param clearStrings
	 *            Map that stores paths as a key and array of strings as the value.
	 */
	private void addToInvertedIndex(Map<String, String[]> clearStrings) {
		//add everything step by step to Nested Map

		lock.lockReadWrite();
		
		for(String path: clearStrings.keySet()) {
			String[] words = clearStrings.get(path);
			for (int i = 0; i < words.length; i++) {
				if (invertedIndex.containsKey(words[i])) {
					for (Map<String, Set<Integer>> maps : invertedIndex.get(words[i])) {							
						if (maps.containsKey(path)) {
							Set<Integer> intPath = maps.get(path);
							intPath.add(i+1);
						} else {
							Set<Integer> newWordIndex = new TreeSet<Integer>();
							newWordIndex.add(i+1);
							maps.put(path, newWordIndex);
						}
					}
				} else {
					Set<TreeMap<String, Set<Integer>>> setOfPaths = new HashSet<TreeMap<String, Set<Integer>>>();
					TreeMap<String, Set<Integer>> maps = new TreeMap<String, Set<Integer>> ();
					Set<Integer> newWordIndex = new TreeSet<Integer>();
					newWordIndex.add(i+1);
					maps.put(path, newWordIndex);
					setOfPaths.add(maps);
					invertedIndex.put(words[i], setOfPaths);	
				}
			}
		}
		
		lock.unlockReadWrite();
	}

	/** 
	 * Function that returns Set of all keys from map
	 * @param 
	 * @return Set<String>
	 */
	public Set<String> keySet() {

		return invertedIndex.keySet();

	}
	
	/** 
	 * Getter that returns value from map
	 * @param word String
	 * @return Set<TreeMap<String, Set<Integer>>>
	 */
	public Set<TreeMap<String, Set<Integer>>> get (String word) {
	
		return invertedIndex.get(word);

	}
	
	/** 
	 * Function that returns boolean whether some key is in map
	 * @param word String
	 * @return boolean
	 */
	public boolean containsKey(String word) {
	
		return invertedIndex.containsKey(word);

	}

}