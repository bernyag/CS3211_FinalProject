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
	private final List<UrlTuple> urlBuffer;

	// capacity of the above list
	private final int MAX_CAPACITY;

	// stack storing URLs for this crawling thread to use
	private volatile Stack<String> TASK_STACK;

	// how long the thread will live
	private final long TIME_TO_LIVE;

	// barrier to wait for the threads
	private final CyclicBarrier BARRIER;
	
	public static HashSet<String> seenurls = new HashSet<>();
	
	private NavigableSet<String> IUT;
	private DB DB;

	BufferedReader br;

	public WebCrawler(final List<UrlTuple> sharedQueue, final Stack<String> taskQueue, 
			NavigableSet<String> IUT, DB db, final int size,
			final long timeToLive, final TimeUnit timeUnit, CyclicBarrier barrier) {
		this.urlBuffer = sharedQueue;
		this.TASK_STACK = taskQueue;
		this.MAX_CAPACITY = size;
		this.TIME_TO_LIVE = System.nanoTime() + timeUnit.toNanos(timeToLive);
		this.BARRIER = barrier;
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
		while (TIME_TO_LIVE > System.nanoTime()) {
			try {
				System.out.println("SIZE OF STACK: -----> " + TASK_STACK.size());
				if (!TASK_STACK.isEmpty()) {
					// get a URL to work with
					String nextURL = TASK_STACK.pop();
					System.out.println(TASK_STACK.size());
					ArrayList<String> urlsFound = jsoupExtractHtmlAndLinks(nextURL);
					if (urlsFound == null)
						continue;

					String html = urlsFound.remove(urlsFound.size()-1);
		
					// create a tuple to put in the BUL
					UrlTuple pair = new UrlTuple(nextURL, html, urlsFound);
					
					System.out.println("After jsoup size: " + urlsFound.size());
					// put the object into the BUL
					produce(pair);
				} else {
					System.out.println(Thread.currentThread().getName() + " has nothing to do!");
				}
			} catch (final InterruptedException ex) {
				ex.printStackTrace();
			}
		}

		try {
			BARRIER.await();
			//br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(Thread.currentThread().getName() + " has finished!");

	}

	/**
	 * Goes to the webpage of the provided URL and fetches the HTML data. Looks
	 * through the HTML to find contained links and pushes these onto the stack
	 * associated with the thread.
	 * 
	 * @param url
	 * @return
	 */
	private ArrayList<String> extractHtmlAndLinks(final String urlstring) {
		URL url = null;
		try {
			url = new URL(urlstring);
		} catch (final MalformedURLException e) {
			// Do nothing
		}

		URLConnection con = null;
		try {
			con = url.openConnection();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		con.setConnectTimeout(10000);
		con.setReadTimeout(10000);

		InputStream is = null;
		try {
			is = con.getInputStream();
		} catch (final Exception e) {
			return null;
		}

		br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		final StringBuffer sb = new StringBuffer();
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (final Exception e) {
			// Do nothing
		}

		final String html = sb.toString();

		final Pattern pattern = Pattern
				.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		final Matcher m = pattern.matcher(html);

		final HashSet<String> foundURLs = new HashSet<>();
		while (m.find()) {
			final String found = m.group();
			foundURLs.add(found);
		}

		System.out.println("URL STRING: " +  urlstring);
		for( String e : foundURLs){
			//System.out.println(urlstring + "------>" + e);
		}

		for(String s : foundURLs){
			if(!seenurls.contains(s)){
				TASK_STACK.add(s);
				seenurls.add(s);
			}
		}
		

		ArrayList<String> ret = new ArrayList<>();
		ret.addAll(foundURLs);
		ret.add(html);

		return ret;
	}
	
	private ArrayList<String> jsoupExtractHtmlAndLinks(final String urlstring) {
		// try to fetch the document
		Document doc = null;
		try {			
			doc = Jsoup.connect(urlstring).get();
		} catch (IOException e) {
			return null;
		}
		
		// parse HTML into a string
		String html = doc.toString();

		// get the links from the website
		Elements links = doc.select("a[href]");
		
		
		HashSet<String> currentset = new HashSet<>();

		// push these links onto the crawlers associated stack 
		for (Element link : links) {
			currentset.add(link.attr("abs:href").toString());
		}
		
		ArrayList<String> ret = new ArrayList<String>();
		
		ret.addAll(currentset);
		
		//seenurls.add(urlstring);
		
		for(String s : ret) {
			if(!IUT.contains(s)) {
				TASK_STACK.add(s);
			}
		}
		
		ret.add(html);
		
		// return string representation of html
		return ret;
	}
	

	/**
	 * Puts the webpage-tuple into the buffer. If the buffer is full the tread will
	 * have to wait until it's empty.
	 * 
	 * @param pair
	 * @throws InterruptedException
	 */
	private void produce(final UrlTuple pair) throws InterruptedException {
		synchronized (urlBuffer) {

			// check the blocking condition
			while (urlBuffer.size() == MAX_CAPACITY) {
				System.out.println("Before wait thread: " + Thread.currentThread().getName());
				urlBuffer.wait();
				System.out.println("After wait thread: " + Thread.currentThread().getName());
			}

			if (TIME_TO_LIVE < System.nanoTime()) {
				//while(WebCrawlerDriver.ThreadsAreStillWaiting()) {
				urlBuffer.notifyAll();	
				//}
				System.out.println("Notified and returning thread: " + Thread.currentThread().getName());
				return;
			}
			
			System.out.println("urlbuf size " + urlBuffer.size());

			urlBuffer.add(pair);

			System.out.println("Produced a url-html pair by thread " + Thread.currentThread().getName());

			urlBuffer.notifyAll();

		}
	}
}