package top.shuzz;

import top.shuzz.config.ConfigHelper;
import top.shuzz.socketserver.WebSocketServer;
import top.shuzz.httpserver.HttpServer;

/**
 * @author heng
 * @since 2023/6/21
 */
public class AppMain {

    public static void main(String[] args) {

        // 创建 HTTP 服务线程
        final var httpServerPort = ConfigHelper.getConfigInteger("server", "HTTP_PORT");
        final var httpStarter = new Thread(() -> HttpServer.initAndStart(httpServerPort), "http-service");

        // 创建 WebSocket 服务线程
        final var wsServerPort = ConfigHelper.getConfigInteger("server", "WEB_SOCKET_PORT");
        final var webSocketStarter = new Thread(() -> WebSocketServer.initAndStart("localhost", wsServerPort), "websocket-service");

        // 启动 WebSocket 服务
        webSocketStarter.start();

        // 启动 HTTP 服务
        httpStarter.start();


        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // TODO: 处理程序退出前的收尾工作
        }));*/
    }
}
