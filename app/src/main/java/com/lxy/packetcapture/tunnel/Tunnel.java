package com.lxy.packetcapture.tunnel;

import android.util.Log;

import com.lxy.packetcapture.service.LocalVpnService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * created by 李昕怡 on 2019/8/28
 * ps:Tunnel主要是创建localTunnel和remoteTunnel 并将两个tunnel绑定 进行两个隧道之间数据的通信
 */

public abstract class Tunnel {
    public static final String TAG = "Tunnel";

    private SocketChannel mSocketChannel;
    private Selector mSelector;
    private Tunnel mBrotherChannel;//本地channel和远程channel都需要和相应的channel绑定
    private InetSocketAddress mServerIP;//和远程真正的服务器连接时 远程隧道连接的地址
    private InetSocketAddress mDestAddress;
    public static final ByteBuffer BUFFER = ByteBuffer.allocate(20000);
    private ByteBuffer mRemainBuffer;//这个buffer主要是缓存一次不能写到通道中的剩余数据

    public abstract void afterReceived(ByteBuffer buffer) throws Exception;//在隧道读取到数据后可进行数据的解密
    public abstract void beforeSend(ByteBuffer buffer) throws Exception;//在写入到隧道后对数据的加密等
    public abstract boolean isTunnelEstablished();

    public Tunnel(){}

    //传入localTunnel的信息 建立本地tunnel
    public Tunnel(SocketChannel socketChannel,Selector selector){
        this.mSocketChannel = socketChannel;
        this.mSelector = selector;
    }

    //传入remoteTunnel信息 建立远程tunnel
    public Tunnel(InetSocketAddress inetSocketAddress,Selector selector) throws IOException {
        mSocketChannel = SocketChannel.open();
        mSocketChannel.configureBlocking(false);
        this.mServerIP = inetSocketAddress;
        this.mSelector = selector;
    }

    public void setBrotherTunnel(Tunnel brotherTunnel){
        this.mBrotherChannel = brotherTunnel;
    }

    public void connectServer(InetSocketAddress destAddress) throws IOException {
        //保护socket不走vpn
        if(LocalVpnService.INSTANCE.protect(mSocketChannel.socket())){
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);//注册连接事件
            mSocketChannel.connect(mServerIP);//和服务器连接
            Log.d(TAG,"和服务器连接成功！！！");
        }else {
            Log.d(TAG,"远程tunnel和服务器连接失败！！！");
        }
    }

    public void onConnectable(SelectionKey key,Selector selector){
        SocketChannel channel = (SocketChannel) key.channel();
        if(channel == null){

        }
        try {
            if(channel.isConnectionPending()){
                channel.finishConnect();//此时mSocketChannel还处于阻塞状态 知道连接完成或失败才返回
                //onTunnelEstablished(channel);//socket设置为不阻塞且可读
                beginReceive(channel,selector);
            }else {
                Log.d(TAG,"和服务器连接失败!!!");
                this.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.close();
        }
    }

    //两个通道之间相互通信 从一个通道读取数据并写入另一个通道
    public void onReadable(SelectionKey key){
        try {
            ByteBuffer buffer = BUFFER;
            buffer.clear();
            int bytesRead = mSocketChannel.read(buffer);
            if(bytesRead > 0){
                buffer.flip();//翻转缓冲区 limit = position;position = 0
                afterReceived(buffer);//这里是对读取的数据包中的数据进行解密的操作等
                if(buffer.hasRemaining()){//return position < limit;
                    mBrotherChannel.beforeSend(buffer);
                    if(!mBrotherChannel.write(buffer,true)){
                        key.cancel();//这里是取消读事件 因为兄弟隧道不能一次写入全部发送的数据
                    }
                }
            }else if (bytesRead < 0){
                this.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.close();
        }
    }

    public void onWriteable(SelectionKey key){
        try{
            if(this.write(mRemainBuffer,false)){
                key.cancel();//取消写事件 因为事件写入完毕 需要接受响应事件了
                if(isTunnelEstablished()){
                    //mBrotherChannel.beginReceive();//通知兄弟可以接受数据了 改为读事件
                }else {
                    //this.beginReceive();//接受代理服务器响应数据
                }
            }
        }catch (IOException e){
            e.printStackTrace();
            this.close();
        }
    }

    public boolean write(ByteBuffer buffer,boolean mRemained) throws ClosedChannelException {
        int bytesSent;
        try{
            while(buffer.hasRemaining()){
                bytesSent = mSocketChannel.write(buffer);
                if(bytesSent == 0)
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        if(buffer.hasRemaining()){//position < limit 意味着一次没有将数据写完
            if(mRemained){//true 还有剩余数据 写入mRemainBuffer中 并将事件注册为可写入
                mRemainBuffer = ByteBuffer.allocate(buffer.capacity());
                mRemainBuffer.clear();
                mRemainBuffer.put(buffer);
                mRemainBuffer.flip();//回到buffer position为0的地方
                mSocketChannel.register(mSelector,SelectionKey.OP_WRITE);
            }
            return false;
        }else {//都发送成功了
            return true;
        }
    }

    protected void beginReceive(SocketChannel channel,Selector selector) throws IOException {
        if(channel.isBlocking()){//本地 远程隧道工作之前都要设置不堵塞
            channel.configureBlocking(false);
        }
        if(selector != null){
            channel.register(selector,SelectionKey.OP_READ);//注册读事件
        }else {
            Log.e(TAG,"channel is null");
        }

    }

//    protected void onTunnelEstablished(SocketChannel channel) throws IOException {
//        this.beginReceive(channel,);//开始接受数据
//        mBrotherChannel.beginReceive(channel);//绑定的兄弟隧道也开始接受数据
//    }

    public void close(){
        closeResource(true);
    }

    public void closeResource(boolean closeBrother){
        try {
            mSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(mBrotherChannel!=null && closeBrother){
            mBrotherChannel.closeResource(false);//兄弟也释放资源
        }

        mSocketChannel = null;
        mBrotherChannel = null;
        mSelector = null;
        mRemainBuffer = null;
    }
}
