package com.unisky.live;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * 视频编码
 * <ul>
 * <li>YV12格式：w*h个Y，(w*h)/4个V，(w*h)/4个U；YYYYYYYY VV UU</li>
 * <li>YUV420SemiPlanar：w*h个Y，(w*h)/4个UV；YYYYYYYY UVUV</li>
 * <li>YUV420Planar：w*h个Y，(w*h)/4个U，(w*h)/4个Y；YYYYYYYY UU VV</li>
 * </ul>
 * <br>
 * #19 COLOR_FormatYUV420Planar (I420)<br>
 * #20 COLOR_FormatYUV420PackedPlanar (also I420)<br>
 * #21 COLOR_FormatYUV420SemiPlanar (NV12)<br>
 * #39 COLOR_FormatYUV420PackedSemiPlanar (also NV12)<br>
 * #0x7f000100 COLOR_TI_FormatYUV420PackedSemiPlanar (also also NV12)<br>
 * 
 * @author kenping.liu
 * @date 2014.08.08
 * 
 */
public class ULVideoEncoder extends ULAVEncoder
{
    private static final String VIDEO_MIME_TYPE = "video/avc";

    private static int            mColorFormat    = 0;

    private MediaFormat mVideoFormat;
    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;

    private byte[]                mVideoSPSAndPPS;
    private byte[]                mYUV420;

    private ULYuv12Conv mYuv12Conv;
    private ByteBuffer mH264Data;

    public int                   mFrames;
    public long                  mStartTime;
    public long                  mElapsed;

    /**
     * yv12 转 YUV420SemiPlanar, YUV -> Y[UV]
     * 
     * @param yv12bytes
     * @param i420bytes
     * @param width
     * @param height
     */
    public static void swapYV12toIYUV420SemiPlanar(byte[] yv12bytes, byte[] i420bytes, int width, int height)
    {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;

        System.arraycopy(yv12bytes, 0, i420bytes, 0, frameSize);
        int u = frameSize + qFrameSize;
        int v = frameSize;
        int uv = frameSize;
        int totalSize = frameSize + qFrameSize + qFrameSize;
        int semiSize = qFrameSize + qFrameSize;
        if ((semiSize % 16) == 0)
        {
            // Log.i("", "16: " + width + "," + height);
            for (; uv < totalSize;)
            {
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
            }
        }
        else if ((semiSize % 8) == 0)
        {
            // Log.i("", "8: " + width + "," + height);
            for (; uv < totalSize;)
            {
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
            }
        }
        else if ((semiSize % 4) == 0)
        {
            // Log.i("", "4: " + width + "," + height);
            for (; uv < totalSize;)
            {
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
            }
        }
        else
        {
            Log.i("", "2: " + width + "," + height);
            for (; uv < totalSize;)
            {
                i420bytes[uv++] = yv12bytes[u++];
                i420bytes[uv++] = yv12bytes[v++];
            }
        }
    }

    public static void swapYV12toYUV420Planar(byte[] yv12bytes, byte[] i420bytes, int width, int height)
    {
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;
        // Y
        System.arraycopy(yv12bytes, 0, i420bytes, 0, frameSize);
        // Cb (U)
        System.arraycopy(yv12bytes, frameSize + qFrameSize, i420bytes, frameSize, qFrameSize);
        // Cr (V)
        System.arraycopy(yv12bytes, frameSize, i420bytes, frameSize + qFrameSize, qFrameSize);
    }

    public ULVideoEncoder(ULAVCodecOption codecOption, ULAVPTS pts, ULAVMuxer muxer)
    {
        super(codecOption, pts, muxer);
    }

