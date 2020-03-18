package entity;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Currently not in use. An instance of the URLbuffer 
 * is created directly in IndexBuilder.java
 * 
 * @deprecated yes
 * @since 2020-03-18
 */
public class URLbuffer {
    private final String[] buffer;
    private final int capacity;

    private int front;
    private int rear;
    private int count;

    private final Lock lock = new ReentrantLock();

    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public URLbuffer(int capacity) {
        super();

        this.capacity = capacity;

        buffer = new String[capacity];
    }

    public void deposit(String data) throws InterruptedException {
        lock.lock();

        try {
            while (count == capacity) {
                notFull.await();
            }

            buffer[rear] = data;
            rear = (rear + 1) % capacity;
            count++;

            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public String fetch() throws InterruptedException {
        lock.lock();

        try {
            while (count == 0) {
                notEmpty.await();
            }

            String result = buffer[front];
            front = (front + 1) % capacity;
            count--;

            notFull.signal();

            return result;
        } finally {
            lock.unlock();
        }
    }
}