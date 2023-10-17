package top.shuzz.socketserver;

import java.io.Serializable;

/**
 * WebSocket 计数
 * @author heng
 * @since 2023/6/24
 */
public class WebSocketCountStatVo implements Serializable {

    private int channelsCount;
    private int channelMapCount;
    private int usersCount;

    public WebSocketCountStatVo() {}

    public WebSocketCountStatVo(final int channelsCount, final int channelMapCount, final int usersCount) {
        this.channelsCount = channelsCount;
        this.channelMapCount = channelMapCount;
        this.usersCount = usersCount;
    }

    public int getChannelsCount() {
        return channelsCount;
    }

    public void setChannelsCount(int channelsCount) {
        this.channelsCount = channelsCount;
    }

    public int getChannelMapCount() {
        return channelMapCount;
    }

    public void setChannelMapCount(int channelMapCount) {
        this.channelMapCount = channelMapCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = usersCount;
    }

    @Override
    public String toString() {
        return "WebSocketCountStatVo{" +
                "channelsCount=" + channelsCount +
                ", channelMapCount=" + channelMapCount +
                ", userCount=" + usersCount +
                '}';
    }
}
