package threads;

import java.util.*;
import java.util.concurrent.TimeUnit;

import entity.*;

/**
 * This class defines the index building threads which consume elements from an
 * associated buffer (which is shared with other IBTs) and puts them into the
 * IUT.
 * 
 * @since 2020-03-18
 */
public class IndexBuilder implements Runnable {
	private final List<UrlTuple> urlBuffer;
	private final int MAX_CAPACITY;
	private final UrlTree urlIndex;
	private final long timeToLive;

	public IndexBuilder(List<UrlTuple> sharedQueue, UrlTree urlIndex, int max_capacity, final long timeToLive,
			final TimeUnit timeUnit) {
		this.urlIndex = urlIndex;
		this.urlBuffer = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
		this.timeToLive = System.nanoTime() + timeUnit.toNanos(timeToLive);
	}

	/**
	 * This method starts the Index building thread. It's task is to consume objects
	 * from the buffer and insert them into the URLIndexTree
	 */
	@Override
	public void run() {
		while (timeToLive > System.nanoTime()) {
			try {
				consume();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Thread " + Thread.currentThread().getName() + " has finished!");
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
		synchronized (urlBuffer) {
			while (urlBuffer.size() != MAX_CAPACITY) {
				if(timeToLive < System.nanoTime()){
					urlBuffer.notifyAll();
					return;
				}
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ urlBuffer.size());
				urlBuffer.wait();
			}

			

			while (urlBuffer.size() > 0) {
				UrlTuple pair = (UrlTuple) urlBuffer.remove(0);
				System.out.println("Consumed a pair by thread " + Thread.currentThread().getName());
				if (!urlIndex.search(pair)) {
					urlIndex.insert(pair);
				}
			}

			if(timeToLive < System.nanoTime()){
				urlBuffer.notifyAll();
				return;
			}
			urlBuffer.notifyAll();
		}
	}
}