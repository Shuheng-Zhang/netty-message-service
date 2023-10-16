package top.shuzz.socketserver;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通信通道管理容器
 *
 * @author heng
 * @since 2023/6/21
 */
public class ChannelSupervise {

    final private static Log LOGGER = LogFactory.get();

    /**
     * <p>通信通道组</p>
     * <p>用于管理客户端的通信连接</p>
     */
    final private static ChannelGroup SOCKET_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * <p>通信通道映射表</p>
     * <p>用于存储通信ID文本值与通信通道对象的映射</p>
     */
    final private static ConcurrentHashMap<String, ChannelId> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * <p>用户映射表</p>
     * <p>用于管理用户-通信ID文本值的映射</p>
     */
    final private static ConcurrentHashMap<String, String> USER_MAP = new ConcurrentHashMap<>();

    /**
     * 添加用户与通信ID的绑定
     *
     * @param userName  用户名
     * @param channelId 通信通道ID文本值
     */
    public static void addUser(final String userName, final String channelId) {
        USER_MAP.put(userName, channelId);
        LOGGER.info("User [{}:{}] Joined", userName, channelId);
    }

    /**
     * 移除用户-通信通道映射
     *
     * @param userName 待移除用户名
     */
    public static void removeUser(final String userName) {
        USER_MAP.remove(userName);
    }

    /**
     * 移除用户-通信通道映射
     *
     * @param channelId 通道ID文本值
     */
    public static void removeUserByChannelId(final String channelId) {
        USER_MAP.entrySet().removeIf(item -> {
            final var ret = item.getValue().equals(channelId);
            if (ret) {
                LOGGER.info("User [{}:{}] Leaved", item.getKey(), channelId);
            }
            return ret;
        });
    }

    /**
     * 获取用户关联通信通道ID
     *
     * @param userName 用户名
     * @return 通信通道ID文本值
     */
    public static String getUserChannelId(final String userName) {
        return USER_MAP.get(userName);
    }

    /**
     * 添加被管理通信通道
     *
     * @param socketChannel Socket 通信通道对象
     */
    public static void addChannel(final Channel socketChannel) {
        SOCKET_GROUP.add(socketChannel);
        CHANNEL_MAP.put(socketChannel.id().asLongText(), socketChannel.id());
    }

    /**
     * 移除被管理的通信通道
     *
     * @param socketChannel Socket 通信通道对象
     */
    public static void removeChannel(final Channel socketChannel) {
        SOCKET_GROUP.remove(socketChannel);
        CHANNEL_MAP.remove(socketChannel.id().asLongText());
    }

    /**
     * 获取目标通信通道
     *
     * @param channelId 目标通信通道ID文本值
     * @return 通信通道对象
     */
    public static Channel getTargetChannel(final String channelId) {
        return Optional.ofNullable(channelId)
                .map(id -> SOCKET_GROUP.find(CHANNEL_MAP.get(channelId)))
                .orElse(null);
    }

    public static String getChannelClientName(final String channelId) {
        return Optional.ofNullable(channelId)
                .flatMap(id -> USER_MAP.entrySet()
                        .stream()
                        .filter(e -> id.equals(e.getValue())).findFirst()
                        .map(Map.Entry::getKey)
                )
                .orElse(null);
    }

    /**
     * <p>发送广播信息</p>
     * <p>该消息将会被排除源消息通道</p>
     *
     * @param sourceId 消息的源通道ID
     * @param msgText  消息文本
     */
    public static void broadcastMsg(final ChannelId sourceId, final String msgText) {
        // LOGGER.info("SOCKET_GROUP = {}", JSONUtil.toJsonPrettyStr(SOCKET_GROUP.stream().map(ch -> ch.id().asLongText()).toList()));

        SOCKET_GROUP.stream()
                // 过滤源通道
                .filter(ch -> !ch.id().equals(sourceId))
                // 过滤未加入用户组的通道
                .filter(ch -> USER_MAP.contains(ch.id().asLongText()))
                // 推送消息
                .forEach(ch -> ch.writeAndFlush(new TextWebSocketFrame(msgText)));
    }

    /**
     * 输出当前用户及通道的管理数据信息
     */
    public static WebSocketCountStatVo currentConnections() {
        /*LOGGER.debug("");
        LOGGER.debug("================================ Current Connection(s) Info ================================");
        LOGGER.debug("SOCKET_GROUP: {}", SOCKET_GROUP.size());
        LOGGER.debug(" CHANNEL_MAP: {}", CHANNEL_MAP.size());
        LOGGER.debug("    USER_MAP: {}", USER_MAP.size());
        LOGGER.debug("================================ Current Connection(s) Info ================================");
        LOGGER.debug("");*/

        return new WebSocketCountStatVo(SOCKET_GROUP.size(), CHANNEL_MAP.size(), USER_MAP.size());
    }

    public static List<String> currentUsers() {
        return USER_MAP.keySet().stream().toList();
    }
}
