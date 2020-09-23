import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/FirstServlet")

public class SearchServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;
	InvertedIndex invertedIndex;

	/**
	 * Default constructor. 
	 */
	public SearchServlet(InvertedIndex invertedIndex) {
		this.invertedIndex = invertedIndex;
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		String requestedField = request.getParameter("field");
		long startTime = System.nanoTime();
				
		List<String> query = new ArrayList<String>(Arrays.asList(requestedField.split(" ")));
		
		if (request.getParameter("privacy").equals("off")) {
			HttpSession session = request.getSession(true);
			session.setAttribute(requestedField, getShortDate());
		} 

		Boolean exact = false;
		String toggle = request.getParameter("toggle");
		if(toggle.equals("exact")) {
			exact = true;
		} 
		
		TreeMap<String, String> results = search(query, exact);
		
		prepareResponse("Search results", response);
		for (String result: results.keySet()) {
				String outputString="<a href = "+"\""+ result +"\">"+ result +"</a><br>";
				response.getWriter().println(outputString);
		}
	    long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
		response.getWriter().println("<p> Number of results: " + results.size() + "</p>");
		response.getWriter().println("<p> Time taken to calculate and fetch those results: " + elapsedTime + " nanoseconds </p>");
		finishResponse(request, response);

	}
	
	
	/** 
	 * Method that searches for the entered query on the web
	 * @param query list of words to be searched
	 * @param whether exact or partial search
	 * @return TreeMap<String, String> map of urls
	 */
	private TreeMap<String, String> search(List<String> query, boolean exact) {
		
		WorkQueue queue = new WorkQueue();
		Search partial = new Search(invertedIndex, query, exact, queue);
		Map<String, List<Word>> foundQueries = partial.getResult();
		
		TreeMap<String, String> results = new TreeMap<>();
		Iterator<String> querie = foundQueries.keySet().iterator();
		while (querie.hasNext()) {
			String querieNext = querie.next();
			List<Word> listOfWords = foundQueries.get(querieNext);
			Iterator <Word> words = listOfWords.iterator();
			while(words.hasNext()) {
				Word wordNext = words.next();
				results.put(wordNext.getLocation(), null);
			}		
		}	
		
		return results;
	}
}