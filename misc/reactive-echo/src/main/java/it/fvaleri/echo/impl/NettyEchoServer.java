package it.fvaleri.echo.impl;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.fvaleri.echo.EchoServer;
import it.fvaleri.echo.EchoUtil;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class NettyEchoServer extends EchoServer {
    private EventLoopGroup acceptorPool, handlerPool;
    private ChannelFuture serverChannel;


    public NettyEchoServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        new NettyEchoServer(9090).start();
    }

    @Override
    protected void onServerStart(int port) throws Exception {
        acceptorPool = EchoUtil.getNettyGroup(1, "acceptor");
        handlerPool = EchoUtil.getNettyGroup(MAX_THREADS - 1, "handler");
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(acceptorPool, handlerPool).channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new NettyInitializer()).option(ChannelOption.SO_BACKLOG, QUEUE_SIZE)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        serverChannel = bootstrap.bind(port);
    }

    @Override
    protected void onEventLoop() throws Exception {
        serverChannel.sync();
    }

    @Override
    protected void onServerStop() throws Exception {
        EchoUtil.stopNettyGroup(handlerPool);
        EchoUtil.stopNettyGroup(acceptorPool);
        if (serverChannel != null && serverChannel.isCancellable()) {
            serverChannel.cancel(false);
        }
    }

    class NettyInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel channel) throws Exception {
            channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(BUFFER_SIZE));
            channel.pipeline().addLast(new NettyRequestHandler());
        }
    }

    class NettyRequestHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            LOG.trace("Client {} connected", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.error("Handler error: {}", cause.getMessage());
            ctx.close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            LOG.trace("Read {} bytes", ByteBufUtil.getBytes(buf).length);
            ctx.writeAndFlush(buf);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LOG.trace("Client {} disconnected", ctx.channel().remoteAddress());
        }
    }
}

