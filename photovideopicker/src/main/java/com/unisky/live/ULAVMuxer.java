package com.unisky.live;

import java.nio.ByteBuffer;

/**
 * 输出混合器
 * 
 * @author kenping.liu
 * @date 2014-8-14
 */
public class ULAVMuxer
{
    private Object lock = new Object();

    public void setupCodecOption(ULAVCodecOption codecOption)
    {
        // int channels = (codecOption.audioChannels ==
        // AudioFormat.CHANNEL_IN_MONO) ? 1 : 2;
        ULFFmpegTSMuxer.setavoption(codecOption.videoWidth, codecOption.videoHeight, codecOption.videoFPS,
                codecOption.audioSampleRate, 1);
    }

    public void open(String filepath)
    {
        ULFFmpegTSMuxer.open(filepath);
    }

    public void close()
    {
        ULFFmpegTSMuxer.close();
    }

    public void writeData(int isvideo, ByteBuffer data, int offset, int count, long pts)
    {
        synchronized (lock)
        {
            ULFFmpegTSMuxer.feeddata(isvideo, data, offset, count, pts);
        }
    }
}
