package threads;

import entity.*;
import driver.WebCrawlerDriver;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.mapdb.DB;

/**
 * This class defines the index building threads which consume elements from an
 * associated buffer (which is shared with other IBTs) and puts them into the
 * IUT.
 * 
 * @since 2020-03-18
 */
public class IndexBuilder implements Runnable {
	
	// BUL shared between two crawling threads and one index building thread
	private final List<UrlTuple> URL_BUFFER;
	
	// Capacity of the BUL
	private final int MAX_CAPACITY;
	
	
	// Indexed url tree
	private NavigableSet<String> IUT;
	
	//Database for indexed url tree
	private DB DB;
	
	//Sets the document ID for the htmls documents being saved, but also counts the amount of files crawled
	public static Integer htmlDocId = 0;
	
	//Writer to write the result file
	private FileWriter reswriter;
	
	//Counts inputs to the database so we can commit at a reasonable time.
	private static int inputsCounter = 0;

	public IndexBuilder(List<UrlTuple> sharedQueue, NavigableSet<String> IUT, 
			DB db, int max_capacity, FileWriter fw) {
		this.URL_BUFFER = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
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
		DB.commit();
	}
	
	/**
	 * This method writes the results to files.
	 */
	public void writeURL(UrlTuple ut){
		try {	
			
			if(ut.dead()) {
				reswriter.write( ut.getURL() + " ---> " + ut.getParent() + " : *dead-url* \n");
			} else if(htmlDocId > 1000) {
				reswriter.write(ut.getURL() + " ---> " + ut.getParent() + " : *ignored* \n");
			} else {
				String fileloc = "./htmls/" + htmlDocId.toString() + ".html";
				FileWriter htmlw = new FileWriter(fileloc);
				htmlw.write(ut.getHTML());
				reswriter.write( ut.getURL() + " ---> " + ut.getParent() + " : " + new File(fileloc).getAbsolutePath() + "\n");
				htmlw.close();
				htmlDocId++;
			}
			

			
			
	
		} catch (Exception e) {}
	}

	/**
	 * This method tries to consume a pair from the associated buffer (by consume we
	 * mean pop it off the stack and place it in the URLIndexTree). The method is
	 * synchronized in order to avoid multi thread access to the shared buffer
	 */
	private void consume() throws InterruptedException {
		synchronized (URL_BUFFER) {		
			
			//Wait for BUL to be full
			while (URL_BUFFER.size() != MAX_CAPACITY && WebCrawlerDriver.TTL > System.nanoTime()) {
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ URL_BUFFER.size());
				try{
					URL_BUFFER.wait();
				} catch(InterruptedException e) {}
			}
			
			//copy and clear buffer
			ArrayList<UrlTuple> copy = new ArrayList<>();
			copy.addAll(URL_BUFFER);
			URL_BUFFER.clear();
			
			//Write the urls to files
			for(UrlTuple ut : copy){
				writeURL(ut);
				IUT.add(ut.getURL());
				inputsCounter++;
			}
			
			//After 100000 url's we commit to DB to not exceed the memory limit
			if(inputsCounter > 100000) {
				DB.commit();
				inputsCounter = 0;
			}

			URL_BUFFER.notifyAll();
		}
	}
}