package io.keycafe.server.services;

import io.keycafe.server.network.codec.ByteToCommandDecoder;
import io.keycafe.server.network.codec.ReplyEncoder;
import io.keycafe.server.slot.LocalSlot;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class SlotService implements Service {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final int port;
    private LocalSlot lslot;

    public SlotService(int port, LocalSlot lslot) {
        this.port = port;
        this.lslot = lslot;
    }

    @Override
    public void run() throws Exception {
        final ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(new ByteToCommandDecoder());
                        pipeline.addLast(new ReplyEncoder());
                        pipeline.addLast(new SlotChannelHandler(lslot));
                    }
                });

        ChannelFuture f = bootstrap.bind(new InetSocketAddress("localhost", port));
        f.sync().channel().closeFuture().sync();
    }

    @Override
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}