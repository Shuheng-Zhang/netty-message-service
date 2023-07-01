package top.shuzz.httpserver;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import top.shuzz.socketserver.ChannelSupervise;

import java.util.List;
import java.util.Map;

/**
 * @author heng
 * @since 2023/6/24
 */
public class ApiService {

    public static JSON handleGetRequest(final String uri, final Map<String, List<String>> params) {
        if (StrUtil.isEmptyIfStr(uri)) return null;

        return switch (uri) {
            case  "/ws-stat" -> {
                final var stat = ChannelSupervise.currentConnections();
                yield JSONUtil.parseObj(stat, false);
            }
            case "/ws-users" -> {
                yield  new JSONObject();
            }
            default -> null;
        };
    }
}
