package threads;

import entity.UrlTuple;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import java.io.*;
import java.net.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mapdb.DB;

import driver.WebCrawlerDriver;

/**
 * This class defines the crawling threads. The crawling threads have a personal
 * stack of URL:s that they read through. There are 2 CTs associated with every
 * buffer.
 *
 * @since 2020-03-18
 *
 */
public class WebCrawler implements Runnable {
	
	// BUL shared between two crawling threads and one index building thread
	private final List<UrlTuple> URL_BUFFER;

	// capacity of the BUL
	private final int MAX_CAPACITY;

	// stack storing URLs for this crawling thread to use
	private volatile Stack<UrlTuple> TASK_STACK;
	
	// Indexed url tree
	private NavigableSet<String> IUT;
	
	//Database for indexed url tree
	private DB DB;

	public WebCrawler(final List<UrlTuple> sharedQueue, final Stack<UrlTuple> taskQueue, 
			NavigableSet<String> IUT, DB db, final int size) {
		this.URL_BUFFER = sharedQueue;
		this.TASK_STACK = taskQueue;
		this.MAX_CAPACITY = size;
		this.DB = db;
		this.IUT = IUT;
	}

	/**
	 * consumes URLs from it's associated stack and fetches their associated HTML.
	 * Pushes the links in the HMTL onto the stack. Finally it pushes the scraped
	 * webpage onto the shared buffer.
	 * 
	 */
	@Override
	public void run() {
		while (WebCrawlerDriver.TTL > System.nanoTime()) {
			try {
				if (!TASK_STACK.isEmpty()) {
					
					UrlTuple currentURL = TASK_STACK.pop();

					extractHtmlAndLinks(currentURL);
					
					if(currentURL == null) {
						continue; //necessary??
					}
					
					produce(currentURL);
				} else {
					System.out.println(Thread.currentThread().getName() + " has nothing to do!");
				}
			} catch (final InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		
		DB.commit();
		System.out.println("Thread finished!!!!!: " + Thread.currentThread().getName());
	}

	/**
	 * Goes to the webpage of the provided URL and fetches the HTML data. Looks
	 * through the HTML to find contained links and pushes these onto the stack
	 * associated with the thread.
	 * 
	 * @param url
	 * @return
	 */
	private void extractHtmlAndLinks( UrlTuple url) {
		// try to fetch the document
		Document doc = null;
		
		try {			
			doc = Jsoup.connect(url.getURL()).get();
		} catch (IOException e) {
			url.setDead();
			return;
		}
		
		//set the HTML as part of the object
		url.setHTML(doc.toString());

		// get the links from the document
		Elements links = doc.select("a[href]");
		
		
		HashSet<String> currentset = new HashSet<>();

		for (Element link : links) {
			currentset.add(link.attr("abs:href").toString());
		}
		
		ArrayList<String> ret = new ArrayList<String>();
		
		ret.addAll(currentset);
		
		url.setChildren(ret);
		
		//For each found URL we put s in the stack if it's not in IUT already.
		for(String s : ret) {
			if(!IUT.contains(s)) {
				TASK_STACK.add(new UrlTuple(url.getURL(), s));
			}
		}
	}
	

	/**
	 * Puts the webpage-tuple into the buffer. If the buffer is full the tread will
	 * have to wait until it's empty.
	 * 
	 * @param urlTuple
	 * @throws InterruptedException
	 */
	private void produce(final UrlTuple urlTuple) throws InterruptedException {
		synchronized (URL_BUFFER) {

			while (URL_BUFFER.size() == MAX_CAPACITY) {
				URL_BUFFER.wait();
			}

			URL_BUFFER.add(urlTuple);
			
			System.out.println("Produced a url-html pair by thread " + Thread.currentThread().getName());
			
			URL_BUFFER.notifyAll();

		}
	}
}