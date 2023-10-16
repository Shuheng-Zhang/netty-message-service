package top.shuzz.socketserver;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * WebSocket 业务逻辑处理器
 *
 * @author heng
 * @since 2023/6/21
 */
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    final private static Log LOGGER = LogFactory.get();

    /**
     * 服务主机地址(IP/域名)
     */
    final private String host;
    /**
     * 连接前缀
     */
    final private String prefix;
    /**
     * 服务端口号
     */
    final private int port;

    /**
     * WebSocket 握手器
     */
    private WebSocketServerHandshaker handShaker;

    public NioWebSocketHandler(final String host, final int port, final String prefix) {
        this.host = host;
        this.port = port;
        this.prefix = prefix;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Connection [{}] Activated", ctx.channel().id().asLongText());
        // 当连接建立时触发
        // 将该通信通道添加到管理容器
        ChannelSupervise.addChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Connection [{}] Deactivated", ctx.channel().id().asLongText());
        // 当连接断开时触发
        // 将该通信通道移出管理容器
        ChannelSupervise.removeUserByChannelId(ctx.channel().id().asLongText());
        ChannelSupervise.removeChannel(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 当服务器接收到请求数据时触发
        if (msg instanceof FullHttpRequest) {
            // 处理 HTTP-Socket 数据请求
            // 通过握手建立长连接
            final var clientName = this.handleHttpRequest(ctx, (FullHttpRequest) msg);

            // 处理用户注册
            Optional.ofNullable(clientName).ifPresent(cn -> this.handleUserRegister(ctx, clientName));

        } else if (msg instanceof WebSocketFrame) {
            // 处理 WebSocket 数据请求
            this.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * <p>处理 HTTP-WebSocket 请求</p>
     * <p>初次接入时使用 HTTP 协议 访问服务端</p>
     * <p>使用 Upgrade 参数以建立 WebSocket 长连接</p>
     *
     * @param ctx     通道处理器上下文
     * @param request HTTP-WebSocket 请求
     * @return 客户端名称
     */
    private String handleHttpRequest(final ChannelHandlerContext ctx, FullHttpRequest request) {
        // 处理非法请求
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            this.sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return null;
        }

        // 获取客户端名称
        final var uriParams = Optional.ofNullable(request.uri())
                .map(uri -> uri.split("/"))
                .map(Arrays::asList)
                .stream()
                .flatMap(Collection::stream)
                .filter(StrUtil::isNotBlank)
                .toList();
        final var clientName = uriParams.size() == 2 ? uriParams.get(1) : null;

        return Optional.ofNullable(clientName)
                .map(cn -> {
                    // 处理 HTTP-WebSocket 首次连接请求
                    // 为 WebSocket 配置通信握手
                    final var wsFactory = new WebSocketServerHandshakerFactory(
                            "ws://" + host + ":" + port + prefix + "/" + clientName,
                            null,
                            false);

                    // 建立握手
                    handShaker = wsFactory.newHandshaker(request);
                    if (handShaker == null) {
                        // 握手失败
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                        return null;
                    } else {
                        // 握手成功
                        handShaker.handshake(ctx.channel(), request);
                        return clientName;
                    }
                })
                .orElseGet(() -> {
                    this.sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
                    return null;
                });
    }

    /**
     * 返回 HTTP 响应
     *
     * @param ctx      通道处理器上下文
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     */
    private void sendHttpResponse(final ChannelHandlerContext ctx,
                                  final FullHttpRequest request,
                                  final DefaultFullHttpResponse response) {

        if (200 != response.status().code()) {
            final var buf = Unpooled.copiedBuffer(response.status().toString(), StandardCharsets.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }

        final var curChannelFuture = ctx.channel().writeAndFlush(response);

        if (!request.protocolVersion().isKeepAliveDefault() || response.status().code() != 200) {
            curChannelFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    /**
     * 处理客户端用户注册
     *
     * @param ctx        通道处理器上下文
     * @param clientName 客户端用户名
     */
    private void handleUserRegister(ChannelHandlerContext ctx, String clientName) {
        final var channelId = ctx.channel().id().asLongText();

        // 处理用户重复
        Optional.ofNullable(ChannelSupervise.getUserChannelId(clientName)).ifPresent(chId -> {
            ChannelSupervise.getTargetChannel(chId).close();
            ChannelSupervise.removeUserByChannelId(chId);

            LOGGER.warn("Duplicated [{}:{}], Disconnecting...", clientName, chId);
        });

        // 用户注册
        ChannelSupervise.addUser(clientName, channelId);
    }

    /**
     * 处理 WebSocket 帧数据
     *
     * @param ctx   通道处理器上下文
     * @param frame 数据帧
     */
    private void handleWebSocketFrame(final ChannelHandlerContext ctx, WebSocketFrame frame) {

        // 处理通信关闭的帧
        if (frame instanceof CloseWebSocketFrame) {
            handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        // 处理 Ping-Pong 帧
        if (frame instanceof PingWebSocketFrame) {
            LOGGER.info("Connection [{}] Heartbeat", ctx.channel().id().asLongText());
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }

        // 处理业务数据帧
        if (frame instanceof TextWebSocketFrame) {
            final var msgText = ((TextWebSocketFrame) frame).text();

            try {
                final var clientChannelId = ctx.channel().id().asLongText();
                final var source = ChannelSupervise.getChannelClientName(clientChannelId);

                // 处理 JSON 数据体
                if (msgText.startsWith("{") && msgText.endsWith("}")) {
                    final var dataFrame = JSONUtil.toBean(msgText, WebSocketMsgDto.class);
                    final var targets = dataFrame.getTargets();

                    // 处理未加入用户的消息通信
                    if (StrUtil.isEmptyIfStr(source)) {
                        final var reply = new WebSocketMsgDto("Server", null, "Channel Not Join");
                        ChannelSupervise.getTargetChannel(ctx.channel().id().asLongText()).writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(reply)));
                        return;
                    }

                    // 处理指定用户的消息推送
                    if (targets != null && !targets.isEmpty()) {
                        targets.stream()
                                // 过滤自身
                                .filter(target -> !target.equals(source))
                                // 过滤空目标
                                .filter(StrUtil::isNotEmpty)
                                .filter(StrUtil::isNotBlank)
                                // 依次发送消息
                                .forEach(target -> {
                                    final var targetChannelId = ChannelSupervise.getUserChannelId(target);
                                    Optional.ofNullable(ChannelSupervise.getTargetChannel(targetChannelId)).ifPresent(ch -> ch.writeAndFlush(new TextWebSocketFrame(msgText)));
                                });
                        return;
                    }

                    // 群发消息
                    ChannelSupervise.broadcastMsg(ctx.channel().id(), msgText);
                    return;
                }

                // 默认群发文本数据
                ChannelSupervise.broadcastMsg(ctx.channel().id(), msgText);
            } catch (Exception e) {
                LOGGER.error(e, "Error Occurred");
            }
        }

    }


}
