import java.io.BufferedReader;

import threads.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import entity.UrlTuple;
import entity.UrlTree;

import java.util.*;

public class WebCrawlerDriver {

	// number of building threads
	private static final int NO_OF_BUFFERS = 3;

	// maximum capacity of the BUL
	private static final int MAX_CAPACITY = 1000;

	// number of crawling threads
	private static final int NO_OF_CRAWLERS = 6;

	// number of building threads
	private static final int NO_OF_BUILDERS = 3;

	// timeout in nanoseconds
	private static final long MAX_TIMEOUT = 3600L;

	// barrier to wait for all the threads
	private static CyclicBarrier BARRIER = new CyclicBarrier(NO_OF_CRAWLERS + NO_OF_BUILDERS);

	private static String getResultString(Map<String, List<String>> indexContent) {
		String result = "";
		int n = 0;
		for (String key : indexContent.keySet()) {
			String from = key + " -> ";
			List<String> foundURLs = indexContent.get(key);
			for (String url : foundURLs) {
				result += from + url + "\n";
				n++;
			}
		}
		return result + "Total number of URLs: " + n;
	}

	public static void main(String[] args) throws IOException {

		// IUT data structure
		UrlTree index = new UrlTree();

		// create the buffers
		ArrayList<ArrayList<UrlTuple>> buffers = new ArrayList<>();
		for (int i = 0; i < NO_OF_BUFFERS; i++) {
			buffers.add(new ArrayList<UrlTuple>());
		}

		// split the docs of url seeds
		ArrayList<ArrayList<String>> seeds = new ArrayList<>();
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			seeds.add(new ArrayList<String>());
		}

		BufferedReader reader;

		try {
			reader = new BufferedReader(new FileReader("url-example.txt"));
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
			ArrayList<UrlTuple> buffer = buffers.get(i / 2);
			Stack<String> taskStack = new Stack<>();
			ArrayList<String> intialURLs = seeds.get(i);
			for (int j = 0; j < intialURLs.size(); j++) {
				// System.out.println(intialURLs.get(j));
				taskStack.push(intialURLs.get(j));
			}
			crawlers[i] = new Thread(
					new WebCrawler(buffer, taskStack, MAX_CAPACITY, MAX_TIMEOUT, TimeUnit.SECONDS, BARRIER));
			crawlers[i].start();
		}

		// create the builders
		Thread[] builders = new Thread[NO_OF_BUILDERS];
		for (int i = 0; i < NO_OF_BUILDERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i);
			builders[i] = new Thread(
					new IndexBuilder(buffer, index, MAX_CAPACITY, MAX_TIMEOUT, TimeUnit.SECONDS, BARRIER));
			builders[i].start();

		}

		try {
			for (int i = 0; i < NO_OF_CRAWLERS; i++) {
				crawlers[i].join();
			}
			for (int i = 0; i < NO_OF_BUILDERS; i++) {
				builders[i].join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Print the content of the index to the console
		// TODO gotta change it to writing to a file
		System.out.println(getResultString(index.getResult()));
	}
}
