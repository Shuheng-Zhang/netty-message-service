package top.shuzz.httpserver;

import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * @author heng
 * @since 2023/6/24
 */
public class NioHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    final private static Log LOGGER = LogFactory.get();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        final var uri = request.uri();

        switch (request.method().name()) {
            case "GET" -> {
                final var queryDecoder = new QueryStringDecoder(uri, StandardCharsets.UTF_8);
                final var path = queryDecoder.path();
                final var params = queryDecoder.parameters();

                LOGGER.info("GET Request => [{}], [{}]", path, JSONUtil.toJsonStr(params));

                final var result = ApiService.handleGetRequest(path, params);

                this.handleJsonResposne(
                        ctx,
                        request,
                        JSONUtil.toJsonStr(Map.of("code", 0, "msg", "OK", "data", result)));
            }
            case "POST" -> {
                final var reqBody = this.parseHttpBodyPayloadAsJsonStr(request);
                LOGGER.info("POST Request => [{}], [{}]", uri, reqBody);

                this.handleJsonResposne(
                        ctx,
                        request,
                        JSONUtil.toJsonStr(Map.of("code", 0, "msg", "OK", "data", JSONNull.NULL)));
            }
            default -> this.handleJsonResposne(
                    ctx,
                    request,
                    JSONUtil.toJsonStr(Map.of("code", 400, "msg", "BAD REQUEST", "data", JSONNull.NULL))
            );
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }



    private String parseHttpBodyPayloadAsJsonStr(final FullHttpRequest request) {
        final var jsonBuf = request.content();
        return jsonBuf.toString(StandardCharsets.UTF_8);
    }

    private void handleJsonResposne(final ChannelHandlerContext ctx,
                                    final FullHttpRequest request,
                                    final String responseJsonStr) {
        final var responseJsonBytes = Optional.ofNullable(responseJsonStr)
                .map(resJson -> resJson.getBytes(StandardCharsets.UTF_8))
                .orElse(new byte[0]);

        final var response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(responseJsonBytes)
        );
        response.headers()
                .set("Content-Type", "application/json")
                .setInt("Content-Length", response.content().readableBytes());

        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set("Connection", "keep-alive");
            ctx.write(response);
        } else {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
