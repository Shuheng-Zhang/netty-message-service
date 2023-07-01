package top.shuzz.socketserver;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author heng
 * @since 2023/6/21
 */
public class WebSocketServer {

    final private static Log LOGGER = LogFactory.get();

    /**
     * 初始化并启动服务端
     * @param host 服务地址, 默认 localhost
     * @param port 服务端口号, 默认 8080
     */
    public static void initAndStart(final String host, final Integer port) {
        LOGGER.info("WebSocket Server Starting...");

        // 连接处理组
        final var acceptGroup = new NioEventLoopGroup();
        // 业务处理组
        final var workerGroup = new NioEventLoopGroup();

        try {
            // 服务启动类
            final var serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    // 配置连接组及服务组
                    .group(acceptGroup, workerGroup)
                    // 配置通信通道类型
                    .channel(NioServerSocketChannel.class)
                    // 配置通信处理器
                    .childHandler(new NioWebSocketChannelInitializer(
                            StrUtil.isEmptyIfStr(host) ? "localhost" : host,
                            port == null ? 8080 : port,
                            "/ws"
                    ));

            // 绑定服务端点
            final var serverChannel = serverBootstrap.bind(port == null ? 8080 : port).sync().channel();

            LOGGER.info("WebSocket Server Started, Listening on {}", port);

            // 同步等待服务端点关闭
            serverChannel.closeFuture().sync();
        }  catch (Exception e) {
            LOGGER.error(e, "WebSocket Server Error Occurred");
        } finally {
            // 关闭连接组
            acceptGroup.shutdownGracefully();
            // 关闭业务组
            workerGroup.shutdownGracefully();

            LOGGER.info("WebSocket Server Closed, Bye.");
        }

    }
}
