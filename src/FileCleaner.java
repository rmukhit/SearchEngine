import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * FileCleaner Class that cleans all files from unnecessary characters and etc.
 */
public class FileCleaner {	

	/**
	 * Function that cleans all the htmls files and converts them to array of strings
	 *
	 * @param files
	 *            List of html files
	 * @return TreeMap that has path to the file as key and array of strings as value
	 */	
	public static Map<String, String[]> clearHTML(String html, String path) throws IOException {
		// I chose TreeMap structure because I need to sort paths in alphabetical order
		Map<String, String[]> clearStrings = new TreeMap<String, String[]>();

			// clean the string
			String stripedHTML = strip(html);
			// if string is "" then do not convert it to array of strings
			if (!stripedHTML.equals("")) {
				//add the whole html string to array by single white-space
				String[] arrayOfWords = stripedHTML.split(" ");
				// put path and array of strings to TreeMap 
				clearStrings.put(path, arrayOfWords);			
			}
		
		return clearStrings;
	}
	
	/**
	 * Function that cleans all the txt files and converts them to list of treeset of strings
	 *
	 * @param files
	 *            List of txt files
	 * @return List of treeset that stores all the cleaned queries
	 */
	public static List<TreeSet<String>> clearQuery(List<String> query) throws IOException {
		// create nested structure that stores all the cleaned queries
		List<TreeSet<String>> listOfQueries = new ArrayList<TreeSet<String>>();
		
			for (String line : query) {
				// clean each String element (line = single query)
				line = strip(line);
				// store each word from cleaned query as a seperate element in TreeSet
				TreeSet<String> listOfWords = new TreeSet<String>();
				for(String word : line.split(" ")) {
					if(word.length() > 0) {
						listOfWords.add(word);
					}	  
				}
				if (listOfWords.size() > 0) {
					listOfQueries.add(listOfWords);	
				}
			}

		return listOfQueries;
	}
	
	/**
	 * Removes all HTML and non-alphabetic characters (including any CSS and JavaScript), 
	 * multiple white-spaces and sets the whole string to lower case.
	 *
	 * @param html
	 *            text including HTML to remove
	 * @return text without any HTML, CSS, JavaScript or non-alphabetic characters, multiple white-spaces, 
	 * all words are lower case
	 */
	private static String strip(String html) {
		html = stripComments(html);

		html = stripElement(html, "head");
		html = stripElement(html, "style");
		html = stripElement(html, "script");

		html = stripTags(html);
		html = stripEntities(html);
		html = stripNumbers(html);
		// trim the html file so there is no white-spaces 
		// at the beginning and end of the string
		html = html.toLowerCase().trim();
		html = replaceMultipleWhitespaces(html);

		return html;
	}
	
	/**
	 * Replaces all HTML entities with a single space. For example,
	 * "2010&ndash;2012" will become "2010 2012".
	 *
	 * @param html
	 *            text including HTML entities to remove
	 * @return text without any HTML entities
	 */
	private static String stripEntities(String html) {
		return html.replaceAll("&[^ ;]+;", " ");
	}

	/**
	 * Replaces all HTML comments with a single space. For example, "A<!-- B
	 * -->C" will become "A C".
	 *
	 * @param html
	 *            text including HTML comments to remove
	 * @return text without any HTML comments
	 */
	private static String stripComments(String html) {
		return html.replaceAll("(?s)<!--.*?-->", " ");
	}

	/**
	 * Replaces all HTML tags with a single space. For example, "A<b>B</b>C"
	 * will become "A B C".
	 *
	 * @param html
	 *            text including HTML tags to remove
	 * @return text without any HTML tags
	 */
	private static String stripTags(String html) {
	     return html.replaceAll("\\<[^>]*>"," ");
	}
	     
	/**
	 * Replaces everything between the element tags and the element tags
	 * themselves with a single space. For example, consider the html code: *
	 *
	 * <pre>
	 * &lt;style type="text/css"&gt;body { font-size: 10pt; }&lt;/style&gt;
	 * </pre>
	 *
	 * If removing the "style" element, all of the above code will be removed,
	 * and replaced with a single space.
	 *
	 * @param html
	 *            text including HTML elements to remove
	 * @param name
	 *            name of the HTML element (like "style" or "script")
	 * @return text without that HTML element
	 */
	private static String stripElement(String html, String name) {
		String regex = "<((?i)" + name + ")([\\s\\S]+?)</(?i)" + name + "([\\s]*)>";
		return html.replaceAll(regex," ");
	}
	
	/**
	 * Replaces all non-alphabetic characters with a single space. For example: 
	 * "Hello I have 3 children!" will become "Hello I have children"
	 *
	 *
	 * @param html
	 *            text including all non-alphabetic characters to remove
	 * @return text without that non-alphabetic characters
	 */
	private static String stripNumbers(String html) {
		return html.replaceAll("(?U)[^\\p{Alpha}\\p{Space}]+"," ");
	}
	
	/**
	 * Replaces all multiple white-spaces, tabs, new lines, non-breaking spaces (#160), 
	 * figure spaces (#8199) and etc to one single space
	 *
	 * @param html
	 *            text including all multiple white-spaces to remove characters to remove
	 * @return text without multiple white-spaces
	 */
	private static String replaceMultipleWhitespaces(String html) {
		html = html.replaceAll("[^\\p{L}\\p{Nd}]+", " ");
		html = html.replaceAll("\u00A0"," ");
		html = html.replaceAll("\\s+"," ");
		return html;
	}
}
	
	
	
	
	
