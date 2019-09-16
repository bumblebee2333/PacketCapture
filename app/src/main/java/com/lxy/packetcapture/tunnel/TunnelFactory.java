package com.lxy.packetcapture.tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * created by 李昕怡 on 2019/8/30
 * ps:这个类主要是因为在new Tunnel的时候 需要实现Tunnel的抽象方法 所以多出一个类来帮助实现
 */

public class TunnelFactory {

    public static Tunnel cteateLocalTunnel(SocketChannel socketChannel, Selector selector) throws IOException {
        return new MyTunnel(socketChannel,selector);
    }

    public static Tunnel createRemoteTunnel(InetSocketAddress inetSocketAddress,Selector selector) throws IOException {
        return new MyTunnel(inetSocketAddress,selector);
    }
}