    @Override
    public void open()
    {
        mElapsed = 0;
        mFrames = 0;
        mStartTime = System.currentTimeMillis();

        int frameSize = mCodecOption.videoWidth * mCodecOption.videoHeight;
        int qFrameSize = frameSize / 4;

        probeColorFormat();
        Log.i("kp-h264", String.format("colorFormat=%d", mColorFormat));

        mVideoSPSAndPPS = null;
        mYuv12Conv = new ULYuv12Conv();
        mYuv12Conv.reset(mCodecOption.videoWidth, mCodecOption.videoHeight);
        Log.i("kp-h264", mYuv12Conv.toString());
        // mYUV420 = new byte[mYuv12Conv.size];
        mYUV420 = new byte[frameSize + qFrameSize + qFrameSize];

        mVideoFormat = MediaFormat
                .createVideoFormat(VIDEO_MIME_TYPE, mCodecOption.videoWidth, mCodecOption.videoHeight);
        mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mCodecOption.videoBitrate);
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mCodecOption.videoFPS);
        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        // 关键帧间隔时间 单位秒
        mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        // mVideoFormat.setInteger("stride", mCodecOption.videoWidth);
        // mVideoFormat.setInteger("slice-height", mCodecOption.videoHeight);

        mBufferInfo = new MediaCodec.BufferInfo();
        mH264Data = ByteBuffer.allocateDirect(frameSize + qFrameSize + qFrameSize);

        try
        {
            mMediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mMediaCodec.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        try
        {
            long elapsed = System.currentTimeMillis() - mStartTime;
            Log.i("kp-h264", "mFrames=" + mFrames + ",EplapsedTime=" + elapsed + ",FPS=" + (elapsed / mFrames));

            mMediaCodec.stop();
            mMediaCodec.release();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void offerVideoData(byte[] inputData, long pts)
    {
        mFrames++;
        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar)
        {
            // long ts = System.currentTimeMillis();
            ULFFmpegTSMuxer.yuv12To420SemiPlanar(inputData, mYUV420, mCodecOption.videoWidth, mCodecOption.videoHeight);
            // swapYV12toIYUV420SemiPlanar(inputData, mYUV420,
            // mCodecOption.videoWidth, mCodecOption.videoHeight);
            // mYuv12Conv.swapTo420SemiPlanar(inputData, mYUV420);
            // mElapsed += (System.currentTimeMillis() - ts);
            // if ((mFrames % 100) == 0)
            // {
            // Log.i("", "mFrames=" + mFrames + ", mEplased=" + mElapsed +
            // ", period=" + (mElapsed / mFrames));
            // }
        }
        else
        {
            swapYV12toYUV420Planar(inputData, mYUV420, mCodecOption.videoWidth, mCodecOption.videoHeight);
            // mYuv12Conv.swapTo420Planar(inputData, mYUV420);
        }
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0)
        {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(mYUV420);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mYUV420.length, pts, 0);
        }
    }

    @Override
    public boolean drainEncodedData()
    {
        boolean hasdata = false;
        if (null != mMediaCodec)
        {
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            while (true)
            {
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                if (outputBufferIndex < 0)
                {
                    break;
                }
                hasdata = true;
                try
                {
                    ByteBuffer encodedData ;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                         encodedData = mMediaCodec.getOutputBuffer(outputBufferIndex);
                    }
                    else
                    {
                        encodedData = outputBuffers[outputBufferIndex];
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not
                    // needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) > 0)
                    {
                        // 保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
                        mVideoSPSAndPPS = new byte[mBufferInfo.size];
                        encodedData.position(mBufferInfo.offset);
                        encodedData.get(mVideoSPSAndPPS);
                    }
                    else if (mBufferInfo.size > 0)
                    {
                        // key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上
                        // bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0 && null != mVideoSPSAndPPS)
                        {
                            mBufferInfo.size += mVideoSPSAndPPS.length;
                            if (mH264Data.capacity() < mBufferInfo.size)
                            {
                                mH264Data = ByteBuffer.allocateDirect(mBufferInfo.size + 1);
                            }
                            mH264Data.clear();
                            mH264Data.put(mVideoSPSAndPPS, 0, mVideoSPSAndPPS.length);
                            encodedData.position(mBufferInfo.offset);
                            mH264Data.put(encodedData);
                            mH264Data.position(0);
                            mMuxer.writeData(1, mH264Data, 0, mBufferInfo.size, mBufferInfo.presentationTimeUs);
                        }
                        else
                        {
                            mMuxer.writeData(1, encodedData, mBufferInfo.offset, mBufferInfo.size,
                                    mBufferInfo.presentationTimeUs);
                        }
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
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

    public int encodeFrame(byte[] input, byte[] output, long pts)
    {
        int pos = 0;
        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar)
        {
            swapYV12toIYUV420SemiPlanar(input, mYUV420, mCodecOption.videoWidth, mCodecOption.videoHeight);
        }
        else
        {
            swapYV12toYUV420Planar(input, mYUV420, mCodecOption.videoWidth, mCodecOption.videoHeight);
        }
        try
        {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0)
            {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(mYUV420);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mYUV420.length, pts, 0);
            }

            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
            while (outputBufferIndex >= 0)
            {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[mBufferInfo.size];
                outputBuffer.get(outData);

                if (mVideoSPSAndPPS != null)
                {
                    System.arraycopy(outData, 0, output, pos, outData.length);
                    pos += outData.length;

                }
                else
                {
                    // bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                    // 保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    if (spsPpsBuffer.getInt() == 0x00000001)
                    {
                        mVideoSPSAndPPS = new byte[outData.length];
                        System.arraycopy(outData, 0, mVideoSPSAndPPS, 0, outData.length);
                    }
                    else
                    {
                        return -1;
                    }
                }

                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
            }
            // key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上
            // bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME
            if (output[4] == 0x65)
            {
                System.arraycopy(output, 0, mYUV420, 0, pos);
                System.arraycopy(mVideoSPSAndPPS, 0, output, 0, mVideoSPSAndPPS.length);
                System.arraycopy(mYUV420, 0, output, mVideoSPSAndPPS.length, pos);
                pos += mVideoSPSAndPPS.length;
            }

        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        return pos;
    }

    private static int probeColorFormat()
    {
        if (mColorFormat <= 0)
        {
            mColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
            for (int i = (MediaCodecList.getCodecCount() - 1); i >= 0; i--)
            {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (codecInfo.isEncoder())
                {
                    for (String t : codecInfo.getSupportedTypes())
                    {
                        if (VIDEO_MIME_TYPE.equalsIgnoreCase(t))
                        {
                            try
                            {
                                MediaCodecInfo.CodecCapabilities cc = codecInfo.getCapabilitiesForType(t);
                                for (int cf : cc.colorFormats)
                                {
                                    if (cf == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                                            || cf == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar
                                            || cf == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                                            || cf == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar)

                                    {
                                        mColorFormat = cf;
                                        break;
                                    }
                                }
                                break;
                            }
                            catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return mColorFormat;
    }

}
