package org.caijuan.http.socketsimple;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static org.caijuan.http.common.HttpConstant.*;

/**
 * HTTP/1.1 200 OK
 * Server: nginx
 * Date: Mon, 20 Feb 2017 09:13:59 GMT
 * Content-Type: text/plain;charset=UTF-8
 * Vary: Accept-Encoding
 * Cache-Control: no-store
 * Pragrma: no-cache
 * Expires: Thu, 01 Jan 1970 00:00:00 GMT
 * Cache-Control: no-cache
 * Content-Encoding: gzip
 * Transfer-Encoding: chunked
 * Proxy-Connection: Keep-alive
 * <p>
 * {"code":200,"notice":0,"follow":0,"forward":0,"msg":0,"comment":0}
 */

@Slf4j
public class HttpEncoder {

    public byte[] encode(String content) {
        StringBuffer response = new StringBuffer();
        // 状态行
        String statusLine = "HTTP/1.1 200 OK";
        response.append(statusLine).append(LINE_BREAK);

        // 消息包头
        response.append("Server").append(LINER_SPLIT).append("CaijuanCat").append(LINE_BREAK);
        response.append("Content-Type").append(LINER_SPLIT).append("text/plain;charset=UTF-8").append(LINE_BREAK);

        // 返回数据
        response.append(EMPTY_LINE);
        response.append(content);

        log.debug("服务端发送响应：\n{}", response);
        return response.toString().getBytes(StandardCharsets.UTF_8);
    }
}
