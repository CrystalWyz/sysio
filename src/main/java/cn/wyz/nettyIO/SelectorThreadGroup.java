package cn.wyz.nettyIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wnx
 */
public class SelectorThreadGroup {

    SelectorThread[] sts;
    ServerSocketChannel serverSocketChannel = null;
    AtomicInteger xid = new AtomicInteger(0);

    SelectorThreadGroup workGroup = this;

    public void setWorkGroup(SelectorThreadGroup workGroup) {
        this.workGroup = workGroup;
    }

    public SelectorThreadGroup(int threadNum) {
        sts = new SelectorThread[threadNum];
        for (int i = 0; i < sts.length; i++) {
            sts[i] = new SelectorThread(this);

            new Thread(sts[i]).start();
        }
    }

    public void bind(int port) {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));

            // 选择一个selector注册
            nextSelector(serverSocketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void nextSelector(Channel channel) {
        try {
            SelectorThread next;
            if (channel instanceof ServerSocketChannel) {
                next = next();
                next.selectorThreadGroup = workGroup;
            } else {
                next = nextWorker();
            }
            // 线程间优雅的通信
            next.channelQueue.put(channel);
            // 唤醒线程
            next.selector.wakeup();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SelectorThread next() {
        int index = xid.getAndIncrement() % sts.length;
        return sts[index];
    }

    private SelectorThread nextWorker() {
        int index = xid.getAndIncrement() % workGroup.sts.length;
        return workGroup.sts[index];
    }
}

