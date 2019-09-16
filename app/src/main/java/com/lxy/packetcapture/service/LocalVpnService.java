package com.lxy.packetcapture.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.lxy.packetcapture.packet.IPacket;
import com.lxy.packetcapture.packet.Packet;
import com.lxy.packetcapture.packet.TCPacket;
import com.lxy.packetcapture.packet.UDPacket;
import com.lxy.packetcapture.tcp.LocalSocket;
import com.lxy.packetcapture.tcp.TcpServer;
import com.lxy.packetcapture.tools.LocalAddressUtils;
import com.lxy.packetcapture.tools.NatSession;
import com.lxy.packetcapture.tools.NatSessionManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by 李昕怡 on 2019/8/14
 */

public class LocalVpnService extends VpnService implements Runnable{
    public static final String TAG = "LocalVpnService";

    public static String VPN_ADDRESS = "10.0.0.2";
    private ParcelFileDescriptor mInstance;
    private FileInputStream in;
    private FileOutputStream out;
    private byte[] packet;
    private IPacket mIPacket;
    private TCPacket mTCPacket;
    private UDPacket mUDPacket;
    private int LOCAL_IP;
    public static LocalVpnService  INSTANCE;//实例
    private boolean IsRunning = false;
    private Thread mVpnThread;
    private TcpServer mTcpServer;

    public LocalVpnService(){
        packet = new byte[0xFFFF];
        mIPacket = new IPacket(packet,0);
        mTCPacket = new TCPacket(packet,20);
        mUDPacket = new UDPacket(packet,20);
        INSTANCE = this;
    }

