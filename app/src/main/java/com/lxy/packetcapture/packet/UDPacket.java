package com.lxy.packetcapture.packet;

/**
 * Created by 李昕怡 on 19/8/14
 */

public class UDPacket {

    public static final int SOURCE_PORT_BIT = 0;
    public static final int DESTINATION_PORT_BIT = 2;
    public static final int TOLAL_LENGTH_BIT = 4;
    public static final int CHECK_SUM_BIT = 6;

    public byte[] m_Data;
    public int m_Offset;

    public UDPacket(byte[] data,int offset){
        this.m_Data = data;
        this.m_Offset = offset;
    }

    public int getSourcePort(){
        return Packet.readShort(m_Data,m_Offset + SOURCE_PORT_BIT);
    }

    public int getDestinationPort(){
        return Packet.readShort(m_Data,m_Offset + DESTINATION_PORT_BIT);
    }

    public int getTotalLength(){
        return Packet.readShort(m_Data,m_Offset + TOLAL_LENGTH_BIT);
    }

    public int getCheckSum(){
        return Packet.readShort(m_Data,m_Offset + CHECK_SUM_BIT);
    }

    public void setSourcePort(int port){
        Packet.writeShort(m_Data,m_Offset + SOURCE_PORT_BIT,(short) port);
    }

    public void setDestinationPort(int port){
        Packet.writeShort(m_Data,m_Offset + DESTINATION_PORT_BIT,(short) port);
    }
}
