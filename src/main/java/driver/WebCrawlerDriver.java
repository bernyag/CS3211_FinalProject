package driver;

import java.io.BufferedReader;
import java.io.File;

import entity.*;
import threads.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

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

	// timeout in nanoseconds
	private static final long MAX_TIMEOUT = 300;

	// barrier to wait for all the threads
	private static CyclicBarrier BARRIER = new CyclicBarrier(NO_OF_CRAWLERS + NO_OF_BUILDERS);
	
	private static Thread[] crawlers;
	private static Thread[] builders;


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
	
	public static boolean ThreadsAreStillWaiting() {
		for(int i = 0; i < crawlers.length; i++) {
			if(crawlers[i].getState() == State.WAITING) {
				return true;
			}
		}
		
		for(int j = 0; j < builders.length; j++) {
			if(builders[j].getState() == State.WAITING) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) throws IOException {
		
		FileUtils.deleteDirectory(new File("./indexfiles"));
		FileUtils.deleteDirectory(new File("./htmls"));
		
		FileWriter reswriter = new FileWriter("res", true);
		
		
		try {
			FileUtils.forceDelete(new File("./IUTDB"));
		}catch(Exception e) {
			
		}

		
		DB db = DBMaker.fileDB("IUTDB").make();
		NavigableSet<String> IUT = db.treeSet("example").serializer(Serializer.STRING).createOrOpen();
		
		// IUT data structure
		UrlTree index = new UrlTree();
		UrlTree tre = index;
		

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
			reader = new BufferedReader(new FileReader("url.txt"));
			String line = reader.readLine();
			System.out.println(line);
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
		crawlers = new Thread[NO_OF_CRAWLERS];
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i / 2);
			Stack<String> taskStack = new Stack<>();
			ArrayList<String> intialURLs = seeds.get(i);
			for (int j = 0; j < intialURLs.size(); j++) {
				taskStack.add(intialURLs.get(j));
			}

			crawlers[i] = new Thread(
					new WebCrawler(buffer, taskStack, IUT, db, MAX_CAPACITY, MAX_TIMEOUT, TimeUnit.SECONDS, BARRIER));
			crawlers[i].start();
		}

		// create the builders
		builders = new Thread[NO_OF_BUILDERS];
		for (int i = 0; i < NO_OF_BUILDERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i);
			builders[i] = new Thread(
					new IndexBuilder(buffer, index, IUT, db, MAX_CAPACITY, MAX_TIMEOUT, TimeUnit.SECONDS, BARRIER, reswriter));
			builders[i].start();

		}
		
		
		long ttl = System.nanoTime() + TimeUnit.SECONDS.toNanos(MAX_TIMEOUT);
		
		//while(ttl > System.nanoTime()) {}
		
		try {
			for(int i = 0; i < crawlers.length; i++) {
				crawlers[i].join();
			}
			for(int i = 0; i < builders.length; i++) {
				builders[i].join();
			}
		} catch (Exception e) {}
		

		// Print the content of the index to the console
		// TODO gotta change it to writing to a file
		System.out.println("Total number of URLs: " + IndexBuilder.htmlDocId.toString());
		//System.out.println(getResultString(index.getResult()));
	}
}
