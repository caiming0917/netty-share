package org.caijuan.http.niosimpe;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.caijuan.http.common.HttpConstant.BASIC_RESPONSE;
import static org.caijuan.http.common.HttpConstant.LINE_BREAK;

/**
 * @see <a href="https://blog.csdn.net/futao__/article/details/107346195">NIO实现HTTP服务器</a>
 * <p>
 * NIO实现HTTP服务器
 */
@Slf4j
public class NioHttpServer {


    public static void main(String[] args) {
        new NioHttpServer().start();
    }

    public void start() {
        try {
            // 0.配置 socket and 绑定端口
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            // SocketChannel 设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            // 绑定端口
            serverSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            // 将 SocketChannel 注册到 selector 选择器上
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 1.循环监听
            while (true) {
                if (selector.select() == 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    handleSelectKey(selectionKey, selector);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void handleSelectKey(SelectionKey selectionKey, Selector selector) {
        // 连接
        if (selectionKey.isAcceptable()) {
            accept(selectionKey, selector);
            return;
        }
        // 可读
        if (selectionKey.isReadable()) {
            read(selectionKey, selector);
        }
    }

    private void read(SelectionKey selectionKey, Selector selector) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 4);
            // readBuffer.clear();
            // 3.input : 读取数据
            while (socketChannel.read(readBuffer) > 0) ;
            readBuffer.flip();

            // 4.计算
            // 4.1 序列化解码
            String request = String.valueOf(StandardCharsets.UTF_8.decode(readBuffer));
            // 4.2 业务处理
            log.info("接收到浏览器发来的数据:\n{}", request);
            if (StringUtils.isBlank(request)) {
                selectionKey.cancel();
                selector.wakeup();
                return;
            }
            String content = "hello";
            // 4.3 序列化编码
            ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
            StringBuffer response = new StringBuffer();
            response.append(BASIC_RESPONSE)
                    .append("Server: ServerBaseNIO/1.1").append(LINE_BREAK)
                    .append("content-length: ").append(content.getBytes().length).append(LINE_BREAK)
                    .append(LINE_BREAK)
                    .append(content);
            buffer.put(response.toString().getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            // 5.output : 返回数据
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
            // 5.断开连接
            selectionKey.cancel();
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(SelectionKey selectionKey, Selector selector) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        try {
            // 2 建立连接
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            log.debug("客户端[{}]接入", socketChannel.socket().getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
 