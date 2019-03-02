package it.fvaleri.echo.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import it.fvaleri.echo.EchoServer;
import it.fvaleri.echo.EchoUtil;

import java.nio.ByteBuffer;

public class NioEchoServer extends EchoServer {
    private ExecutorService handlerPool;
    private ServerSocketChannel serverChannel;
    private Selector selector;

    public NioEchoServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        new NioEchoServer(9090).start();
    }

    @Override
    protected void onServerStart(int port) throws Exception {
        handlerPool = EchoUtil.getThreadPool(MAX_THREADS, "handler");
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().setReuseAddress(true);
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port), QUEUE_SIZE);
        selector = Selector.open(); // multiplex channel events into registered keys
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    protected void onEventLoop() throws Exception {
        if (selector != null && selector.isOpen()) {
            selector.select(); // blocking
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    if (serverChannel != null && serverChannel.isOpen()) {
                        SocketChannel channel = serverChannel.accept(); // non blocking
                        if (channel != null && channel.isOpen()) {
                            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                            channel.configureBlocking(false);
                            channel.register(selector, SelectionKey.OP_READ);
                            LOG.trace("Client {} connected", channel.getRemoteAddress());
                            selector.wakeup();
                        }
                    }
                } else {
                    it.remove();
                    handlerPool.execute(new NioRequestHandler(key)); // io offloading
                }
            }
        }
    }

    @Override
    protected void onServerStop() throws Exception {
        EchoUtil.stopThreadPool(handlerPool);
        if (selector != null && selector.isOpen()) {
            selector.close();
            serverChannel.close();
        }
    }

    class NioRequestHandler implements Runnable {
        private SelectionKey key;

        public NioRequestHandler(SelectionKey key) {
            this.key = key;
        }

        @Override
        public void run() {
            synchronized (key) {
                if (key != null && key.isValid()) {
                    try {
                        if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    } catch (Exception e) {
                        LOG.error("Handler error: {}", e.getMessage());
                        key.cancel();
                    }
                }
            }
        }

        private void read(SelectionKey key) throws IOException {
            int numBytes = 0;
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel != null && channel.isConnected()) {
                numBytes = channel.read(buffer); // blocking
                if (numBytes == -1) {
                    key.cancel();
                    LOG.trace("Client {} disconnected", channel.getRemoteAddress());
                } else if (numBytes > 0) {
                    LOG.trace("Read {} bytes", numBytes);
                    channel.register(selector, SelectionKey.OP_WRITE, buffer);
                    selector.wakeup();
                }
            }
        }

        private void write(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel != null && channel.isConnected()) {
                ByteBuffer buffer = (ByteBuffer) key.attachment();
                if (buffer != null) {
                    buffer.flip();
                    channel.write(buffer); // blocking
                    channel.register(selector, SelectionKey.OP_READ);
                    selector.wakeup();
                }
            }
        }
    }
}

