package it.fvaleri.echo.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import it.fvaleri.echo.EchoServer;
import it.fvaleri.echo.EchoUtil;

import java.nio.ByteBuffer;

public class Nio2EchoServer extends EchoServer {
    private AsynchronousChannelGroup handlerPool;
    private AsynchronousServerSocketChannel serverChannel;

    public Nio2EchoServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        new Nio2EchoServer(9090).start();
    }

    @Override
    protected void onServerStart(int port) throws Exception {
        handlerPool = EchoUtil.getNio2Group(MAX_THREADS, "handler");
        serverChannel = AsynchronousServerSocketChannel.open(handlerPool).bind(new InetSocketAddress(port), QUEUE_SIZE);
    }

    @Override
    protected void onEventLoop() throws Exception {
        serverChannel.accept(null, new Nio2RequestHandler());
    }

    @Override
    protected void onServerStop() throws Exception {
        EchoUtil.stopNio2Group(handlerPool);
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
    }

    class Nio2RequestHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
        private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        @Override
        public void completed(AsynchronousSocketChannel channel, Void att) {
            try {
                if (channel != null && channel.isOpen()) {
                    LOG.trace("Client {} connected", channel.getRemoteAddress());
                    int numBytes = 0;
                    while (numBytes != -1) {
                        write(channel, numBytes);
                        numBytes = read(channel);
                    }
                    LOG.trace("Client {} disconnected", channel.getRemoteAddress());
                }
            } catch (Exception e) {
                LOG.error("Handler error: {}", e.getMessage());
                if (channel.isOpen()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }

        private int read(AsynchronousSocketChannel channel) throws Exception {
            buffer.clear();
            int numBytes = channel.read(buffer).get(); // blocking
            if (numBytes > 0) {
                LOG.trace("Read {} bytes", numBytes);
            }
            return numBytes;
        }

        private void write(AsynchronousSocketChannel channel, int numBytes) throws Exception {
            if (numBytes > 0) {
                buffer.flip();
                channel.write(buffer).get(); // blocking
            }
        }

        @Override
        public void failed(Throwable e, Void att) {
            LOG.trace("Handler error: {}", e.getMessage());
        }
    }
}