    /**
     * 启动工作线程
     */
    @Override
    public void onCreate() {
        mVpnThread = new Thread(this,"LocalVpnThread");
        mVpnThread.start();
        setVpnRunningStaus(true);//设置IsRunning = true
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    /**
     * 停止VPN工作线程
     */
    @Override
    public void onDestroy() {
        if(mVpnThread != null){
            mVpnThread.interrupt();
        }

        super.onDestroy();
    }

    private void startVpn() throws IOException {
        this.mInstance = established();//获取文件操作符实例
        startStream();
    }

    private void startStream() throws IOException {
        int size = 0;
        in = new FileInputStream(mInstance.getFileDescriptor());//读取数据包
        out = new FileOutputStream(mInstance.getFileDescriptor());//写回响应数据包
        while(size != -1 && IsRunning){
            boolean hasWrite = false;//检测数据包是否成功变换地址
            size = in.read(packet);
            mIPacket.m_Data = packet;
            if(size > 0){
                //这里是否需要判断TcpServer是否关闭 给TcpServer设置一个Stopped标志
                hasWrite = onIPacketReceived(mIPacket,size);
            }

            if(!hasWrite){

            }
        }
        in.close();
        disconnectVPN();
    }

    private boolean onIPacketReceived(IPacket iPacket,int size) throws IOException {
        boolean hasWrite = false;

        switch (iPacket.getProtocol()){
            case IPacket.TCP:
                Log.e(TAG,"This is tcp!!!");
                hasWrite = onTcpPacketReceived(iPacket,size);
                break;
            case IPacket.UDP:
                onUdpPacketReceived(iPacket,size);
                break;
            default:
                break;
        }
        return hasWrite;
    }

    private boolean onTcpPacketReceived(IPacket iPacket,int size) throws IOException {
        boolean hasWrite = false;
        TCPacket tcPacket = mTCPacket;
        //设置tcpacket真正的offset
         tcPacket.m_Data = iPacket.m_Data;
        tcPacket.m_Offset = iPacket.getHeaderLength();
        Log.d(TAG,"tcPacket.offset:"+String.valueOf(tcPacket.m_Offset));
        if(tcPacket.getSourcePort() == mTcpServer.Port){//tcp数据包由tcpServer返回
            Log.d(TAG,"接收到回复的报文！");
            Log.d(TAG,"源端口："+tcPacket.getSourcePort());
            Log.d(TAG,"mTcpServer.Port："+mTcpServer.Port);
            Log.d(TAG,"目标端口："+tcPacket.getDestinationPort());
            Log.d(TAG,"目的IP："+Packet.ipIntToString(iPacket.getDeatinationIp()));
            Log.d(TAG,"源IP："+Packet.ipIntToString(iPacket.getSourceIP()));
            Log.d(TAG,"包的总长度："+iPacket.getTotalLength());
            NatSession natSession = NatSessionManager.getSession(tcPacket.getDestinationPort());
            if(natSession != null){
                iPacket.setSourceIp(iPacket.getDeatinationIp());//natSession.remoteIP
                tcPacket.setSourcePort(natSession.RemotePort);
                iPacket.setDestinationIp(LOCAL_IP);
                Log.d(TAG,"回复报文的ACK:"+tcPacket.isACK());
                Log.d(TAG,"回复报文的SYN:"+tcPacket.isSYN());
                Log.d(TAG,"是第几个数据包："+natSession.BytesSent);

                boolean checkValue = Packet.computeTCPChecksum(iPacket,tcPacket);
                Log.d(TAG, "这里计算数据包返回到tun的校验值："+String.valueOf(checkValue));
                out.write(iPacket.m_Data,iPacket.m_Offset,size);
            }
            else {
                Log.d(TAG,"natSession = null");
            }
        }else{
            int portKey = tcPacket.getSourcePort();
//            Log.e(TAG, String.valueOf(iPacket.getSourceIP()));
//            if(iPacket.getSourceIP() != Packet.ipStringToInt(VPN_ADDRESS)){
//                return false;
//            }
            if(tcPacket.isSYN() == false && tcPacket.isACK() == false)
                return false;
            Log.d(TAG,"sourcePort:"+tcPacket.getSourcePort());
            NatSession session = NatSessionManager.getSession(portKey);
            if(session == null || session.RemoteIP != iPacket.getDeatinationIp()
                    || session.RemotePort != tcPacket.getDestinationPort()){
                session = NatSessionManager.createSession(tcPacket.getSourcePort(),iPacket.getDeatinationIp(),
                                tcPacket.getDestinationPort());
                int localIP = iPacket.getSourceIP();
                Log.d(TAG,"localIP:"+Packet.ipIntToString(localIP));
                int remoteIP = iPacket.getDeatinationIp();
                String remoteIPString = Packet.ipIntToString(remoteIP);
                Log.e(TAG,"remoteIPString:"+remoteIPString);
                int remotePort = tcPacket.getDestinationPort();
                Log.e(TAG,"remotePort:"+remotePort);
                long seq = tcPacket.getSequenceNumber();
                long ack = tcPacket.getAcknowledgementNumber();
                boolean SYN = tcPacket.isSYN();
                Log.e(TAG,"------SYN:"+SYN);
                boolean ACK = tcPacket.isACK();
                Log.e(TAG,"------ACK:"+ACK);
                int totalLength = iPacket.getTotalLength();
                Log.e(TAG,"totalLength:"+totalLength);
            }
            session.LastNanoTime = System.nanoTime();
            session.PacketSent++;//计算当前数据包发送的是第几个 前两个与服务器建立连接

            int tcpDataSize=iPacket.getTotalLength()-tcPacket.getHeaderLength();
            if(session.PacketSent==2&&tcpDataSize==0){
                return false;
                //丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
            }

            //分析数据，找到host
            if(session.BytesSent==0&&tcpDataSize>10){
                int dataOffset=tcPacket.m_Offset+tcPacket.getHeaderLength();
                //String host=HttpHostHeaderParser.parseHost(tcPacket.m_Data, dataOffset, tcpDataSize);
//                if(host!=null){
//                    session.RemoteHost=host;
//                }
            }

            //转发给本地服务器
            iPacket.setSourceIp(iPacket.getDeatinationIp());
            Log.i(TAG,Packet.ipIntToString(iPacket.getDeatinationIp()));
            iPacket.setDestinationIp(Packet.ipStringToInt(VPN_ADDRESS));
            tcPacket.setDestinationport(mTcpServer.Port);
            final int port = mTcpServer.Port;

            Packet.computeTCPChecksum(iPacket,tcPacket);
            Log.d(TAG, String.valueOf(Packet.computeTCPChecksum(iPacket,tcPacket)));
            //out.write(iPacket.m_Data,iPacket.m_Offset,size);
            LocalSocket localSocket = new LocalSocket(VPN_ADDRESS,mTcpServer.Port,iPacket.m_Data);
            localSocket.start();
        }
        hasWrite = true;
        return hasWrite;
    }

    private void onUdpPacketReceived(IPacket iPacket,int size){

    }

    /**
     * 此线程主要开启两个服务器
     */
    @Override
    public void run() {
        waitUtilPrepared();
        LOCAL_IP = Packet.ipStringToInt(LocalAddressUtils.getIPAddress(this));
        Log.d(TAG,"LOCAL_IP:"+LocalAddressUtils.getIPAddress(this));
        try {
            mTcpServer = new TcpServer(0);
            mTcpServer.start();

            while (IsRunning){
                startVpn();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    private void waitUtilPrepared(){
        while(prepare(this) != null){
            try{
                Thread.sleep(100);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private ParcelFileDescriptor established(){
        Builder builder = new Builder();
        Log.d(TAG, String.valueOf(LOCAL_IP));
        mInstance = builder.addAddress(VPN_ADDRESS,32)
                .addRoute("0.0.0.0",0)
                .setSession("LocalVpnService")
                .establish();
        return mInstance;
    }

    public void disconnectVPN(){
        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(mInstance != null) {
            try {
                mInstance.close();
                mInstance = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.out = null;
    }

    private synchronized void close(){
        disconnectVPN();
        if(mTcpServer != null){
            mTcpServer.stop();
            mTcpServer = null;
        }

        stopSelf();
        setVpnRunningStaus(false);
    }

    public boolean vpnRunningStatus(){
        return IsRunning;
    }

    /**
     * 设置mVpnThread的运行状态
     */
    public void setVpnRunningStaus(boolean isRunning){
        this.IsRunning = isRunning;
    }
}
