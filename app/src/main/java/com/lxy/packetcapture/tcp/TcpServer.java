package com.lxy.packetcapture.tcp;

import android.util.Log;

import com.lxy.packetcapture.packet.IPacket;
import com.lxy.packetcapture.packet.Packet;
import com.lxy.packetcapture.packet.TCPacket;
import com.lxy.packetcapture.tools.NatSession;
import com.lxy.packetcapture.tools.NatSessionManager;
import com.lxy.packetcapture.tunnel.MyTunnel;
import com.lxy.packetcapture.tunnel.Tunnel;
import com.lxy.packetcapture.tunnel.TunnelFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by 李昕怡 on 2019/8/15
 * ps:tcp服务器 当虚拟网卡接收到数据后 得到请求的数据 此时需要发送到远端服务器得到响应的数据
 * 因此需要一个中转服务器和外界建立连接
 */

public class TcpServer implements Runnable{
    public static final String TAG = "TcpServer";
    private ServerSocketChannel mServerSocketChannel;
    private Selector mSelector;
    private Thread mServerThread;

    public static int Port;

    public TcpServer(int port) throws IOException {
        this.mSelector = Selector.open();//打开selector 监听通道中的事件
        this.mServerSocketChannel = ServerSocketChannel.open();//打开服务器接收通道 和本地发来的数据包进行通信
        this.mServerSocketChannel.configureBlocking(false);//设定通道不阻塞
        this.mServerSocketChannel.socket().bind(new InetSocketAddress(port));//和当前服务器的端口绑定 这里的port为0 目的是让系统给定一个随机的不被占用的端口号
        this.mServerSocketChannel.register(this.mSelector, SelectionKey.OP_ACCEPT);//注册接收就绪事件
        this.Port = this.mServerSocketChannel.socket().getLocalPort();//为服务器随机分配端口号
        //Log.d(TAG, String.valueOf(mServerSocketChannel.socket().getInetAddress()));
        //Log.d(TAG, String.valueOf(mServerSocketChannel.socket().getLocalSocketAddress()));
        Log.d(TAG, "本地服务器端口号："+String.valueOf(Port));
    }

    public void start(){
        mServerThread = new Thread(this);
        mServerThread.setName("TcpServerThread");
        mServerThread.start();
    }

    public void stop(){
        if(mSelector != null){
            try {
                mSelector.close();
                mSelector = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(mServerSocketChannel != null){
            try {
                mServerSocketChannel.close();
                mServerSocketChannel = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //采用轮询的方式监听selector上是否有需要处理的事件
    @Override
    public void run() {
        Log.d(TAG,"进入TcpServer的run方法。。。");
        try {
            //轮询访问selector
            while(true){
                Log.d(TAG,"进入了while循环 监听事件。。。。");
                int select = mSelector.select();//当注册事件时 会一直堵塞
                if(select == 0){
                    Thread.sleep(100);
                    continue;
                }
                Set<SelectionKey> keys = mSelector.selectedKeys();//选择器选出事件集合
                if(keys.isEmpty()){
                    Log.d(TAG,"事件集合为空。。。");
                }
                Log.d(TAG, "keys集合的大小:"+String.valueOf(keys.size()));
                Iterator<SelectionKey> it = keys.iterator();//迭代器
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    if(key.isValid()){//此事件是否有效
                        if(key.isAcceptable()){//转备好和新的SocketChannel连接
                            onAccepted(key);
                        }else if(key.isConnectable()){//是否完成套接字连接操作
                            //检索附件
                            MyTunnel.getInstance().onConnectable(key,mSelector);
                            //((Tunnel)key.attachment()).onConnectable(key);//attachment返回类型为object 强转为Tunnel 调用其中的方法
                            Log.d(TAG,"I am connecting!!!");
                        }else if(key.isReadable()){//是否可以读
                            ((Tunnel)key.attachment()).onReadable(key);
                            Log.d(TAG,"I am reading!!!");
                        }else if(key.isWritable()){//是否可写
                            ((Tunnel)key.attachment()).onWriteable(key);
                            Log.d(TAG,"I am writing!!!");
                        }
                    }
                    it.remove();//此事件若被使用 则从集合中删除 以免重复利用
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            stop();
            Log.d(TAG,"TcpServer thread exited!!!");
        }
    }


    private void onAccepted(SelectionKey key){
        Tunnel localTunnel = null;
        Tunnel remoteTunnel = null;

        try {
            SocketChannel localChannel = mServerSocketChannel.accept();//监听接收到的socketchannel
            localTunnel = TunnelFactory.cteateLocalTunnel(localChannel,mSelector);//创建localTunnel

            InetSocketAddress destAddress = getDestAddress(localChannel);
            if(destAddress != null){
                Log.d(TAG,"I am accepting!!!");
                remoteTunnel = TunnelFactory.createRemoteTunnel(destAddress,mSelector);
                remoteTunnel.setBrotherTunnel(localTunnel);//关联本地兄弟
                localTunnel.setBrotherTunnel(remoteTunnel);//关联远程兄弟
                remoteTunnel.connectServer(destAddress);//远程隧道和服务器连接
            }else {
                Log.d(TAG,"无法和远程隧道建立连接！！！");
                localTunnel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"和远程隧道建立连接时抛出异常！！！");
            if(localTunnel!=null){
                localTunnel.close();
            }
        }
    }

    //这个方法主要是通过localTunnel来获取应用发出的端口号 再用natsession找到对应的RemotePort
    InetSocketAddress getDestAddress(SocketChannel localChannel){
        IPacket packet = new IPacket(LocalSocket.packet,0);
        TCPacket tcPacket = new TCPacket(LocalSocket.packet,packet.getHeaderLength());
        int portKey = tcPacket.getSourcePort();
        //int portKey = localChannel.socket().getPort();
        Log.d(TAG, "远端："+String.valueOf(portKey));
        int key = localChannel.socket().getLocalPort();
        Log.d(TAG,"本地："+key);
        Log.d(TAG, String.valueOf(NatSessionManager.sessions.size()));
        NatSession session = NatSessionManager.getSession(portKey);
        if(session != null){
            Log.d(TAG, Packet.ipIntToString(session.RemoteIP));
            Log.d(TAG, String.valueOf(session.RemotePort));
            InetSocketAddress remoteAddress = new InetSocketAddress(Packet.ipIntToString(session.RemoteIP),session.RemotePort);
            return remoteAddress;
        }
        else {
            return null;
        }
    }
}
