package io.github.theminiluca.rapidobjectdb.utils;

import java.util.concurrent.ThreadFactory;

/**
 * SyncThreadFactory
 * @since 2.0.0-SNAPSHOT
 * */
public class SyncThreadFactory implements ThreadFactory {

    private final String db;
    private long threadCounter = 0;

    public SyncThreadFactory(String url, int port, String name) {
        this.db = name+"@"+url+":"+port;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "BE-"+db+"-"+threadCounter++);
    }
}
