package threads;

import entity.*;

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

	public IndexBuilder(List<UrlTuple> sharedQueue, UrlTree urlIndex, int max_capacity, 
			final long timeToLive, final TimeUnit timeUnit, CyclicBarrier barrier) {
		this.URL_INDEX = urlIndex;
		this.URL_BUFFER = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
		this.TIME_TO_LIVE = System.nanoTime() + timeUnit.toNanos(timeToLive);
		this.BARRIER = barrier;
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
					URL_BUFFER.notifyAll();
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
				URL_INDEX.addURL(ut);;
			}
			

			if (TIME_TO_LIVE < System.nanoTime()) {
				URL_BUFFER.notifyAll();
				return;
			}
			URL_BUFFER.notifyAll();
		}
	}
}