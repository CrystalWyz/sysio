package cn.wyz.nettyIO;

public class MainThread {
    public static void main(String[] args) {
        // 启动器

        // 1. 创建IO Thread
        SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(3);
//        SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(3);
        // 2. 注册
        selectorThreadGroup.bind(9999);
    }
}
