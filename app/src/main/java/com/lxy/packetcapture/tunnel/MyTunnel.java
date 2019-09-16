package com.lxy.packetcapture.tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class MyTunnel extends Tunnel {
    private static MyTunnel myTunnel;

    public static MyTunnel getInstance(){
        if(myTunnel == null){
            synchronized (MyTunnel.class){
                if(myTunnel == null){
                    myTunnel = new MyTunnel();
                }
            }
        }
        return myTunnel;
    }

    public MyTunnel(){
        super();
    }

    public MyTunnel(InetSocketAddress inetSocketAddress, Selector selector) throws IOException {
        super(inetSocketAddress, selector);
    }

    public MyTunnel(SocketChannel socketChannel,Selector selector) throws IOException{
        super(socketChannel,selector);
    }

    @Override
    public void afterReceived(ByteBuffer buffer) throws Exception {

    }

    @Override
    public void beforeSend(ByteBuffer buffer) throws Exception {

    }

    /**
     * 这个方法主要是在数据是否发送给兄弟 和 写数据时判断是否写给兄弟
     */
    @Override
    public boolean isTunnelEstablished() {
        return true;
    }
}
