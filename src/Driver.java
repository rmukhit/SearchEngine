import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
/*
 * Driver Class from where we run the whole program
 */
public class Driver {

	/** 
	 * Main function that takes all the parameters, adds them to TreeMap and then runs the whole program.
	 *
	 * @param args
	 * @void
	 */
	public static void main(String[] args) {

		try {
			ArgumentMap arg = new ArgumentMap(args);

			// if there is "-path" flag then
			if (arg.containsKey("-path")) {
				// search files from the value of "-path"
				File dir = new File(arg.get("-path"));
				
				InvertedIndex invertedIndex;
				
				WorkQueue queue;
				if (arg.containsKey("-threads")) {
					if (Integer.parseInt(arg.get("-threads")) <= 0) {
						queue = new WorkQueue();
					} else {
						queue = new WorkQueue(Integer.parseInt(arg.get("-threads")));
					}				
					invertedIndex = new InvertedIndex(dir, queue);
				} else {
					// if there are no -threads flag, then program runs just on 1 thread
					queue = new WorkQueue(1);
					invertedIndex = new InvertedIndex(dir, queue);
				}

				// if there is no "-index" then don't create JSON file
				if (arg.containsKey("-index")) {
					// if there's any value of "-index" then create JSON file with given name
					if (arg.get("-index") != null) {	
						Json.writeJson(invertedIndex, arg.get("-index"));
						// if the value of "-index" is equal to null, then create index.json file
					} else {
						Json.writeJson(invertedIndex, "index.json");
					} 	
				}

				if (arg.containsKey("-query")) {
					if (arg.get("-query") != null) {
						// search for query txt files
						File queryDirectory = new File(arg.get("-query"));

						Search foundQueries;
						if (arg.containsKey("-threads")) {
							if (Integer.parseInt(arg.get("-threads")) <= 0) {
								queue = new WorkQueue();
							} else {
								queue = new WorkQueue(Integer.parseInt(arg.get("-threads")));
							}				
							if (arg.containsKey("-exact")) {
								foundQueries = new Search(invertedIndex, queue, queryDirectory, true);
							} else {
								foundQueries = new Search(invertedIndex, queue, queryDirectory, false);
							}	
						} else {
							// if there are no -threads flag, then program runs just on 1 thread
							queue = new WorkQueue(1);
							if (arg.containsKey("-exact")) {
								foundQueries = new Search(invertedIndex, queue, queryDirectory, true);
							} else {
								foundQueries = new Search(invertedIndex, queue, queryDirectory, false);
							}
						}

						// create Json file that stores all the found words, their position, location and frequency
						if (arg.containsKey("-results")) {
							if (arg.get("-results") != null) {
								Json.writeQueryJson(foundQueries, arg.get("-results"));
							} else {
								Json.writeQueryJson(foundQueries, "results.json");
							}
						}
					}
				}
			} else if (arg.containsKey("-url")) {

				String url = arg.get("-url");

				int limit = 0;
				if (arg.containsKey("-limit")) {
					limit = Integer.parseInt(arg.get("-limit"));
				} else {
					limit = 50;
				}

				InvertedIndex invertedIndex = new InvertedIndex(url, limit);

				// if there is no "-index" then don't create JSON file
				if (arg.containsKey("-index")) {
					// if there's any value of "-index" then create JSON file with given name
					if (arg.get("-index") != null) {	
						Json.writeJson(invertedIndex, arg.get("-index"));
						// if the value of "-index" is equal to null, then create index.json file
					} else {
						Json.writeJson(invertedIndex, "index.json");
					} 	
				}

				if (arg.containsKey("-query")) {
					if (arg.get("-query") != null) {
						// search for query txt files
						File queryDirectory = new File(arg.get("-query"));

						Search foundQueries;
						WorkQueue queue = new WorkQueue();			
						if (arg.containsKey("-exact")) {
							foundQueries = new Search(invertedIndex, queue, queryDirectory, true);
						} else {
							foundQueries = new Search(invertedIndex, queue, queryDirectory, false);
						}	


						// create Json file that stores all the found words, their position, location and frequency
						if (arg.containsKey("-results")) {
							if (arg.get("-results") != null) {
								Json.writeQueryJson(foundQueries, arg.get("-results"));
							} else {
								Json.writeQueryJson(foundQueries, "results.json");
							}
						}
					}

				}

				int port = 0;
				if(arg.containsKey("-port")) {
					port = Integer.parseInt(arg.get("-port"));
				} else {
					port = 8080;
				}

				Server server = new Server(port);
				// HTTP connector
				ServerConnector http = new ServerConnector(server);
				http.setHost("localhost");
				http.setPort(port);
				http.setIdleTimeout(30000);
			
				ResourceHandler resource_handler = new ResourceHandler();
				resource_handler.setDirectoriesListed(true);
				resource_handler.setWelcomeFiles(new String[]{ "index.html" });
				resource_handler.setResourceBase(".");

				HandlerList handlers = new HandlerList();

				SearchServlet searchServlet = new SearchServlet(invertedIndex);
				ServletHolder servletHolder = new ServletHolder(searchServlet);
				SearchHistoryServlet searchHistoryServlet = new SearchHistoryServlet();
				ServletHolder historyServletHolder = new ServletHolder(searchHistoryServlet);
				CleanServlet cleanServlet = new CleanServlet();
				ServletHolder cleanServletHolder = new ServletHolder(cleanServlet);
				
				ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
				servletContext.setContextPath("/");
				servletContext.addServlet(servletHolder, "/");
				servletContext.addServlet(historyServletHolder, "/history");
				servletContext.addServlet(cleanServletHolder, "/clean");

				handlers.setHandlers(new Handler[] {resource_handler, servletContext});
				server.setHandler(handlers);

				server.start();
				server.join();


				//if there is no "-path" flag then we should create empty JSON file if we have "-index"
				// or create nothing if there is no "-index" flag
			} else {
				if (arg.containsKey("-index")) {	
					// if we have "-index" key, then we should check its value to know where to create empty JSON file
					File fileDir;
					if ((arg.get("-index") != null)) {
						fileDir = new File(arg.get("-index"));
					} else {
						fileDir = new File("index.json");
					}
					Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDir), "UTF8"));
					out.append("");
					out.close();	
				}

				// it doesn't matter is there any -query files if there is no -path from where we create invertedIndex
				// then we just need to know is there -result flag so that we create empty Json file
				if (arg.containsKey("-results")) {
					File fileDir;
					if (arg.get("-results") != null) {
						fileDir = new File(arg.get("-results"));
					} else {
						fileDir = new File("results.json");
					}
					Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileDir), "UTF8"));
					out.append("");
					out.close();	
				}			
			}
		} catch (Exception e) {

		}	
	}

}
