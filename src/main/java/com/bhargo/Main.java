package com.bhargo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by barya on 10/5/2016.
 */
@SpringBootApplication
public class Main implements CommandLineRunner{

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    static void producerConsumerWaitNotify () {
        Resource resource = new Resource("");
        new userProducer(resource).start();
        new userConsumer(resource).start();
    }

    static class userProducer  extends Thread {

        private Resource resource;

        userProducer(Resource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            synchronized (resource) {
                while (true) {
                    if(resource.isProduced()) {
                        try {
                            resource.wait();
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("produced by " + Thread.currentThread().getName() + new Date());
                    resource.setProduced(true);
                    resource.notify();
                }
            }

        }
    }

    static class userConsumer extends Thread {

        private Resource resource;

        userConsumer(Resource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            synchronized (resource) {
                while (true) {
                    if (!resource.isProduced()) {
                        try {
                            resource.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("consumed by " + Thread.currentThread().getName() + new Date());
                    resource.setProduced(false);
                    resource.notify();
                }
            }
        }
    }

    static void customCyclicBarrierDemo () {
        customCyclicBarrier barrier = new customCyclicBarrier(5, () -> {
            System.out.println("All threads have arrived at the barrier");
        });
        new Thread(new customRunnable(barrier, true)).start();
        new Thread(new customRunnable(barrier, false)).start();
        new Thread(new customRunnable(barrier, false)).start();
        new Thread(new customRunnable(barrier, false)).start();
        new Thread(new customRunnable(barrier, false)).start();
    }

    @Override
    public void run(String... args) throws Exception {
        //multipleProdCons();
        ExecutorService executorService = Executors.newScheduledThreadPool(2);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        //System.out.println(executor.getMaximumPoolSize() + " " + executor.getCorePoolSize());
        //executor.submit(() -> {});

        //findHighestTwoNumbers(new Integer[]{54,856,86,12,4,66,856,35});

        // multiThreadingPrintNumbers();
        customCyclicBarrierDemo();
    }

    static class customRunnable implements Runnable {

        private customCyclicBarrier customCyclicBarrier;
        private boolean wait;

        public customRunnable(Main.customCyclicBarrier customCyclicBarrier, boolean wait) {
            this.customCyclicBarrier = customCyclicBarrier;
            this.wait = wait;
        }

        @Override
        public void run() {
            try {
                if (wait) {
                    Thread.sleep(5000);
                }
                System.out.println(Thread.currentThread().getName() + " is waiting at the barrier");
                customCyclicBarrier.await();
                System.out.println(Thread.currentThread().getName() + " crossed the barrier");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class customCyclicBarrier  {
        private AtomicInteger count;
        private Runnable postAction;
        private Lock lock = new ReentrantLock();
        private Condition condition = lock.newCondition();

        public customCyclicBarrier(int count, Runnable postAction) {
            this.count = new AtomicInteger(count);
            this.postAction = postAction;
        }

        public customCyclicBarrier(int count) {
            this.count = new AtomicInteger(count);
        }

        public void await() throws Exception {
            lock.lock();
            count.decrementAndGet();
            System.out.println("The count is " + count);
            if(count.get() < 0) {
                throw  new Exception("barries broken");
            }
                try {
                    if(count.get() == 0) {
                        if (postAction != null) {
                            postAction.run();
                        }
                        condition.signalAll();
                        return;
                    }
                    if(count.get() > 0) {
                        condition.await();
                    }
                } finally {
                    lock.unlock();
                }

        }

        private void signal () {
            condition.signalAll();
        }
    }



    @FunctionalInterface
    private interface func<T> {
        T produce ();
    }

    private static void multiThreadingPrintNumbers() {

        BlockingQueue<Integer> sendingQueue1 = new ArrayBlockingQueue<>(1);
        BlockingQueue<Integer> sendingQueue2 = new ArrayBlockingQueue<>(1);
        BlockingQueue<Integer> sendingQueue3 = new ArrayBlockingQueue<>(1);


        int[] intArr1 = new int[]{1};
        int[] intArr2 = new int[]{2};
        int[] intArr3 = new int[]{3};
        new Thread(new userRunnable<Integer>(sendingQueue3, sendingQueue1, true, () -> {
            if (intArr1[0] == 1) {
                intArr1[0] = 4;
                return new Integer(1);
            } else if (intArr1[0] == 4) {
                intArr1[0] = 7;
                return new Integer(4);
            } else {
                intArr1[0] = 1;
                return new Integer(7);
            }
        })).start();
        new Thread(new userRunnable<Integer>(sendingQueue1, sendingQueue2, false, () -> {
            if (intArr2[0] == 2) {
                intArr2[0] = 5;
                return new Integer(2);
            } else if (intArr2[0] == 5) {
                intArr2[0] = 8;
                return new Integer(5);
            } else {
                intArr2[0] = 2;
                return new Integer(8);
            }
        })).start();
        new Thread(new userRunnable<Integer>(sendingQueue2, sendingQueue3, false, () -> {
            if (intArr3[0] == 3) {
                intArr3[0] = 6;
                return new Integer(3);
            } else if (intArr3[0] == 6) {
                intArr3[0] = 9;
                return new Integer(6);
            } else {
                intArr3[0] = 3;
                return new Integer(9);
            }

        })).start();

    }
    private static class userRunnable<T> implements Runnable {

        private BlockingQueue<T> receivingQueue;
        private BlockingQueue<T> sendingQueue;
        private boolean init;
        private func<T> func;

        public userRunnable(BlockingQueue<T> receivingQueue, BlockingQueue<T> sendingQueue, boolean init, func func) {
            this.receivingQueue = receivingQueue;
            this.sendingQueue = sendingQueue;
            this.init = init;
            this.func = func;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    if(init) {
                        sendingQueue.put(func.produce());
                        init = false;
                    }
                    if(receivingQueue.size() > 0) {
                        System.out.println(receivingQueue.take());
                        sendingQueue.put(func.produce());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void findHighestTwoNumbers (Integer[] arr) {
        int highest, secondHighest;
        if(arr[0] > arr[1]) {
            highest = arr[0];
            secondHighest = arr[1];
        } else {
            secondHighest = arr[0];
            highest = arr[1];
        }
        for (int i =2; i<arr.length -1; i++ ) {
            if(arr[i] > secondHighest) {
                if(arr[i] > highest) {
                    secondHighest = highest;
                    highest = arr[i];
                } else if (arr[i] < highest) {
                    secondHighest = arr[i];
                }
            }
        }
        System.out.printf("The largest num is %d and the 2nd largest is %d", highest, secondHighest);
    }

    private static void sortMapByValues (Map<Integer, String> map) {
        Set<String> set = new TreeSet<>(map.values());
    }

    private static void multipleProdCons () {
        BlockingDeque<Resource> blockingDeque = new LinkedBlockingDeque<>();
        for(int i =1; i<=10;i++) {
            new Thread(new Producer(blockingDeque)).start();
        }

        for(int i =1; i<=10;i++) {
            new Thread(new Consumer(blockingDeque)).start();
        }
    }

    private static class Resource {

        private boolean produced;

        private String str;

        public Resource(String str) {
            this.str = str;
        }

        public boolean isProduced() {
            return produced;
        }

        public void setProduced(boolean produced) {
            this.produced = produced;
        }

        public String getStr() {

            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    private static class Producer implements Runnable {

        private BlockingDeque<Resource> blockingDeque;

        public Producer(BlockingDeque<Resource> blockingDeque) {
            this.blockingDeque = blockingDeque;
        }

        @Override
        public void run() {
            Resource resource;
                while (true) {
                    try {
                        Thread.sleep(2000);
                        resource = new Resource(Thread.currentThread().getName() + " " + new Date().toString());
                        blockingDeque.add(resource);
                        System.out.println(resource);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    private static class Consumer implements Runnable {

        private BlockingDeque<Resource> blockingDeque;

        public Consumer(BlockingDeque<Resource> blockingDeque) {
            this.blockingDeque = blockingDeque;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    if(blockingDeque.size() > 0)
                    System.out.println(Thread.currentThread().getName() + " consumed " + blockingDeque.take());
                } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
