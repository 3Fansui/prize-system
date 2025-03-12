package com.test.prizesystem.async;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 双锁阻塞队列实现
 * <p>
 * 使用双锁机制(头锁和尾锁)提高并发性能，头部和尾部可以并行操作。
 * 通过条件变量实现线程等待和唤醒，避免忙等待消耗CPU。
 * 
 * @param <E> 元素类型
 * @version 1.0
 */
@SuppressWarnings("all")
public class BlockingQueue2<E> {

    private final E[] array;
    private int head;
    private int tail;
    private AtomicInteger size = new AtomicInteger();

    private ReentrantLock tailLock = new ReentrantLock();
    private Condition tailWaits = tailLock.newCondition();

    private ReentrantLock headLock = new ReentrantLock();
    private Condition headWaits = headLock.newCondition();

    public BlockingQueue2(int capacity) {
        this.array = (E[]) new Object[capacity];
    }

    private boolean isEmpty() {
        return size.get() == 0;
    }

    private boolean isFull() {
        return size.get() == array.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }

    /**
     * 将元素放入队列，如果队列已满则等待
     * @param e 要添加的元素
     * @throws InterruptedException 如果线程被中断
     */
    public void offer(E e) throws InterruptedException {
        int c; // 添加前元素个数
        tailLock.lockInterruptibly();
        try {
            // 1. 队列满则等待
            while (isFull()) {
                tailWaits.await();
            }

            // 2. 不满则入队
            array[tail] = e;
            if (++tail == array.length) {
                tail = 0;
            }

            // 3. 修改 size
            c = size.getAndIncrement();
            if (c + 1 < array.length) {
                tailWaits.signal();
            }
        } finally {
            tailLock.unlock();
        }

        // 4. 如果从0变为非空，由offer这边唤醒等待非空的poll线程
        if(c == 0) {
            headLock.lock();
            try {
                headWaits.signal();
            } finally {
                headLock.unlock();
            }
        }
    }

    /**
     * 从队列中取出元素，如果队列为空则等待
     * @return 队列中的元素
     * @throws InterruptedException 如果线程被中断
     */
    public E poll() throws InterruptedException {
        E e;
        int c; // 取走前的元素个数
        headLock.lockInterruptibly();
        try {
            // 1. 队列空则等待
            while (isEmpty()) {
                headWaits.await();
            }

            // 2. 非空则出队
            e = array[head];
            array[head] = null; // help GC
            if (++head == array.length) {
                head = 0;
            }

            // 3. 修改 size
            c = size.getAndDecrement();
            if (c > 1) {
                headWaits.signal();
            }
        } finally {
            headLock.unlock();
        }

        // 4. 队列从满->不满时 由poll唤醒等待不满的 offer 线程
        if(c == array.length) {
            tailLock.lock();
            try {
                tailWaits.signal();
            } finally {
                tailLock.unlock();
            }
        }

        return e;
    }

    /**
     * 获取队列大小
     * @return 队列中的元素数量
     */
    public int size() {
        return size.get();
    }
}
