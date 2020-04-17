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
	private static int MAX_TIMEOUT = 300;

	// barrier to wait for all the threads
	private static CyclicBarrier BARRIER = new CyclicBarrier(NO_OF_CRAWLERS + NO_OF_BUILDERS);
	
	private static Thread[] crawlers;
	private static Thread[] builders;
	public static long TTL;
	public static String inputfile;
	public static String outputfile;


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
	
	private static boolean checkArguments(String[] args) {
		
		//java cawler.jar -time 24h -input seed.txt -output res.txt -storedPageNum 1000
		
		for(int i = 0; i < args.length; i++) {
			
			String argument = args[i];
			
			if (argument.equals("-time")) {
			    if (i+1 < args.length) {
			    	MAX_TIMEOUT = Integer.parseInt(args[i++]);
			    	continue;
			    }
			    else {
			    	System.err.println("-time requires a number");			    	
			    }
			}
			
			if (argument.equals("-input")) {
			    if (i+1 < args.length) {
			        outputfile = args[i++];
			        continue;
			    }
			    else {
			    	System.err.println("-inputrequires a filename");			    	
			    }
			}
			
			if (argument.equals("-output")) {
			    if (i+1 < args.length) {
			        outputfile = args[i++];
					continue;
			    }
			    else {
			    	System.err.println("-output requires a filename");			    	
			    }
			}
			
			if (argument.equals("-storedPageNum")) {
			    if (i+1 < args.length) {
			        outputfile = args[i++];
			        continue;
			    }
			    else {
			    	System.err.println("-storedPageNum requires a number");			    	
			    }
			}
			return false;
		}
		
		
		return true;
	}
	
	

	public static void main(String[] args) throws IOException {
		
		/*if(!checkArguments(args)) {
			System.err.println("Usage: java cawler.jar -time 24h -input seed.txt -output res.txt -storedPageNum 1000");	
		}*/
		
		FileUtils.deleteDirectory(new File("./htmls"));
		new File("./htmls").mkdirs();

		
		try {
			FileUtils.forceDelete(new File("./IUTDB"));
			FileUtils.forceDelete(new File("./res"));
		}catch(Exception e) {
			
		}
		
		
		FileWriter reswriter = new FileWriter("res", true);
		

		
		DB db = DBMaker.fileDB("IUTDB").make();
		NavigableSet<String> IUT = db.treeSet("example").serializer(Serializer.STRING).createOrOpen();
		

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
		
		TTL  = System.nanoTime() + TimeUnit.SECONDS.toNanos(MAX_TIMEOUT);

		// create the crawlers
		crawlers = new Thread[NO_OF_CRAWLERS];
		for (int i = 0; i < NO_OF_CRAWLERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i / 2);
			Stack<UrlTuple> taskStack = new Stack<>();
			ArrayList<String> intialURLs = seeds.get(i);
			for (int j = 0; j < intialURLs.size(); j++) {
				taskStack.add(new UrlTuple("root", intialURLs.get(j)));
			}

			crawlers[i] = new Thread(
					new WebCrawler(buffer, taskStack, IUT, db, MAX_CAPACITY, BARRIER));
			crawlers[i].start();
		}

		// create the builders
		builders = new Thread[NO_OF_BUILDERS];
		for (int i = 0; i < NO_OF_BUILDERS; i++) {
			ArrayList<UrlTuple> buffer = buffers.get(i);
			builders[i] = new Thread(
					new IndexBuilder(buffer, IUT, db, MAX_CAPACITY, BARRIER, reswriter));
			builders[i].start();

		}
		
		while(TTL+1000 > System.nanoTime()) {}
		
		try {
			for(int i = 0; i < crawlers.length; i++) {
				crawlers[i].interrupt();
				//crawlers[i].join();
			}
			for(int i = 0; i < builders.length; i++) {
				builders[i].interrupt();
				//builders[i].join();
			}
		} catch (Exception e) {}
		

		// Print the content of the index to the console
		// TODO gotta change it to writing to a file
		System.out.println("Total number of URLs: " + IndexBuilder.htmlDocId.toString());
		//System.out.println(getResultString(index.getResult()));
	}
}
