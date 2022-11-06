package org.caijuan.socket;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * BIO 服务端
 * <p>
 * https://blog.csdn.net/echohawk/article/details/124668763
 */
@Slf4j
public class TestHttpServer {

    public static void main(String[] args) {
        start();
    }

    /**
     * 启动服务器
     */
    public static void start() {
        int port = 8080;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            log.info("服务端开始启动，端口 port: {}", port);
            Socket socket;
            while (Objects.nonNull((socket = serverSocket.accept()))) {
                log.info("建立连接，客户端 IP:[{}], 端口 port:[{}]", socket.getInetAddress().getHostAddress(), socket.getPort());
                try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                     BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {

                    // 读取请求的消息
                    StringBuffer request = new StringBuffer();
                    byte[] buffer = new byte[1024];
                    int length;
                    if ((length = inputStream.read(buffer)) > 0) {
                        request.append(new String(buffer, 0, length));
                    }
                    log.info("服务端接收请求：\n{}", request);

                    // 返回响应
                    StringBuffer response = new StringBuffer();
                    response.append(request);
                    log.info("服务端发送响应：\n{}", response);

                    outputStream.write(response.toString().getBytes());
                    outputStream.flush();
                    log.info("=== 断开连接 ===\n");
                } catch (IOException e) {
                    log.error("发生异常：", e);
                }
            }
        } catch (IOException e) {
            log.error("socketsimple 启动异常，{}", e.getMessage());
            e.printStackTrace();
        }

    }
}
