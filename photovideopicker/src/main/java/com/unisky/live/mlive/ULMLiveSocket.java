package com.unisky.live.mlive;

import com.whj.photovideopicker.PhotoPicker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * 
 * @author kenping.liu
 * @date 2014-8-20
 */
public class ULMLiveSocket
{
    public static class Msg
    {
        public int    tag   = 0;
        public int    len   = 0;
        public byte[] value = null;

        public void validateLen()
        {
            if (null == value)
            {
                len = 0;
            }
            else if (len < 1 || len > value.length)
            {
                len = value.length;
            }
        }
    }

    private static final int HEADER_LEN = 8;

    public String mSvrIP;
    public int               mSvrPort;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private DatagramSocket udpSocket; //udp

    public static Msg buildMsg(int tag, int len, byte[] value)
    {
        Msg msg = new Msg();
        msg.tag = tag;
        msg.value = value;
        msg.len = len;
        msg.validateLen();
        return msg;
    }

    public ULMLiveSocket()
    {
    }

    public void connect(String svrip, int svrport) throws Exception
    {
        close();

        mSocket = new Socket(svrip, svrport);
        mSocket.setKeepAlive(true);//生存时间
        mOutputStream = mSocket.getOutputStream();
        mInputStream = mSocket.getInputStream();
    }


    private InetAddress inetAddress;

    public void udpConnect(int svrport){
        if(udpSocket != null){
            udpSocket.close();
        }
        try{
            inetAddress = InetAddress.getByName(PhotoPicker.IP_ADDRESS); //192.168.123.23  ZxingModelUtils.getInstance().getIp()
            this.mSvrPort = svrport;
            udpSocket = new DatagramSocket(svrport);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void udpSendPackage(byte[] bytes) throws Exception {
        if(udpSocket != null){
            DatagramPacket packet = new DatagramPacket(bytes,bytes.length,inetAddress,mSvrPort);
            udpSocket.send(packet);
        }
    }

    public void closeUdpConnect(){
        if(udpSocket != null){
            udpSocket.close();
        }
    }

    public void close()
    {
        if (null != mInputStream)
        {
            try
            {
                mInputStream.close();
            }
            catch (Exception ex)
            {
            }
        }
        if (null != mOutputStream)
        {
            try
            {
                mOutputStream.close();
            }
            catch (Exception ex)
            {
            }
        }
        if (null != mSocket)
        {
            try
            {
                mSocket.close();
            }
            catch (Exception ex)
            {
            }
        }

        mInputStream = null;
        mOutputStream = null;
        mSocket = null;
    }

    public Msg readMsg() throws Exception
    {
        Msg msg = new Msg();
        byte header[] = new byte[HEADER_LEN];
        if (mInputStream.read(header) == HEADER_LEN)
        {
            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            msg.tag = buffer.getInt();
            msg.len = buffer.getInt();

            if (msg.len > 0)
            {
                buffer = ByteBuffer.allocate(msg.len);
                byte data[] = new byte[10240];
                int recv_cnt = 0;
                while (recv_cnt < msg.len)
                {
                    int cnt = mInputStream.read(data);
                    buffer.put(data, 0, cnt);
                    recv_cnt += cnt;
                }
                // return new String(buffer.array(), "utf-8").trim();
                msg.value = buffer.array();
            }
        }

        return msg;
    }

    public void writeMsg(Msg msg) throws Exception
    {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LEN + msg.len);
        buffer.order(ByteOrder.BIG_ENDIAN).putInt(msg.tag).putInt(msg.len);
        if (msg.len > 0)
        {
            buffer.put(msg.value, 0, msg.len);
        }
        // buffer.position(0);
        mOutputStream.write(buffer.array());
        mOutputStream.flush();
    }
}
