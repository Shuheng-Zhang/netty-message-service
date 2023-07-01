package top.shuzz.socketserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author heng
 * @since 2023/6/21
 */
public class NioWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    final private String host;
    final private int port;
    final private String prefix;

    public NioWebSocketChannelInitializer(final String host, final int port, final String prefix) {
        this.host = host;
        this.port = port;
        this.prefix = prefix;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final var pipeLine = ch.pipeline();
        pipeLine.addLast("logger", new LoggingHandler());
        pipeLine.addLast("http-codec", new HttpServerCodec());
        pipeLine.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeLine.addLast("http-chunked", new ChunkedWriteHandler());

        // 业务处理器(自定义 WebSocket 处理器)
        pipeLine.addLast("nio-websocket-handler", new NioWebSocketHandler(host, port, prefix));
    }
}
