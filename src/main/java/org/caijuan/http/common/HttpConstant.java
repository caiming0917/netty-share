package org.caijuan.http.common;

/**
 * @see <a href="https://www.jianshu.com/p/8fe93a14754c">HTTP协议</a>
 */
public class HttpConstant {
    public static final String LINE_BREAK = "\r\n";
    public static final String LINER_SPLIT = ": ";
    public static final String EMPTY_LINE = "\r\n\r\n";
    public static final String SPACE = "\\s";

    public static final String STATUS_LINE = "HTTP/1.1 200 OK" + LINE_BREAK;

    // 响应的基础头信息
    public static final String RESPONSE_HEADER =
            "Content-Type: text/html;charset=utf-8" + LINE_BREAK +
            "Vary: Accept-Encoding" + LINE_BREAK;

    // 响应的基础信息
    public static final String BASIC_RESPONSE = STATUS_LINE + RESPONSE_HEADER;
}
