package threads;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import entity.UrlTuple;

import java.io.*;
import java.net.*;

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
	private final Stack<String> taskStack;

	private final long timeToLive;

	// TODO use the data structure of a buffer to limit the amount of arguments
	// taken by the constructor
	public WebCrawler(final List<UrlTuple> sharedQueue, final Stack<String> taskQueue, final int size, final long timeToLive,
			final TimeUnit timeUnit) {
		this.urlBuffer = sharedQueue;
		this.taskStack = taskQueue;
		this.MAX_CAPACITY = size;
		this.timeToLive = System.nanoTime() + timeUnit.toNanos(timeToLive);
	}

	/**
	 * consumes URLs from it's associated stack and fetches their associated HTML.
	 * Pushes the links in the HMTL onto the stack. Finally it pushes the scraped
	 * webpage onto the shared buffer.
	 * 
	 */
	@Override
	public void run() {
		while (timeToLive > System.nanoTime()) {
			try {
				if (!taskStack.isEmpty()) {
					
					// get a URL to work with

					String nextURL = taskStack.pop();
					// System.out.println(nextURL);

					// get html from this link
					// TODO could we run into errors when we try to extract the link? In that case
					// we need exception handlings
					if(Thread.currentThread().getName().equals("Thread-4")){
						System.out.println("THREAD 4 before");
					}
					ArrayList<String> urlsFound = extractHtmlAndLinks(nextURL);
					if(Thread.currentThread().getName().equals("Thread-4")){
						System.out.println("THREAD 4 after");
					}
					// if jsoup failed, carry on to the next link
					if (urlsFound == null)
						continue;

					// create a tuple to put in the BUL
					UrlTuple pair = new UrlTuple(nextURL, urlsFound);

					// put the object into the BUL
					produce(pair);
				}
			} catch (final InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Thread " + Thread.currentThread().getName() + " has finished!");

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
			// TODO Auto-generated catch block
		}

		InputStream is = null;
		try {
			is = (InputStream) url.getContent();
		} catch (final Exception e) {
			System.out.println("got classcastexeption " + Thread.currentThread().getName());
			return null;
		}

		if(Thread.currentThread().getName().equals("Thread-4")){
			System.out.println("THREAD 4 -------- AFTER INPUT STREAM");
		}

		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		final StringBuffer sb = new StringBuffer();
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (final Exception e) {
			System.out.println("Exception 2");
		}

		final String html = sb.toString();

		final Pattern pattern = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		final Matcher m = pattern.matcher(html);

		final ArrayList<String> foundURLs = new ArrayList<>();

		while (m.find()) {
			final String found = m.group();
			taskStack.push(found);
			foundURLs.add(found);
		}

		return foundURLs;
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
				urlBuffer.wait();
			}

			if (timeToLive < System.nanoTime()) {
				urlBuffer.notifyAll();
				return;
			}

			urlBuffer.add(pair);
			System.out.println("Produced a url-html pair by thread " + Thread.currentThread().getName());

			urlBuffer.notifyAll();

		}
	}
}