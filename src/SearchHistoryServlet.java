import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SearchHistoryServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

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
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession();
		String[] names = session.getValueNames();
		prepareResponse("Search History", response);
		out.println("<h1> Search History </h1>");
		out.println("<p>");
		if (session != null) {
			for(int i = 0; i < names.length; i++) {
				String outputString = "Query: \"" + names[i] + "\" time: " + session.getAttribute(names[1]) + "<br>" ;
				out.println(outputString);
			}
		}
		out.println("</p>");
		out.println("<form method=\"POST\" action=\"/clean\"/>\n" + 
		 		"		 <input type = \"submit\" value = \"Clean History\">\n" + 
		 		"		 </form>");
		finishResponse(request, response);

	}
	
}
