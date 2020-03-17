package threads;
import java.util.*;
import entity.*;

// This will be our IUT

public class IndexBuilder implements Runnable {
	private final List<UrlHtmlTuple> urlBuffer;
	private final int MAX_CAPACITY;
	private final UrlTree urlIndex;

	public IndexBuilder(List<UrlHtmlTuple> sharedQueue, UrlTree urlIndex, int max_capacity) {
		this.urlIndex = urlIndex;
		this.urlBuffer = sharedQueue;
		this.MAX_CAPACITY = max_capacity;
	}

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