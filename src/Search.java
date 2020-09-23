import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * Class where we search queries from InvertedIndex, create Word objects and sort them
 */
public class Search {
	
	WorkQueue queue;
	Map<String, List<Word>> foundQueries;
	InvertedIndex invertedIndex;
	
	/**
	 * constructor for search
	 * @throws IOException 
	 */
	public Search(InvertedIndex invertedIndex, WorkQueue queue, File queryDirectory, boolean exact) throws IOException {
		this.queue = queue;
		this.foundQueries = new TreeMap<String, List<Word>>();
		this.invertedIndex = invertedIndex;
		if (exact) {
			searchQueryFiles(queryDirectory, true); 
		} else {
			searchQueryFiles(queryDirectory, false); 
		}
		queue.finish();
	    queue.shutdown();
	}
	
	public Search(InvertedIndex invertedIndex, List<String> query, boolean exact, WorkQueue queue) {
		this.queue = queue;
		this.foundQueries = new TreeMap<String, List<Word>>();
		this.invertedIndex = invertedIndex;
		if (exact) {
			queue.execute(new MultithreadedExactSearch(query));
		} else {
			queue.execute(new MultithreadedPartialSearch(query));
		}
		queue.finish();
	    queue.shutdown();
	}

	
	/**
	 * Function that recursively travels through all the directories and searches for txt files
	 *
	 * @param dir
	 *            path to directory or a single file
	 * @return list of txt files in the given directory 
	 * @throws IOException 
	 */
	private List<File> searchQueryFiles(File dir, boolean exact) throws IOException {
		List<File> queries = new ArrayList<File>();
		// check if the dir is directory or a file
		if (dir.isDirectory()) {
			// if it is a directory, then recursively travel and add all txt files to queries
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					queries.addAll(searchQueryFiles(file, exact));
				} else {
					if (file.getName().toLowerCase().endsWith(".txt")) {
						if (exact) {
							List<String> query = Files.readAllLines(file.toPath(), Charset.defaultCharset());
							queue.execute(new MultithreadedExactSearch(query));
						} else {
							List<String> query = Files.readAllLines(file.toPath(), Charset.defaultCharset());
							queue.execute(new MultithreadedPartialSearch(query));
						}
					}         
				}
			}	
			// if it is a file then check it has txt extension, if so add to queries
		} else {
			if (dir.getName().toLowerCase().endsWith(".txt")) {
				if (exact) {
					List<String> query = Files.readAllLines(dir.toPath(), Charset.defaultCharset());
					queue.execute(new MultithreadedExactSearch(query));
				} else {
					List<String> query = Files.readAllLines(dir.toPath(), Charset.defaultCharset());
					queue.execute(new MultithreadedPartialSearch(query));
				}
			}  
		}
		return queries;
	}

	/*
	 * MultithreadedExactSearch class
	 */
	private class MultithreadedExactSearch implements Runnable {
		List<String> query;

		public MultithreadedExactSearch(List<String> query) {
			this.query = query;
		}

		@Override
		public void run() {		
			try {
				List<TreeSet<String>> clearQueries = FileCleaner.clearQuery(query);
				exactSearch(clearQueries);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
		
	
	/*
	 * MultithreadedPartialSearch class
	 */
	private class MultithreadedPartialSearch implements Runnable {
		List<String> query;


		public MultithreadedPartialSearch(List<String> query) {
			this.query = query;
		}
		
		public void run() {	
			try {
				List<TreeSet<String>> clearQueries = FileCleaner.clearQuery(query);
				partialSearch(clearQueries);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * exactSearch method 
	 *
	 * @param clearQueries 
	 * 					queries for search
	 * @void
	 */
	private void exactSearch(List<TreeSet<String>> clearQueries) {
		// travel through our structures to find all the necessary infromation
		for (TreeSet<String> query: clearQueries) {
			// we need this string to store the full query in one line
			// for example "aar alpaca elephant"
			String fullQuery = "";
			// helper strcture that will store location (path) as a key, and position and count in an array as value
			Map<String, int[]> result = new HashMap<String, int[]>();
			for (String word : query) {
				fullQuery += word + " ";
				invertedIndex.lock.lockReadOnly();
				if (invertedIndex.containsKey(word)) {
					Set<TreeMap<String, Set<Integer>>> setOfPaths = invertedIndex.get(word);
					invertedIndex.lock.unlockReadOnly();
					Iterator <TreeMap<String, Set<Integer>>> maps = setOfPaths.iterator();
					while(maps.hasNext()) {
						TreeMap<String, Set<Integer>> mapsNext = maps.next();
						Iterator <String> dir = mapsNext.keySet().iterator();
						while(dir.hasNext()) {
							String dirNext = dir.next();
							// we need to find min position to store in structure
							// so, I chose the minimal value possible to compare it firstly
							int position = Integer.MAX_VALUE;
							int count = 0;
							// if this location already exists in our helper map then just take all the values from it
							if (result.containsKey(dirNext)) {
								position = result.get(dirNext)[0];
								count = result.get(dirNext)[1];
							} 
							Set<Integer> setOfPos = mapsNext.get(dirNext);
							Iterator<Integer> pos = setOfPos.iterator();
							while (pos.hasNext()){
								int posNext = pos.next();
								count++;
								if (posNext < position) {
									position = posNext;
								}
							}
							result.put(dirNext, new int[] {position, count});
						}
						
					}
				}

			}
			Iterator <String> path = result.keySet().iterator();
			// create List of Word objects
			List<Word> words = new ArrayList<Word>();
			while(path.hasNext()) {
				String pathNext = path.next();
				int position = result.get(pathNext)[0];
				int count = result.get(pathNext)[1];
				String location = pathNext;
				Word word = new Word(count, position, location);
				words.add(word);
			}
			//sort Word objects
			Collections.sort(words);
			String newQuery = fullQuery.trim();
			// add everything to our final structure to return
			synchronized(foundQueries) {
				foundQueries.put(newQuery, words);
			}
		}
		

				
	}
	
	/**
	 * partialSearch method 
	 *
	 * @param clearQueries 
	 * 					queries for search
	 * @void
	 */
	private void partialSearch(List<TreeSet<String>> clearQueries) {
		for (TreeSet<String> query: clearQueries) {
			String fullQuery = "";
			Map<String, int[]> result = new HashMap<String, int[]>();
			for (String word : query) {
				fullQuery += word + " ";
				// we need to iterate through all keys and use only keys that start with query word
				invertedIndex.lock.lockReadOnly();
				for (String keyWord : invertedIndex.keySet()) {
					if (keyWord.startsWith(word)) {
						Set<TreeMap<String, Set<Integer>>> setOfPaths = invertedIndex.get(keyWord);
						invertedIndex.lock.unlockReadOnly();
						Iterator <TreeMap<String, Set<Integer>>> maps = setOfPaths.iterator();
						while(maps.hasNext()) {
							TreeMap<String, Set<Integer>> mapsNext = maps.next();
							Iterator <String> dir = mapsNext.keySet().iterator();
							while(dir.hasNext()) {
								String dirNext = dir.next();
								int position = Integer.MAX_VALUE;
								int count = 0;
								if (result.containsKey(dirNext)) {
									position = result.get(dirNext)[0];
									count = result.get(dirNext)[1];
								} 
								Set<Integer> setOfPos = mapsNext.get(dirNext);
								Iterator<Integer> pos = setOfPos.iterator();
								while (pos.hasNext()){
									int posNext = pos.next();
									count++;
									if (posNext < position) {
										position = posNext;
									}
								}
								result.put(dirNext, new int[] {position, count});
							}

						}
					}
				}
			}
			Iterator <String> path = result.keySet().iterator();
			List<Word> words = new ArrayList<Word>();
			while(path.hasNext()) {
				String pathNext = path.next();
				int position = result.get(pathNext)[0];
				int count = result.get(pathNext)[1];
				String location = pathNext;
				Word word = new Word(count, position, location);
				words.add(word);
			}
			Collections.sort(words);
			String newQuery = fullQuery.trim();

			synchronized(foundQueries) {
				foundQueries.put(newQuery, words);
			}
		}

	}
	
	/**
	 * method that returns keySet from global variale foundQueries
	 */
	public Set<String> keySet() {
		return foundQueries.keySet();
	}
	
	/**
	 * method that returns the value from the key from foundQueries global variable
	 */
	public List<Word> get(String query) {
		return foundQueries.get(query);
	}
	
	public Map<String, List<Word>> getResult() {
		return foundQueries;
	}

}
