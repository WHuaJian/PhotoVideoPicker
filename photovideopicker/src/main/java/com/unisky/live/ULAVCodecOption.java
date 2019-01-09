package com.unisky.live;

import android.media.AudioFormat;

/**
 * 
 * @author kenping.liu
 * @date 2014.08.08
 */
public class ULAVCodecOption
{
    public static class VideoProfile
    {
        public int resolution = VIDEO_RESOLUTION_480P;
        public int bitrate    = VIDEO_BITRATE_MID;
    }
    public  ULAVCodecOption(){
    }
    
    public  ULAVCodecOption(int resolution, int bitrate){
        setVideoProfile(resolution, getBitrate(bitrate));
    }

    // 352*288
    public static final int VIDEO_RESOLUTION_288P   = 0;
    // default 640*480
    public static final int VIDEO_RESOLUTION_480P   = 1;
    // 1280*720
    public static final int VIDEO_RESOLUTION_720P   = 2;

    public static final int VIDEO_BITRATE_LOW       = 1024 * 800;
    public static final int VIDEO_BITRATE_MID       = 1024 * 1500;
    public static final int VIDEO_BITRATE_HIGH      = 1024 * 3000;

    // Configured with the options below
    // Bits per second
    public static final int AUDIO_BIT_RATE          = 96000;
    // Samples per second
    public static final int AUDIO_SAMPLE_RATE       = 44100;
    // AAC frame size. Audio encoder input size is a multiple of this
    public static final int AUDIO_SAMPLES_PER_FRAME = 1024;
    public static final int AUDIO_CHANNEL_CONFIG    = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT            = AudioFormat.ENCODING_PCM_16BIT;

    // 视频选项
    public boolean          videoEnabled            = true;
    public int              videoWidth              = 1280;
    public int              videoHeight             = 720;
    public int              previewSizeVideoWidth              = 1280;
    public int              previewSizeVideoHeight             = 720;
    public int              videoFPS                = 25;
    public int              videoBitrate            = 1024 * 1500;

    public boolean          audioEnabled            = true;
    public int              audioSampleRate         = AUDIO_SAMPLE_RATE;
    public int              audioChannels           = AUDIO_CHANNEL_CONFIG;
    public int              audioBitrate            = AUDIO_BIT_RATE;

    public int getBitrate(int bitrate){
        switch (bitrate) {
            case 0:
              return  VIDEO_BITRATE_LOW;
            case 2:
                return  VIDEO_BITRATE_HIGH;
            case 1:
            default:
                return VIDEO_BITRATE_MID;
        }
    }
    
    public void setVideoProfile(int resolution, int bitrate)
    {
        videoEnabled = true;
        videoFPS = 25;
        switch (resolution)
        {
        case VIDEO_RESOLUTION_288P:
            videoWidth = 352;
            videoHeight = 288;
            videoBitrate = (bitrate > 0) ? bitrate : VIDEO_BITRATE_LOW;
            break;
        case VIDEO_RESOLUTION_720P:
            videoWidth = 1280;
            videoHeight = 720;
            videoBitrate = (bitrate > 0) ? bitrate : VIDEO_BITRATE_MID;
            break;
        case VIDEO_RESOLUTION_480P:
        default:
            videoWidth = 640;
            videoHeight = 480;
            videoBitrate = (bitrate > 0) ? bitrate : VIDEO_BITRATE_HIGH;
            break;
        }
    }
}
