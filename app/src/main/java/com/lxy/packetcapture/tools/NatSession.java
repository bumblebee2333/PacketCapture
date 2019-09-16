package com.lxy.packetcapture.tools;

/**
 * created by 李昕怡 on 2019/8/27
 * 路由表 保存源端口 目标端口 目标IP
 */

public class NatSession {
    public int localPort;
    public int RemoteIP;
    public int RemotePort;
    public String RemoteHost;//远端主机名
    public int BytesSent;
    public int PacketSent;//记录数据包的发送数目 例如tcp数据 前两个数据包是和服务器建立连接请求的数据包
    public long LastNanoTime;//以纳米为单位的时间 记录这个是为了根据数据存在的时间清除数据集合 以免造成不必要的浪费
}
