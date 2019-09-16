package com.lxy.packetcapture.packet;

/**
 * author:lixinyi
 * date:2019/8/9
 */

public class Packet {

    public static int readShort(byte[] data,int offset){
        int num = ((data[offset] & 0xFF) << 8 | data[offset+1] & 0xFF) & 0xFFFF;
        return num;
    }

    //获取每一位的地址并将它们右移 或就是将每8位的地址整合在一起
    public static int readInt(byte[] data,int offset){
        int num = ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
        return num;
    }

    public static void writeInt(byte[] data,int offset,int value){
        data[offset] = (byte) (value >> 24);
        data[offset+1] = (byte)(value >> 16);
        data[offset+2] = (byte)(value >> 8);
        data[offset+3] = (byte)(value);
    }

    public static void writeShort(byte[] data,int offset,short value){
        data[offset] = (byte)(value >> 8);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    //将ip地址转化为x.x.x.x的形式
    public static String ipIntToString(int ip){
        return String.format("%s.%s.%s.%s",(ip >> 24) & 0x00FF,(ip >> 16) & 0x00FF,
                (ip >> 8) & 0xFF,ip & 0xFF);
    }

    //将x.x.x.x样子的IP地址转化成32位二进制数字地址
    public static int ipStringToInt(String ip){
        String[] strings = ip.split("\\.");
        int num = (Integer.parseInt(strings[0]) << 24) |
                (Integer.parseInt(strings[1]) << 16) |
                (Integer.parseInt(strings[2]) << 8) |
                (Integer.parseInt(strings[3]));
        return num;
    }

    //计算TCP的校验和 TCP伪首部+TCP首部+TCP数据
    public static boolean computeTCPChecksum(IPacket iPacket,TCPacket tcPacket){
        //TCP首部+TCP数据 = IP整体长度 - IP首部
        int tcp_length = iPacket.getTotalLength() - iPacket.getHeaderLength();
        if(tcp_length < 0)
            return false;
        //计算 TCP伪首部 = 源IP地址 + 目标IP地址 + 协议号 + TCP包长度
        long sum = getPseudoHeadLength(iPacket,tcp_length);

        short oldChecksum = tcPacket.getCheckSum();
        tcPacket.setCheckSum((short) 0);//将校验和置0

        short newChecksum = checksum(sum,tcPacket.m_Data,tcPacket.m_Offset,tcp_length);

        tcPacket.setCheckSum(newChecksum);
        return oldChecksum == newChecksum;
    }

    //计算伪首部长度
    public static long getPseudoHeadLength(IPacket iPacket,int len){
        byte[] buf = iPacket.m_Data;
        int offset = iPacket.m_Offset + IPacket.SOURCE_IP_BIT;
        long sum = 0;
        //ip包中地址占8字节 计算地址
        int address = 8;
        while (address > 1){
            sum += readShort(buf,offset) & 0xFFFF;
            offset += 2;
            address -= 2;
        }
        if(address > 1){//可能会有剩余的字节
            sum += (buf[offset] & 0xFF) << 8;
        }
        //在此基础上计算协议号
        sum += iPacket.getProtocol() & 0xFF;
        //在此基础上计算TCP包长度
        sum += len;
        return sum;
    }

    //计算校验和
    public static short checksum(long sum,byte[] buf,int offset,int len){
        while (len > 1){
            sum += readShort(buf,offset) & 0xFFFF;
            offset += 2;
            len -= 2;
        }
        if(len > 0){
            sum += (buf[offset] & 0xFF) << 8;
        }

        while((sum >> 16) > 0){
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return (short) ~sum;
    }
}
