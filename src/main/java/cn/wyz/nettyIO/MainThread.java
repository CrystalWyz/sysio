package cn.wyz.nettyIO;

public class MainThread {
    public static void main(String[] args) {
        // 启动器

        // 1. 创建IO Thread
        SelectorThreadGroup boss = new SelectorThreadGroup(3);
        SelectorThreadGroup work = new SelectorThreadGroup(3);
        boss.setWorkGroup(work);
        // 2. 注册
        boss.bind(9999);
    }
}
