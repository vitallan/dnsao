package com.allanvital.dnsao.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ThreadShop {

    public static ThreadFactory buildThreadFactory(String poolName) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                int andIncrement = threadNumber.getAndIncrement();
                t.setName(String.format("%s-%02d", poolName, andIncrement));
                return t;
            }
        };
    }

    public static ExecutorService buildExecutor(String poolName, int size) {
        return Executors.newFixedThreadPool(size, buildThreadFactory(poolName));
    }

}