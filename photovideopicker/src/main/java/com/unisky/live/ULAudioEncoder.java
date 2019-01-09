package com.unisky.live;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音频编码
 * @author kenping.liu
 * @date 2014.08.08
 * 
 */
public class ULAudioEncoder extends ULAVEncoder
{
    private final String TAG = ULAudioEncoder.class.getSimpleName();
    public static final int       TIMEOUT_USEC    = 100;

    // AAC Low Overhead Audio Transport Multiplex
    public static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";


    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaFormat mAudioFormat;

    private byte[]                audioPacket     = new byte[1024];
    // ADTS Header length
    private int                   ADTS_LENGTH     = 7;
    // Variables Recycled by addADTStoPacket
    // AAC LC 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
    private int                   profile         = 2;
    // 44.1KHz
    private int                   freqIdx         = 4;
    // MPEG-4 Audio Channel Configuration. 1 Channel front-center
    private int                   chanCfg         = 1;

    public ULAudioEncoder(ULAVCodecOption codecOption, ULAVPTS pts, ULAVMuxer muxer)
    {
        super(codecOption, pts, muxer);
    }

    @Override
    public void open()
    {
        mAudioFormat = new MediaFormat();
        mAudioFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, ULAVCodecOption.AUDIO_SAMPLE_RATE);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mCodecOption.audioBitrate);
        mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        mBufferInfo = new MediaCodec.BufferInfo();
        try {
            mMediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mMediaCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            Log.e(TAG, "",e);
        }
    }

    @Override
    public void close()
    {
        if (mMediaCodec != null)
        {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    public void offerAudioData(AudioRecord audioRecord, boolean endOfStream)
    {
        // send current frame data to encoder
        try
        {
           
            int bufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (bufferIndex >= 0)
            {
                ByteBuffer inputBuffer;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    inputBuffer =mMediaCodec.getInputBuffer(bufferIndex);
                }
                else
                {
                    ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                    inputBuffer = inputBuffers[bufferIndex];
                }
                inputBuffer.clear();
                int inputLen = audioRecord.read(inputBuffer, ULAVCodecOption.AUDIO_SAMPLES_PER_FRAME * 2);
                if (inputLen == AudioRecord.ERROR_INVALID_OPERATION)
                {
                    Log.e("", "Audio read error");
                }
                long offset = (inputLen / ULAVCodecOption.AUDIO_SAMPLE_RATE) / 1000000000;
                long pts = mPTS.pts(offset);
                if (endOfStream)
                {
                    Log.i("", "EOS received in sendAudioToEncoder");
                    mMediaCodec.queueInputBuffer(bufferIndex, 0, inputLen, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                else
                {
                    mMediaCodec.queueInputBuffer(bufferIndex, 0, inputLen, pts, 0);
                }
            }
        }
        catch (Throwable t)
        {
            Log.e("", "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    @Override
    public boolean drainEncodedData()
    {
        boolean hasdata = false;
        if (null != mMediaCodec)
        {
            int outBitsSize = 0;
            int outPacketSize = 0;

            ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

            while (true)
            {
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (outputBufferIndex < 0)
                {
                    break;
                }
                hasdata = true;
                ByteBuffer encodedData ;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                     encodedData = mMediaCodec.getOutputBuffer(outputBufferIndex);
                }
                else
                {
                    encodedData = encoderOutputBuffers[outputBufferIndex];
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
                {
                    // 什么都不做？
                    // outBitsSize = mBufferInfo.size;
                    // outPacketSize = outBitsSize + ADTS_LENGTH;
                    // addADTStoPacket(audioPacket, outPacketSize);
                    // encodedData.get(audioPacket, ADTS_LENGTH, outBitsSize);
                    // encodedData.position(mBufferInfo.offset);
                    // encodedData.put(audioPacket, 0, outPacketSize);
                    // mBufferInfo.size = outPacketSize;
                }
                else if (mBufferInfo.size > 0)
                {
                    outBitsSize = mBufferInfo.size;
                    outPacketSize = outBitsSize + ADTS_LENGTH;
                    addADTStoPacket(audioPacket, outPacketSize);
                    encodedData.position(mBufferInfo.offset);
                    encodedData.get(audioPacket, ADTS_LENGTH, outBitsSize);
                    //encodedData为只读,不能put,使用新的输出文件
                    ByteBuffer output= ByteBuffer.allocateDirect(outPacketSize);
                    output.put(audioPacket,0,outPacketSize);
                    mBufferInfo.size = outPacketSize;
                    output.limit(outPacketSize);
                    mMuxer.writeData(0, output, mBufferInfo.offset, mBufferInfo.size,
                            mBufferInfo.presentationTimeUs);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                {
                    break;
                }
            }
        }
        return hasdata;
    }
    
    /**
     * Add ADTS header at the beginning of each and every AAC packet. This is<br>
     * needed as MediaCodec encoder generates a packet of raw AAC data.<br>
     * <br>
     * Note the packetLen must count in the ADTS header itself. See:<br>
     * http://wiki.multimedia.cx/index.php?title=ADTS Also:<br>
     * http://wiki.multimedia.cx/index.php?title=MPEG-
     * 4_Audio#Channel_Configurations<br>
     **/
    private void addADTStoPacket(byte[] packet, int packetLen)
    {
        // fill in ADTS data
        packet[0] = (byte) 0xFF; // 11111111 = syncword
        packet[1] = (byte) 0xF9; // 1111 1 00 1 = syncword MPEG-2 Layer CRC
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
