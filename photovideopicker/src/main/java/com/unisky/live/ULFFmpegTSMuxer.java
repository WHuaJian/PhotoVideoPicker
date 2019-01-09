package com.unisky.live;

import java.nio.ByteBuffer;

/**
 * JNI bridge
 * @author kenping.liu
 * @date   2014-8-15
 */
public class ULFFmpegTSMuxer
{
    static
    {
        System.loadLibrary("ulavtsmuxer");
    }
    
    public static native void setavoption(int vw, int vh, int vfps, int asample, int achannel);
    public static native void open(String filepath);
    public static native void feeddata(int isvideo, ByteBuffer data, int offset, int count, long pts);
    public static native void close();
    
    public static native void yuv12To420SemiPlanar(byte[] yv12, byte[] yuv420, int width, int height);
}
