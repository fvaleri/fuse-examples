package it.fvaleri.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoClient implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(EchoClient.class);
    private static final long REQUEST_TIMEOUT_MS = 10_000;

    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
        try {
            String host = System.getProperty("host", "localhost");
            int port = Integer.parseInt(System.getProperty("port", "9090"));
            int num = Integer.parseInt(System.getProperty("num", "10"));

            LOG.info("Running {} TCP clients against {}:{}", num, host, port);
            long t = System.nanoTime();

            // parallel requests
            List<Future<Boolean>> results = new ArrayList<>();
            ExecutorService clientPool = EchoUtil.getThreadPool(num, "client");
            for (int i = 0; i < num; i++) {
                results.add(clientPool.submit(new EchoClient(host, port)));
            }
            EchoUtil.stopThreadPool(clientPool);

            // result check
            int errors = 0;
            for (Future<Boolean> future : results) {
                try {
                    if (!future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)) { // blocking
                        errors++;
                    }
                } catch (Exception e) {
                    errors++;
                }
            }

            long d = System.nanoTime() - t;
            LOG.info("Took {} ms, {} client errors", d / 1_000_000, errors);

        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public Boolean call() {
        boolean out = false;
        SocketChannel channel = null;
        try {
            // send one request and exit
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            if (channel != null && channel.isOpen()) {
                LOG.trace("Sending request");
                EchoUtil.sleep((long) (Math.random() * 500 + 100));
                ByteBuffer buffer = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
                channel.write(buffer); // blocking
                buffer.flip();
                if (channel.read(buffer) > 0) { // blocking
                    out = true;
                }
            }
        } catch (Exception e) {
            LOG.trace("Request error: {}", e.getMessage());
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
            }
        }
        return out;
    }
}

