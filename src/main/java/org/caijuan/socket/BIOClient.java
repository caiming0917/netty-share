package org.caijuan.socket;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


@Slf4j
public class BIOClient {

    public static void main(String[] args) {
        send("127.0.0.1", 8080);
    }

    public static void send(String host, int port) {
        try (Socket socket = new Socket(host, port);
             BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
             BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream())) {

            // 发送消息
            StringBuffer request = new StringBuffer();
            request.append("I am Alice,");
            request.append("hello!");
            log.info("客户端发送请求：{}", request);
            outputStream.write(request.toString().getBytes());
            outputStream.flush();
            // 添加关闭socket 输出流
            socket.shutdownOutput();

            // 读取请求的消息
            StringBuffer response = new StringBuffer();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                response.append(new String(buffer, 0, length));
            }
            log.info("客户端接收响应：{}", response);
        } catch (IOException e) {
            log.error("发生异常：", e);
        }
    }
}
