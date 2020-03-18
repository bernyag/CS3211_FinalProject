package threads;
import java.util.*;
import entity.*;

/**
 * This class defines the index building threads which consume elements from an 
 * associated buffer (which is shared with other IBTs) and puts them into the IUT.
 * 
 * @since 2020-03-18
 */
public class IndexBuilder implements Runnable {
	private final List<UrlHtmlTuple> urlBuffer;
	private final int MAX_CAPACITY;
	private final UrlTree urlIndex;

	public IndexBuilder(List<UrlHtmlTuple> sharedQueue, UrlTree urlIndex, int max_capacity) {
		this.urlIndex = urlIndex;
		this.urlBuffer = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
	}
	
	/** This method starts the Index building thread. 
	 * It's task is to consume objects from the buffer and insert them 
	 * into the URLIndexTree
	 */
	@Override
	public void run() {
		while (true) {
			try {
				consume();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * This method tries to consume a pair from the associated buffer
	 * (by consume we mean pop it off the stack and place it in the URLIndexTree).
	 * The method is synchronized in order to avoid multi thread access to the 
	 * shared buffer
	 * 
	 * @question Why do we have 2 blocks both outputting empty? Can't these be merged
	 */
	private void consume() throws InterruptedException {
		synchronized (urlBuffer) {
			while (urlBuffer.size() != MAX_CAPACITY) {
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ urlBuffer.size());
				urlBuffer.wait();
			}

			while (urlBuffer.size() > 0) {
				UrlHtmlTuple pair = (UrlHtmlTuple) urlBuffer.remove(0);
				System.out.println("Consumed a pair by thread " + Thread.currentThread().getName());
				if(!urlIndex.search(pair)) {
					urlIndex.insert(pair);
				}
			}

			if (urlBuffer.isEmpty()) {
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: "
						+ urlBuffer.size());
				urlBuffer.wait();
			}

			urlBuffer.notifyAll();
		}
	}
}