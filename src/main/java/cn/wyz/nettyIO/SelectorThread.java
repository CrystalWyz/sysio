package cn.wyz.nettyIO;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wnx
 */
public class SelectorThread implements Runnable {
    // 一个selector对应一个thread
    // 并发情况下，每个客户端只绑定到其中一个selector(分治)
    Selector selector = null;
    LinkedBlockingQueue<Channel> channelQueue = new LinkedBlockingQueue<>();
    SelectorThreadGroup selectorThreadGroup;
    public SelectorThread(SelectorThreadGroup selectorThreadGroup) {
        try {
            this.selectorThreadGroup = selectorThreadGroup;
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // loop
        while (true) {
            try {
                int ioEventNums = selector.select();
                if (ioEventNums > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {

                        }
                    }
                }

                // 处理其他 task
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readHandler(SelectionKey key) {
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        byteBuffer.clear();
        while (true) {
            try {
                int num = client.read(byteBuffer);
                if (num > 0) {
                    byteBuffer.flip();
                    while (byteBuffer.hasRemaining()) {
                        client.write(byteBuffer);
                    }
                    byteBuffer.clear();
                } else  if (num == 0) {
                    break;
                } else {
                    // 客户端断开
                    key.cancel();
                    break;
                }

                // 处理额外的task
                Channel channel = channelQueue.take();
                if (channel instanceof ServerSocketChannel) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                } else if (channel instanceof SocketChannel) {
                    SocketChannel socketChannel = (SocketChannel) channel;
                    ByteBuffer clientByteBuffer = ByteBuffer.allocateDirect(4096);
                    socketChannel.register(selector, SelectionKey.OP_READ, clientByteBuffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void acceptHandler(SelectionKey key) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);

            // choose a selector and register!!
            selectorThreadGroup.nextSelector(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
