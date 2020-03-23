package threads;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
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
	private final Stack<String> TASK_STACK;

	// how long the thread will live
	private final long TIME_TO_LIVE;

	// barrier to wait for the threads
	private final CyclicBarrier BARRIER;

	public WebCrawler(final List<UrlTuple> sharedQueue, final Stack<String> taskQueue, final int size,
			final long timeToLive, final TimeUnit timeUnit, CyclicBarrier barrier) {
		this.urlBuffer = sharedQueue;
		this.TASK_STACK = taskQueue;
		this.MAX_CAPACITY = size;
		this.TIME_TO_LIVE = System.nanoTime() + timeUnit.toNanos(timeToLive);
		this.BARRIER = barrier;
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
				if (!TASK_STACK.isEmpty()) {

					// get a URL to work with
					String nextURL = TASK_STACK.pop();

					ArrayList<String> urlsFound = extractHtmlAndLinks(nextURL);

					if (urlsFound == null)
						continue;

					// create a tuple to put in the BUL
					UrlTuple pair = new UrlTuple(nextURL, urlsFound);

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

		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
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

		final ArrayList<String> foundURLs = new ArrayList<>();

		while (m.find()) {
			final String found = m.group();
			TASK_STACK.push(found);
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

			if (TIME_TO_LIVE < System.nanoTime()) {
				urlBuffer.notifyAll();
				return;
			}

			urlBuffer.add(pair);
			System.out.println("Produced a url-html pair by thread " + Thread.currentThread().getName());

			urlBuffer.notifyAll();

		}
	}
}