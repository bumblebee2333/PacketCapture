package com.lxy.packetcapture.packet;

/**
 * author:lixinyi
 * date:2019/8/9
 */

public class IPacket {
    private final static String TAG = "IPacket";

    public final static int VERSION_BIT = 0;
    public final static int HEADER_LENGTH_BIT = 0;
    public final static int TOS_BIT = 1;//区分服务从第一个字节开始
    public final static int TOTAL_LENGTH_BIT = 2;//总长度占第二 三字节
    public final static int SEQUENCE_BIT = 4;//序列号 占四 五字节
    public final static int FLAGS = 6;//第六个字节的前三位 表示是否分包
    public final static int OFFSET_BIT = 6;//片偏移 标识每一个分段相对于原始数据的位置
    public final static int TTL_BIT = 8;//生存时间 经过路由器减一
    public final static int PROTOCOL_BIT = 9;//协议号
    public final static int HEADER_CHECKSUM_BIT = 10;//首部校验和 占10 11字节
    public final static int SOURCE_IP_BIT = 12;//源地址 四字节
    public final static int DEST_IP_BIT = 16;//目标地址 四字节

    //可能会出现的协议号
    public final static int ICMP = 1;
    public final static int IGMP = 2;
    public final static int TCP = 6;
    public final static int UDP = 17;
    public final static int OSPF = 89;

    public byte[] m_Data;//传进来的IP数据
    public int m_Offset;//IP包的偏移量为0 根据网络层的IP数据包的总体结构分布可知

    public IPacket(byte[] data,int offset){
        this.m_Data = data;
        this.m_Offset = offset;
    }

    //获取IP报文的协议版本号
    //版本号占前四位  & 0Xff意味去掉后8位之前的数 再向右移4位得到版本号
    public int getVersion(){
        return (m_Data[m_Offset + VERSION_BIT] & 0XFF) >> 4;
    }

    //获取首部长度 单位为字节
    public int getHeaderLength(){
        return (m_Data[m_Offset + HEADER_LENGTH_BIT] & 0xF) * 4;
    }

    //获取区分服务字段 一般情况不使用
    public int getTos(){
        return m_Data[m_Offset + TOS_BIT] & 0XFF;
    }

    //获取总长度
    public int getTotalLength(){
        return Packet.readShort(m_Data,m_Offset + TOTAL_LENGTH_BIT) & 0xFFFF;
    }

    //获取序列号
    //标识发送方发送的每一个数据包，如果未发生分片则依次加一；若发生分片，分片后的数据包都采用同一个序列号
    public int getIdentification(){
        return Packet.readShort(m_Data,m_Offset + SEQUENCE_BIT) & 0xFFFF;
    }

    /**
     * 获取标志 数据包是否发生分片
     * 010 未发生分片
     * 000 发生分片且是最后一个分片
     * 001 发生分片且后续还有分片
     * 共占三位：第一位保留 未使用；第二位DF;第三位MF
     */
    public int getFlags(){
        return (m_Data[m_Offset + FLAGS]) >> 5;
    }

    /**
     * 是否发生分片
     * 0 发生分片
     * 1 未发生分片
     */
    public boolean isDF(){
        if(getFlags() >> 1 == 1)
            return false;
        else
            return true;
    }

    /**
     * 是否还有分片:001
     */
    public boolean isMF(){
        if((getFlags() & 1) == 1 && isDF())
            return true;
        else
            return false;
    }

    //返回片偏移
    public int getOffsetByte(){
        return Packet.readShort(m_Data, m_Offset + OFFSET_BIT) * 8;
    }

    public int getTTL(){
        return m_Data[m_Offset + TTL_BIT] & 0xFF;
    }

    //获取上层协议
    public int getProtocol(){
        return m_Data[m_Offset + PROTOCOL_BIT] & 0xFF;
    }

    //获取首部校验和
    public int getHeaderChecksum(){
        return Packet.readShort(m_Data,m_Offset + HEADER_CHECKSUM_BIT);
    }

    //获得int型的源地址
    public int getSourceIP(){
        return Packet.readInt(m_Data,m_Offset + SOURCE_IP_BIT);
    }

    public void setSourceIp(int ip){
        Packet.writeInt(m_Data,m_Offset + SOURCE_IP_BIT,ip);
    }

    public int getDeatinationIp(){
        return Packet.readInt(m_Data,m_Offset + DEST_IP_BIT);
    }

    public void setDestinationIp(int ip){
        Packet.writeInt(m_Data,m_Offset + DEST_IP_BIT,ip);
    }

    //检验求和
    private int getCheckSum(){
        int headerLength = getHeaderLength();
        int sum = 0;

        for(int i=m_Offset;i<m_Offset + headerLength;i += 2){
            sum += Packet.readInt(m_Data,m_Offset+i);
        }

        //如果报头长度为奇数 那么需要加上最后一个字节 这里为什么要左移8位还不太懂
        if((m_Offset + headerLength) % 2 > 0){
            sum += (m_Data[m_Offset + headerLength - 1] & 0xFF) << 8;
        }

        //计算校验和16位之上是否有进位，若有进位则循环折叠求和，直到16位之上没有进位
        while ((sum >> 16) > 0){
            sum = (sum >> 16) + (sum & 0xFFFF);
        }

        return sum;
    }

    //检查头部校验和 为0则正常
    public boolean checkSum(){
        return ((~ getCheckSum()) & 0xFFFF) == 0;
    }
}
