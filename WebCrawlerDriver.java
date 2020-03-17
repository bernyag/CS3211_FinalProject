import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;

public class WebCrawlerDriver {

	// number of building threads
	private static final int NO_OF_BUFFERS = 3;

	// maximum capacity of the BUL
	private static final int MAX_CAPACITY = 10;

	// number of crawling threads
	private static final int NO_OF_CRAWLERS = 6;

	// number of building threads
	private static final int NO_OF_BUILDERS = 3;

	public static void main(String[] args) {

		// IUT data structure
		UrlTree index = new UrlTree();

		// create the buffers
		ArrayList<ArrayList<UrlHtmlTuple>> buffers = new ArrayList<>();
		for (int i = 0; i < NO_OF_BUFFERS; i++) {
			buffers.add(new ArrayList<UrlHtmlTuple>());
		}

		// split the docs of url seeds
		ArrayList<ArrayList<String>> seeds = new ArrayList<>();
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			seeds.add(new ArrayList<String>());
		}

		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("D:\\EclipseProjects\\Web Crawler\\bin\\url-example.txt"));
			String line = reader.readLine();
			int line_no = 0;
			while (line != null) {
				// get the right seed list
				ArrayList<String> list = seeds.get(line_no % NO_OF_CRAWLERS);
				// read next line
				line = reader.readLine();
				list.add(line);
				line_no++;
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create the crawlers
		Thread[] crawlers = new Thread[NO_OF_CRAWLERS];
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			ArrayList<UrlHtmlTuple> buffer = buffers.get(i / 2);
			Stack<String> taskStack = new Stack<>();
			ArrayList<String> intialURLs = seeds.get(i);
			for (int j = 0; j < intialURLs.size(); j++) {
				//System.out.println(intialURLs.get(j));
				taskStack.push(intialURLs.get(j));
			}
			crawlers[i] = new Thread(new WebCrawler(buffer, taskStack, MAX_CAPACITY));
			crawlers[i].start();
		}

		// create the builders
		Thread[] builders = new Thread[NO_OF_BUILDERS];
		for (int i = 0; i < NO_OF_BUILDERS; i++) {
			ArrayList<UrlHtmlTuple> buffer = buffers.get(i);
			builders[i] = new Thread(new IndexBuilder(buffer, index, MAX_CAPACITY));
			builders[i].start();

		}

	}
}
