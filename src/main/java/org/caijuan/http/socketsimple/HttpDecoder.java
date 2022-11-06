package org.caijuan.http.socketsimple;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static org.caijuan.http.common.HttpConstant.*;

// GET / HTTP/1.1
// token: caijuan
// Content-Type: text/plain
// User-Agent: PostmanRuntime/7.29.2
// Accept: */*
// Cache-Control: no-cache
// Postman-Token: b9e74900-690a-4216-80f2-019bbcfde7b0
// Host: localhost:8080
// Accept-Encoding: gzip, deflate, br
// Connection: keep-alive
// Content-Length: 47
//
// 我是菜卷
// 我在请求有里添加了 token


@Slf4j
public class HttpDecoder {

    /**
     * 解码
     *
     * @param buffer 字节数组
     * @param length 数据长度
     * @return 转换后的 Java Map 对象
     */
    public Map<String, String> deEncode(byte[] buffer, int length) {

        StringBuffer httpContent = new StringBuffer();
        httpContent.append(new String(buffer, 0, length));

        log.debug("服务端接收请求：\n{}", httpContent);
        if (httpContent.length() <= 0) {
            log.info("数据不符合 http 格式，处理结束！");
            return null;
        }

        Map<String, String> request = new HashMap<>();
        String[] content = httpContent.toString().split(EMPTY_LINE);
        String[] lines = content[0].split(LINE_BREAK);
        // 请求行
        String header = lines[0];
        request.put("Header", header);
        String[] headerInfo = header.split(SPACE);

        request.put("Method", headerInfo[0]);
        request.put("Uri", headerInfo[1]);
        request.put("Protocol", headerInfo[2]);

        // 请求头
        for (int i = 1; i < lines.length; i++) {
            String[] line = lines[i].split(LINER_SPLIT);
            request.put(line[0], line[1]);
        }
        String url = request.get("Host") + request.get("Uri");
        request.put("Url", url);

        // 数据
        if (content.length > 1) {
            request.put("Data", content[1]);
        }
        return request;
    }
}
