package top.shuzz.socketserver;

import java.io.Serializable;
import java.util.List;

/**
 * WebSocket 数据传输对象
 * @author heng
 * @since 2023/6/21
 */
public class WebSocketMsgDto implements Serializable {

    /**
     * 消息目标列表
     */
    private List<String> targets;
    /**
     * 消息源
     */
    private String source;
    /**
     * 消息数据体
     */
    private Object dataBody;

    public WebSocketMsgDto() {}

    public WebSocketMsgDto(final String source, final List<String> targets, final Object dataBody) {
        this.source = source;
        this.targets = targets;
        this.dataBody = dataBody;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDataBody(Object dataBody) {
        this.dataBody = dataBody;
    }

    public Object getDataBody() {
        return dataBody;
    }

    public List<String> getTargets() {
        return targets;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "WebSocketMsgDto{" +
                "targets=" + targets +
                ", source='" + source + '\'' +
                ", dataBody=" + dataBody +
                '}';
    }
}
