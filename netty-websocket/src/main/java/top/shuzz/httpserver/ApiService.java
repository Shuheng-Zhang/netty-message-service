package top.shuzz.httpserver;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import top.shuzz.socketserver.ChannelSupervise;

import java.util.List;
import java.util.Map;

/**
 * API 服务
 * @author heng
 * @since 2023/6/24
 */
public class ApiService {

    final private static String API_PREFIX = "/api";

    /**
     * GET 请求处理器
     * @param uri URI请求地址
     * @param params 请求参数
     * @return 响应数据
     */
    public static JSON handleGetRequest(final String uri, final Map<String, List<String>> params) {
        if (StrUtil.isEmptyIfStr(uri)) return null;

        return switch (uri) {
            case  API_PREFIX + "/ws-stat" -> {
                final var stat = ChannelSupervise.currentConnections();
                yield JSONUtil.parseObj(stat, false);
            }
            case API_PREFIX + "/ws-users" -> new JSONArray(ChannelSupervise.currentUsers());
            default -> null;
        };
    }
}
