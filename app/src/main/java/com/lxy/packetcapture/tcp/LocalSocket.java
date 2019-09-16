package com.lxy.packetcapture.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by 李昕怡 on 2019/9/11
 * 主要解决FileOutputStream.write()写回到tun中的问题
 * while(true)轮询监听不到事件
 * 原因：根据log打印发现数据根本没有被发送出去 好像是写回tun中 tcp/ip协议栈又将它发送了回来
 */

public class LocalSocket implements Runnable{
    private SocketChannel mSocketChannel;
    private Selector mSelector;
    public static byte[] packet;
    private Thread mLocalThread;

    public LocalSocket(String LOCAL_IP,int port,byte[] packet) throws IOException {
        this.packet = packet;
        mSocketChannel = SocketChannel.open();
        mSocketChannel.configureBlocking(false);
        mSelector = Selector.open();
        mSocketChannel.connect(new InetSocketAddress(LOCAL_IP,port));
        mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
    }

    public void start(){
        mLocalThread = new Thread("localThread");
        mLocalThread.start();
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel.isConnectionPending()){//此通道是否正在进行连接操作
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.write(ByteBuffer.wrap(packet));
        channel.register(mSelector,SelectionKey.OP_READ);
    }

    @Override
    public void run() {
        try {
            mSelector.select();
            Set<SelectionKey> selectionKeys = mSelector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (true){
                SelectionKey key = iterator.next();
                if(key.isConnectable()){
                    connect(key);
                }else if(key.isReadable()){

                }else if(key.isWritable()){

                }
                iterator.remove();//移除事件，避免重复
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        try{
            if(mSelector != null){
                mSelector.close();
                mSelector = null;
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        try{
            if(mSocketChannel != null){
                mSocketChannel.close();
                mSocketChannel = null;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
