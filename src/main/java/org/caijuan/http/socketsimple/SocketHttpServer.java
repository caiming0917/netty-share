package org.caijuan.http.socketsimple;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

/**
 * Http 服务端
 */
@Slf4j
public class SocketHttpServer {

    public static void main(String[] args) {
        new SocketHttpServer().start();
    }


    /**
     * 启动服务器
     */
    public void start() {
        int port = 8080;
        try {
            // 0.绑定端口
            ServerSocket serverSocket = new ServerSocket(port);
            log.info("服务端开始启动，端口 port: {}", port);
            Socket socket;
            // 1.循环监听 and 2.建立连接
            while (Objects.nonNull((socket = serverSocket.accept()))) {
                log.info("建立连接，客户端 IP:[{}], 端口 port:[{}]", socket.getInetAddress().getHostAddress(), socket.getPort());
                try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                     BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {

                    // 3.input : 读取数据
                    byte[] buffer = new byte[1024 * 256];
                    int length = inputStream.read(buffer);

                    // 4.compute : 计算
                    // 4.1 编码
                    HttpDecoder httpDecoder = new HttpDecoder();
                    Map<String, String> request = httpDecoder.deEncode(buffer, length);
                    if (Objects.isNull(request)) {
                        log.info("-------------- 断开连接 --------------\n");
                        continue;
                    }
                    // 4.2 业务处理
                    log.info("url: {}", request.get("Url"));
                    log.info("method: {}", request.get("Method"));
                    log.info("data: {}", request.get("Data"));
                    String responseContent = "这是我的 http web 服务器 ^-^";
                    // 4.3 编码
                    HttpEncoder httpEncoder = new HttpEncoder();
                    byte[] response = httpEncoder.encode(responseContent);

                    // 5.output : 返回数据
                    outputStream.write(response);
                    outputStream.flush();

                    // 6.断开连接
                    log.info("-------------- 断开连接 --------------\n");
                } catch (IOException e) {
                    log.error("发生异常：", e);
                }
            }
        } catch (IOException e) {
            log.error("socket simple 服务端启动异常，{}", e.getMessage());
            e.printStackTrace();
        }

    }
}
