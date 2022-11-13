package org.caijuan.reactor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 多线程 Reactor
 */
@Slf4j
public class MultiNettyServer {

    public void start(int port) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast("codec", new HttpServerCodec())// HTTP 编解码
                            .addLast("compressor", new HttpContentCompressor())// HttpContent 压缩
                            .addLast("aggregator", new HttpObjectAggregator(65536))// HTTP 消息聚合
                            .addLast("handler", new HttpServerHandler());// 自定义业务逻辑处理器
                    }
                }).childOption(ChannelOption.SO_KEEPALIVE, true);// 设置 TCP 长连接
            ChannelFuture f = b.bind().sync();
            log.info("netty http 服务端启动， 监听端口： {}" ,port);
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new MultiNettyServer().start(8080);
    }
}
