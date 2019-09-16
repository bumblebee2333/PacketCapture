package com.lxy.packetcapture.tools;

import android.util.SparseArray;

/**
 * created by 李昕怡 on 2019/8/27
 * NAT管理对象 就是将数据包的源端口作为key 保存目标IP和目标port等
 */

public class NatSessionManager {

    /**
     * 保存会话的最大个数
     */
    static final int MAX_SESSION_COUNT = 60;

    /**
     * 会话保存的时间
     */
    static final long SESSION_TIMEOUT = 60 * 10000000L;
    public static final SparseArray<NatSession> sessions = new SparseArray<>();

    /**
     * 通过本地端口获取会话信息
     * @param portKey 本地端口
     * @return 会话信息
     */
    public static NatSession getSession(int portKey){
        return sessions.get(portKey);
    }

    /**
     * 获取会话个数
     * @return 会话个数
     */
    public static int getSessionCount(){
        return sessions.size();
    }

    /**
     * 清除过期会话
     */
    public static void clearSessions(){
        long now = System.nanoTime();
        for(int i = sessions.size()-1;i>=0;i--){
            NatSession natSession = sessions.valueAt(i);
            if(now - natSession.LastNanoTime > SESSION_TIMEOUT){
                sessions.removeAt(i);
            }
        }
    }

    //在获取数据包的时候 对NatSession进行赋值
    public static NatSession createSession(int portKey,int remoteIP,int remotePort){
        //若sessions中的个数超过60个 清除sessions
        if(sessions.size() > MAX_SESSION_COUNT){
            clearSessions();
        }

        NatSession session = new NatSession();
        session.RemoteIP = remoteIP;
        session.RemotePort = remotePort;
        session.LastNanoTime = System.nanoTime();
        session.localPort = portKey;

        sessions.put(portKey,session);
        return session;
    }
}
