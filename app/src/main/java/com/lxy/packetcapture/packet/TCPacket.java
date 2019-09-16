package com.lxy.packetcapture.packet;

import java.io.PipedOutputStream;
import java.lang.ref.PhantomReference;

/**
 * author；lixinyi
 * date:2019/8/12
 */

public class TCPacket {
    private final static int SOURCEPORT_BIT = 0;
    private final static int DESTINATIONPORT_BIT = 2;//占2字节
    private final static int SEQUENCE_BIT = 4;//序列号，占4字节
    private final static int ACKNOWLEDGEMENT_BIT = 8;//应答号
    private final static int HEADER_LENGTH_BIT = 12;//数据偏移 占4位
    private final static int TAG_BIT = 13;
    private final static int WINDOW_SIZE_BIT = 14;//窗口大小
    private final static int CHECK_SUM_BIT = 16;//占两个字节
    private final static int URGENT_POINTER_BIT = 18;//紧急指针，当URG为1时这里会进行描述

    public byte[] m_Data;
    public int m_Offset;

    public TCPacket(byte[] data,int offset){
        this.m_Data = data;
        this.m_Offset = offset;
    }

    //获取源端口
    public int getSourcePort(){
        return Packet.readShort(m_Data,m_Offset + SOURCEPORT_BIT);
    }

    //获取目的端口
    public int getDestinationPort(){
        return Packet.readShort(m_Data,m_Offset + DESTINATIONPORT_BIT);
    }

    public long getSequenceNumber(){
        int seq = Packet.readInt(m_Data,m_Offset + SEQUENCE_BIT);
        return (long)seq;
    }

    public long getAcknowledgementNumber(){
        int ack = Packet.readInt(m_Data,m_Offset + ACKNOWLEDGEMENT_BIT);
        return  (long)ack;
    }

    //获取TCP头部的长度 占4字节
    public int getHeaderLength(){
        return ((m_Data[m_Offset + HEADER_LENGTH_BIT] & 0xFF) >> 4) * 4;
    }

    //获取第13个字节的后六位
    public int getTag(){
        return m_Data[m_Offset + TAG_BIT] & 0x2F;
    }

    public boolean isURG(){
        return (getTag() >> 5) == 1;
    }

    public boolean isACK(){
        return ((getTag() >> 4) & 1) == 1;
    }

    public boolean isPSH(){
        return ((getTag() >>3) & 1) == 1;
    }

    public boolean isRST(){
        return ((getTag() >>2) & 1) == 1;
    }

    public boolean isSYN(){
        return ((getTag() >>1) & 1) == 1;
    }

    public boolean isFIN(){
        return (getTag() & 1) == 1;
    }

    public int getWindowSize(){
        return Packet.readShort(m_Data,m_Offset + WINDOW_SIZE_BIT);
    }

    public short getCheckSum(){
        return (short) Packet.readShort(m_Data,m_Offset + CHECK_SUM_BIT);
    }

    public void setCheckSum(short value){
        Packet.writeInt(m_Data,m_Offset + CHECK_SUM_BIT,value);
    }

    //当ARG=1时，使用此方法
    public int getUrgentPointer(){
        return Packet.readShort(m_Data,m_Offset + URGENT_POINTER_BIT);
    }

    public void setSourcePort(int port){
        Packet.writeShort(m_Data,m_Offset + SOURCEPORT_BIT, (short) port);
    }

    public void setDestinationport(int port){
        Packet.writeShort(m_Data,m_Offset + DESTINATIONPORT_BIT,(short) port);
    }
}
