package top.shuzz;

import top.shuzz.socketserver.WebSocketServer;
import top.shuzz.httpserver.HttpServer;

/**
 * @author heng
 * @since 2023/6/21
 */
public class AppMain {

    public static void main(String[] args) {

        // 创建 HTTP 服务线程
        final var httpStarter = new Thread(() -> HttpServer.initAndStart(8080), "http-service");

        // 创建 WebSocket 服务线程
        final var webSocketStarter = new Thread(() -> WebSocketServer.initAndStart("localhost", 8081), "websocket-service");

        // 启动 WebSocket 服务
        webSocketStarter.start();

        // 启动 HTTP 服务
        httpStarter.start();
    }
}
