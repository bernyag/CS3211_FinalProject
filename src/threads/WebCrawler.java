package threads;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import entity.UrlHtmlTuple;

import java.io.IOException;

// This will be our crawling thread

public class WebCrawler implements Runnable {
	// BUL shared between two crawling threads and one index building thread
	private final List<UrlHtmlTuple> urlBuffer;

	// capacity of the above list
	private final int MAX_CAPACITY;

	// stack storing URLs for this crawling thread to use
	private final Stack<String> taskStack;

	
	public WebCrawler(List<UrlHtmlTuple> sharedQueue, Stack<String> taskQueue, int size) {
		this.urlBuffer = sharedQueue;
		this.taskStack = taskQueue;
		this.MAX_CAPACITY = size;
	}
	
	private String extractHtmlAndLinks(String url) {		
		// try to fetch the document
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			return null;
		}

		// parse html into a string
		String html = doc.toString();

		// get the links from the website
		Elements links = doc.select("a[href]");

		// push these links onto a stack
		for (Element link : links) {
			taskStack.push(link.attr("abs:href").toString());
		}
		
		// return string representation of html
		return html;
	}


	@Override
	public void run() {
		while (true) {
			try {
				if (!taskStack.isEmpty()) {
					// get a URL to work with
					String nextURL = taskStack.pop();
					//System.out.println(nextURL);
					
					// get html from this link
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

	private void produce(UrlHtmlTuple pair) throws InterruptedException {
		synchronized (urlBuffer) {

			// check the blocking condition
			while (urlBuffer.size() == MAX_CAPACITY) {
				//System.out.println("Queue is full " + Thread.currentThread().getName() + " is waiting , size: " + urlBuffer.size());
				urlBuffer.wait();
			}
			
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