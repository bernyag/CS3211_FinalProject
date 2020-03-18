package threads;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import entity.UrlHtmlTuple;

import java.io.IOException;


/** 
 * This class defines the crawling threads. The crawling threads have a personal stack
 * of URL:s that they read through. There are 2 CTs associated with every buffer.
 *
 * @since 2020-03-18
 *
 */
public class WebCrawler implements Runnable {
	// BUL shared between two crawling threads and one index building thread
	private final List<UrlHtmlTuple> urlBuffer;

	// capacity of the above list
	private final int MAX_CAPACITY;

	// stack storing URLs for this crawling thread to use
	private final Stack<String> taskStack;

	// TODO use the data structure of a buffer to limit the amount of arguments taken by the constructor
	public WebCrawler(List<UrlHtmlTuple> sharedQueue, Stack<String> taskQueue, int size) {
		this.urlBuffer = sharedQueue;
		this.taskStack = taskQueue;
		this.MAX_CAPACITY = size;
	}

	/**
	 * consumes URLs from it's associated stack and fetches their associated HTML. 
	 * Pushes the links in the HMTL onto the stack. Finally it pushes the scraped 
	 * webpage onto the shared buffer.  
	 *  
	 */
	@Override
	public void run() {
		while (true) {
			try {
				if (!taskStack.isEmpty()) {
					// get a URL to work with
					String nextURL = taskStack.pop();
					//System.out.println(nextURL);
					
					// get html from this link
					// TODO could we run into errors when we try to extract the link? In that case we need exception handlings
					String html = extractHtmlAndLinks(nextURL);
					
					// if jsoup failed, carry on to the next link
					if(html == null) continue;
					
					// create a tuple to put in the BUL
					UrlHtmlTuple pair = new UrlHtmlTuple(nextURL, html);
					
					// put the object into the BUL
					produce(pair);
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Goes to the webpage of  the provided URL and fetches the HTML data. Looks 
	 * through the HTML to find contained links and pushes these onto the stack
	 * associated with the thread. 
	 * 
	 * @param url
	 * @return
	 */
	private String extractHtmlAndLinks(String url) {		
		// try to fetch the document
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();  //TODO use native Java for fetching to avoid .JAR file 
		} catch (IOException e) {
			return null;
		}

		// parse HTML into a string
		String html = doc.toString();

		// get the links from the website
		Elements links = doc.select("a[href]");

		// push these links onto the crawlers associated stack 
		for (Element link : links) {
			taskStack.push(link.attr("abs:href").toString());
		}
		
		// return string representation of html
		return html;
	}


	/** 
	 * Puts the webpage-tuple into the buffer. If the buffer is full the 
	 * tread will have to wait until it's empty. 
	 *  
	 * @param pair
	 * @throws InterruptedException
	 */
	private void produce(UrlHtmlTuple pair) throws InterruptedException {
		synchronized (urlBuffer) {

			// check the blocking condition
			while (urlBuffer.size() == MAX_CAPACITY) {
				//System.out.println("Queue is full " + Thread.currentThread().getName() + " is waiting , size: " + urlBuffer.size());
				urlBuffer.wait();
			}
			
			// TODO don't we need to syncronize this operation? We could potentially add 2elements to buffer at the same time
			Thread.sleep(1000);
			urlBuffer.add(pair);
			System.out.println("Produced a url-html pair by thread " + Thread.currentThread().getName());

			// check blocking condition again
			while (urlBuffer.size() == MAX_CAPACITY) {
				//System.out.println("Queue is full " + Thread.currentThread().getName() + " is waiting , size: " + urlBuffer.size());
				urlBuffer.notifyAll();
				urlBuffer.wait();
			}

			urlBuffer.notifyAll();

		}
	}
}