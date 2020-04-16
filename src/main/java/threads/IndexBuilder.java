package threads;

import entity.*;
import driver.WebCrawlerDriver;

import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.mapdb.DB;

/**
 * This class defines the index building threads which consume elements from an
 * associated buffer (which is shared with other IBTs) and puts them into the
 * IUT.
 * 
 * @since 2020-03-18
 */
public class IndexBuilder implements Runnable {
	private final List<UrlTuple> URL_BUFFER;
	private final int MAX_CAPACITY;
	private final UrlTree URL_INDEX;
	private final long TIME_TO_LIVE;
	private final CyclicBarrier BARRIER;
	private NavigableSet<String> IUT;
	private DB DB;
	public static Integer htmlDocId = 0;
	private FileWriter reswriter;

	public IndexBuilder(List<UrlTuple> sharedQueue, UrlTree urlIndex,
			NavigableSet<String> IUT, DB db, int max_capacity, final long timeToLive, 
			final TimeUnit timeUnit, CyclicBarrier barrier, FileWriter fw) {
		this.URL_INDEX = urlIndex;
		this.URL_BUFFER = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
		this.TIME_TO_LIVE = System.nanoTime() + timeUnit.toNanos(timeToLive);
		this.BARRIER = barrier;
		this.DB = db;
		this.IUT = IUT;
		this.reswriter = fw;
	}
	
	/**
	 * This method starts the Index building thread. It's task is to consume objects
	 * from the buffer and insert them into the URLIndexTree
	 */
	@Override
	public void run() {
		while (TIME_TO_LIVE > System.nanoTime()) {
			try {
				consume();
			} catch (InterruptedException ex) {
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
	
	
	public void addURL(UrlTuple ut){
		String url = ut.getURL();
		String strippedUrl = url.replace("http://","").replace("https://","").replace("www.","");
		System.out.println(strippedUrl);

		try {
			for(String s : ut.getFoundUrls()) {
				reswriter.write(url + " ---> " + s + "\n");
			}
			
			FileWriter fw = new FileWriter("./indexfiles/" + strippedUrl.charAt(0) + "\n", true);
			FileWriter htmlw = new FileWriter("./htmls/" + htmlDocId.toString() + "\n");
			fw.write(url + " ---> " + " " + htmlDocId.toString() + "\n");
			htmlw.write(ut.GetHTML());
			htmlDocId++;
			
			htmlw.close();
			fw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method tries to consume a pair from the associated buffer (by consume we
	 * mean pop it off the stack and place it in the URLIndexTree). The method is
	 * synchronized in order to avoid multi thread access to the shared buffer
	 * 
	 * @question Why do we have 2 blocks both outputting empty? Can't these be
	 *           merged
	 */
	private void consume() throws InterruptedException {
		synchronized (URL_BUFFER) {
			while (URL_BUFFER.size() != MAX_CAPACITY) {
				if (TIME_TO_LIVE < System.nanoTime()) {
					//while(WebCrawlerDriver.ThreadsAreStillWaiting()) {
					URL_BUFFER.notifyAll();	
					//}
					System.out.println("Notified and returning thread: " + Thread.currentThread().getName());
					return;
				}
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ URL_BUFFER.size());
				URL_BUFFER.wait();
			}

			ArrayList<UrlTuple> copy = new ArrayList<>();
			copy.addAll(URL_BUFFER);
			URL_BUFFER.clear();
			
			for(UrlTuple ut : copy){
				addURL(ut);
				IUT.add(ut.getURL());
				
			}

			

			if (TIME_TO_LIVE < System.nanoTime()) {
				System.out.println("Before wait thread: " + Thread.currentThread().getName());
				URL_BUFFER.notifyAll();
				System.out.println("After wait thread: " + Thread.currentThread().getName());
				return;
			}
			URL_BUFFER.notifyAll();
		}
	}
}