package net.unit8.job_streamer.agent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kawasima
 */
public class JobStreamerThreadFactory implements ThreadFactory {
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix = "job-streamer-";
    final ClassLoaderFinder finder;

    public JobStreamerThreadFactory(ClassLoaderFinder finder) {
        this.finder = finder;
    }
    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(true);
        t.setContextClassLoader(finder.find());
        return t;
    }
}
