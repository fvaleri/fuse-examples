package it.fvaleri.echo;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class EchoUtil {
    private static final Logger LOG = LoggerFactory.getLogger(EchoUtil.class);
    private static final long STOP_TIMEOUT_MS = 30_000;
    private static final int QUEUE_SIZE = 100_000;

    private EchoUtil() {
    }

    public static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            LOG.trace("Interrupted while sleeping");
        }
    }

    public static ExecutorService getThreadPool(int size, String prefix) {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy();
        ExecutorService executor = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE), new NamedThreadFactory(prefix), handler);
        LOG.debug("Created {} thread pool of size {}", prefix, size);
        return executor;
    }

    public static void stopThreadPool(ExecutorService executor) {
        try {
            if (executor == null) {
                return;
            }
            executor.shutdown();
            executor.awaitTermination(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Stop interrupted");
        } finally {
            if (executor.isTerminated()) {
                LOG.trace("All tasks completed");
            } else {
                executor.shutdownNow();
            }
        }
    }

    public static AsynchronousChannelGroup getNio2Group(int size, String name) {
        ExecutorService executor = getThreadPool(size, name);
        AsynchronousChannelGroup group = null;
        try {
            group = AsynchronousChannelGroup.withThreadPool(executor);
        } catch (IOException e) {
        }
        return group;
    }

    public static void stopNio2Group(AsynchronousChannelGroup group) {
        try {
            if (group == null) {
                return;
            }
            group.shutdown();
            group.awaitTermination(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Stop interrupted");
        } finally {
            if (group.isTerminated()) {
                LOG.trace("All tasks completed");
            }
        }
    }

    public static EventLoopGroup getNettyGroup(int size, String name) {
        Executor executor = getThreadPool(size, name);
        EventLoopGroup group = new NioEventLoopGroup(size, executor);
        return group;
    }

    public static void stopNettyGroup(EventLoopGroup group) {
        try {
            if (group == null) {
                return;
            }
            group.shutdownGracefully();
            group.awaitTermination(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Stop interrupted");
        } finally {
            if (group.isTerminated()) {
                LOG.trace("All tasks completed");
            }
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private String name;
        private AtomicLong counter;

        public NamedThreadFactory(String name) {
            this.name = name;
            this.counter = new AtomicLong();
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" + counter.incrementAndGet());
        }
    }
}

