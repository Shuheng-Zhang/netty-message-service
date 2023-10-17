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
 * HTTP 处理器
 * @author heng
 * @since 2023/6/24
 */
public class NioHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    final private static Log LOGGER = LogFactory.get();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        final var uri = request.uri();

        switch (request.method().name()) {
            case "GET" -> { // 处理 GET 请求
                final var queryDecoder = new QueryStringDecoder(uri, StandardCharsets.UTF_8);
                final var path = queryDecoder.path();
                final var params = queryDecoder.parameters();

                LOGGER.info("GET Request => [{}], [{}]", path, JSONUtil.toJsonStr(params));

                // 处理GET业务
                final var result = ApiService.handleGetRequest(path, params);

                Optional.ofNullable(result)
                        .ifPresentOrElse(
                                // 处理正常响应
                                r -> this.handleJsonResponse(
                                        ctx,
                                        HttpResponseStatus.OK,
                                        request,
                                        JSONUtil.toJsonStr(Map.of("code", 0, "msg", "OK", "data", r))
                                ),
                                // 处理不存在API响应
                                () -> this.handleJsonResponse(
                                        ctx,
                                        HttpResponseStatus.BAD_REQUEST,
                                        request,
                                        JSONUtil.toJsonStr(Map.of("code", 400, "msg", "BAD_REQUEST", "data", "NO SUCH API"))
                                )
                        );
            }
            case "POST" -> { // 处理 POST 请求
                final var reqBody = this.parseHttpBodyPayloadAsJsonStr(request);
                LOGGER.info("POST Request => [{}], [{}]", uri, reqBody);

                this.handleJsonResponse(
                        ctx,
                        HttpResponseStatus.OK,
                        request,
                        JSONUtil.toJsonStr(Map.of("code", 0, "msg", "OK", "data", JSONNull.NULL)));
            }
            // 处理其他请求
            default -> this.handleJsonResponse(
                    ctx,
                    HttpResponseStatus.BAD_REQUEST,
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

    /**
     * 处理 JSON 响应
     * @param ctx 通信上下文
     * @param httpResponseStatus HTTP 状态
     * @param request HTTP 请求体
     * @param responseJsonStr JSON数据
     */
    private void handleJsonResponse(final ChannelHandlerContext ctx,
                                    final HttpResponseStatus httpResponseStatus,
                                    final FullHttpRequest request,
                                    final String responseJsonStr) {

        // 获取数据二进制字节流
        final var responseJsonBytes = Optional.ofNullable(responseJsonStr)
                .map(resJson -> resJson.getBytes(StandardCharsets.UTF_8))
                .orElse(new byte[0]);

        // 组装响应体
        final var response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                httpResponseStatus,
                Unpooled.wrappedBuffer(responseJsonBytes)
        );

        // 设置响应头
        response.headers()
                .set("Content-Type", "application/json")
                .setInt("Content-Length", response.content().readableBytes());

        if (HttpUtil.isKeepAlive(request)) {
            // 处理 keep-alive
            response.headers().set("Connection", "keep-alive");
            // 返回响应数据
            ctx.write(response);
        } else {
            // 返回响应数据并关闭连接
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
