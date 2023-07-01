package top.shuzz.httpserver;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author heng
 * @since 2023/6/24
 */
public class HttpServer {

    final private static Log LOGGER = LogFactory.get();

    public static void initAndStart(final Integer port) {
        LOGGER.info("HTTP Server Starting...");

        final var acceptGroup = new NioEventLoopGroup();
        final var workerGroup = new NioEventLoopGroup();

        try {
            final var serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(acceptGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NioHttpChannelInitializer());

            final var serverChannel = serverBootstrap.bind(port).channel();

            LOGGER.info("HTTP Server Started, Listening on {}", port);

            serverChannel.closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error(e, "HTTP Server Error Occurred");
        } finally {
            acceptGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            LOGGER.info("HTTP Server Closed, Bye.");
        }
    }
}
