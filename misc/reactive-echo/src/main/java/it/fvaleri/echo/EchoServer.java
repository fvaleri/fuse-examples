package it.fvaleri.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EchoServer implements Runnable {
    protected static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);
    protected static final int MAX_THREADS = Math.max(Runtime.getRuntime().availableProcessors(), 4);
    protected static final int QUEUE_SIZE = 100_000;
    protected static final int BUFFER_SIZE = 1_024;

    protected volatile boolean running;
    private Thread serverThread;
    private int port;

    public EchoServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        while (running) {
            try {
                onEventLoop();
            } catch (Exception e) {
            }
        }
    }

    public void start() {
        if (!running) {
            try {
                LOG.debug("Server is starting");
                running = true;
                onServerStart(port);
                serverThread = new Thread(this, "server");
                serverThread.start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    stop();
                }, "shutdown"));
                LOG.info("Server started on port {}", port);
            } catch (Exception e) {
                LOG.error("Unable to start the server", e);
            }
        } else {
            LOG.warn("Server already started");
        }
    }

    private void stop() {
        if (running) {
            try {
                LOG.debug("Server is stopping");
                running = false;
                serverThread.interrupt();
                onServerStop();
                LOG.info("Server stopped");
            } catch (Exception e) {
            }
        } else {
            LOG.warn("Server already stopped");
        }
    }

    public String status() {
        return running ? "RUNNING" : "STOPPED";
    }

    /**
     * Server initialization and socket opening.
     */
    protected abstract void onServerStart(int port) throws Exception;

    /**
     * Event loop for handling client requests or channel events.
     */
    protected abstract void onEventLoop() throws Exception;

    /**
     * Server cleanup and socket closing.
     */
    protected abstract void onServerStop() throws Exception;

}
