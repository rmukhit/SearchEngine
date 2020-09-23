import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;


/*
 * Class Json that prints everything in a Json format
 */
public class Json {
	
	/**
	 * Function that writes invertedIndex to JSON file and creates this file in the given path.
	 *
	 * @param invertedIndex
	 *           InvertedIndex that stores all our words, file paths where words are found and their positions ib the text
	 *    @param path
	 *           path where we should store JSON file        
	 * @void
	 */
	public static void writeJson(InvertedIndex invertedIndex, String path) {
		try {
			File fileDir = new File(path);
			// write using UTF8 encoding
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDir), "UTF8"));
			
			//append strings to the Writer out that we will write to JSON
			// step by step using JSON standard add all the tabs, quotation marks, angle brackets etc
			out.append("{ \n");
			Iterator <String> word = invertedIndex.keySet().iterator();
			while (word.hasNext()) {
				String wordNext = word.next();
				out.append("\t\""+ wordNext + "\": {\n");
				Set<TreeMap<String, Set<Integer>>> setOfPaths = invertedIndex.get(wordNext);
				Iterator <TreeMap<String, Set<Integer>>> maps = setOfPaths.iterator();
				while(maps.hasNext()) {
					TreeMap<String, Set<Integer>> mapsNext = maps.next();
					Iterator <String> dir = mapsNext.keySet().iterator();
					while(dir.hasNext()) {
						String dirNext = dir.next();
						out.append("\t\t\"" + dirNext + "\": [\n");
						Set<Integer> setOfPos = mapsNext.get(dirNext);
						Iterator<Integer> pos = setOfPos.iterator();
						// I use iterator to check should we add comma to the end of each structure or no
						while (pos.hasNext()){
							int posNext = pos.next();
							if(pos.hasNext()) {
								out.append("\t\t\t" + posNext +",\n");
							} else {
								out.append("\t\t\t" + posNext +"\n");
							}						
						}
						if(dir.hasNext()) {
							out.append("\t\t],\n");
						} else {
							out.append("\t\t]\n");
						}		
					}						
				}
				if(word.hasNext()) {
					out.append("\t},\n");
				} else {
					out.append("\t}\n");
				}			
			}
			out.append("}");
			out.close();
		} 
		catch (Exception e) {
			System.out.println(e.getMessage());
		} 
	}
	
	/**
	 * Function that writes foundQueries to JSON file and creates this file in the given path.
	 *
	 * @param foundQueries
	 *           Nested Map that stores all queries, found words, its position, location and frequency
	 *    @param path
	 *           path where we should store JSON file        
	 * @void
	 */
	public static void writeQueryJson(Search foundQueries, String path) {
		try {
			File fileDir = new File(path);
			// write using UTF8 encoding
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDir), "UTF8"));
			
			//append strings to the Writer out that we will write to JSON
			// step by step using JSON standard add all the tabs, quotation marks, angle brackets etc
			out.append("[\n");
			
			Iterator<String> querie = foundQueries.keySet().iterator();
			while (querie.hasNext()) {
				out.append("\t{\n");
				String querieNext = querie.next();
				out.append("\t\t\"queries\": \""+ querieNext + "\",\n");
				out.append("\t\t\"results\": [\n");
				List<Word> listOfWords = foundQueries.get(querieNext);
				Iterator <Word> words = listOfWords.iterator();
				while(words.hasNext()) {
					Word wordNext = words.next();
					out.append("\t\t\t{\n");
					out.append("\t\t\t\t\"where\": \"" + wordNext.getLocation() + "\",\n");
					out.append("\t\t\t\t\"count\": " + wordNext.getFrequency() + ",\n");
					out.append("\t\t\t\t\"index\": " + wordNext.getPosition() + "\n");
					if (words.hasNext()) {
						out.append("\t\t\t},\n");
					} else {
						out.append("\t\t\t}\n");
					}
				}
				out.append("\t\t]\n");
				if (querie.hasNext()) {
					out.append("\t},\n");
				} else {
					out.append("\t}\n");
				}					
			}
			
			out.append("]");	
			out.close();
		} 
		catch (Exception e) {
			System.out.println(e.getMessage());
		} 
	}
	
	
}
