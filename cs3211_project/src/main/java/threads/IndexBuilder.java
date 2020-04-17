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
	private final CyclicBarrier BARRIER;
	private NavigableSet<String> IUT;
	private DB DB;
	public static Integer htmlDocId = 0;
	private FileWriter reswriter;

	public IndexBuilder(List<UrlTuple> sharedQueue, UrlTree urlIndex, NavigableSet<String> IUT, 
			DB db, int max_capacity, CyclicBarrier barrier, FileWriter fw) {
		this.URL_INDEX = urlIndex;
		this.URL_BUFFER = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
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
	public void run(){
		while (WebCrawlerDriver.TTL > System.nanoTime()) {
			try {
				consume();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Thread finished!!!!!: " + Thread.currentThread().getName());
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
			System.out.println("Before await thread: " + Thread.currentThread().getName());
			while (URL_BUFFER.size() != MAX_CAPACITY && WebCrawlerDriver.TTL > System.nanoTime()) {
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ URL_BUFFER.size());
				System.out.println("Before await " + Thread.currentThread().getName());
				try{
					URL_BUFFER.wait();
				} catch(InterruptedException e) {
					
				}
		
			}
			System.out.println("Before copy " + Thread.currentThread().getName());
			ArrayList<UrlTuple> copy = new ArrayList<>();
			copy.addAll(URL_BUFFER);
			URL_BUFFER.clear();
			
			for(UrlTuple ut : copy){
				addURL(ut);
				IUT.add(ut.getURL());
				
			}

			System.out.println("before nano " + Thread.currentThread().getName());


			URL_BUFFER.notifyAll();
		}
	}
}