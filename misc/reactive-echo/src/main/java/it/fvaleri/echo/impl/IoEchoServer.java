package it.fvaleri.echo.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import it.fvaleri.echo.EchoServer;
import it.fvaleri.echo.EchoUtil;

public class IoEchoServer extends EchoServer {
    private ServerSocket serverSocket;
    private ExecutorService handlerPool;

    public IoEchoServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        new IoEchoServer(9090).start();
    }

    @Override
    protected void onServerStart(int port) throws Exception {
        serverSocket = new ServerSocket(port, QUEUE_SIZE);
        handlerPool = EchoUtil.getThreadPool(MAX_THREADS, "handler");
    }

    @Override
    protected void onEventLoop() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            Socket socket = serverSocket.accept(); // blocking
            LOG.trace("Client {} connected", socket.getRemoteSocketAddress());
            handlerPool.execute(new IoRequestHandler(socket));
        }
    }

    @Override
    protected void onServerStop() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            EchoUtil.stopThreadPool(handlerPool);
            serverSocket.close();
        }
    }

    class IoRequestHandler implements Runnable {
        private Socket socket;

        public IoRequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            if (socket != null && socket.isConnected()) {
                try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
                    socket.setKeepAlive(true);
                    int numBytes = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((numBytes = is.read(buffer)) != -1) { // blocking
                        LOG.trace("Read {} bytes", numBytes);
                        os.write(buffer, 0, numBytes); // blocking
                        os.flush();
                    }
                } catch (Exception e) {
                    LOG.error("Handler error: {}", e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                    }
                    LOG.trace("Client {} disconnected", socket.getRemoteSocketAddress());
                }
            }
        }
    }
}

