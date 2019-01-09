/**
 * @author kenping.liu
 * @date   2014.08.15
 */
#include <jni.h>

#include "ulavffmpegts.h"

ULAVFFMpegTS g_ffmpeg;

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void Java_com_unisky_live_ULFFmpegTSMuxer_setavoption(JNIEnv* env, jobject obj,
        jint vw, jint vh, jint vfps, jint asample, jint achannel)
{
    g_ffmpeg.SetAVOptions(vw, vh, vfps, asample, achannel);
}

JNIEXPORT void Java_com_unisky_live_ULFFmpegTSMuxer_open(JNIEnv* env, jobject obj, jstring filepath)
{
    const char* str = env->GetStringUTFChars(filepath, 0);
    g_ffmpeg.Open(str);
    env->ReleaseStringUTFChars(filepath, str);
}

JNIEXPORT void Java_com_unisky_live_ULFFmpegTSMuxer_feeddata(JNIEnv* env, jobject obj,
        jint isvideo, jobject avdata, jint offset, jint count, jlong pts)
{
    uint8_t* data = (uint8_t*)(env->GetDirectBufferAddress(avdata));
    g_ffmpeg.WriteData(isvideo, data, offset, count, pts);
}

JNIEXPORT void Java_com_unisky_live_ULFFmpegTSMuxer_close(JNIEnv* env, jobject obj)
{
    g_ffmpeg.Close();
}

// JNI的YUV转换实现，通过编译O2/O3优化后，1帧1280*720的YUV转换，可稳定在4毫秒左右
JNIEXPORT void Java_com_unisky_live_ULFFmpegTSMuxer_yuv12To420SemiPlanar(JNIEnv* env, jobject obj,
        jbyteArray yv12, jbyteArray yuv420, jint width, jint height)
{
    uint8_t* pYV12 = (uint8_t*)env->GetByteArrayElements(yv12, NULL);
    uint8_t* pYUV420 = (uint8_t*)env->GetByteArrayElements(yuv420, NULL);

    size_t ysize = width * height;
    size_t csize = ysize / 4;
    size_t size  = ysize + csize + csize;

    memcpy(pYUV420, pYV12, ysize);
    uint8_t* uv = pYUV420 + ysize;
    uint8_t* u  = pYV12 + ysize + csize;
    uint8_t* v  = pYV12 + ysize;

    if ( (csize % 32)==0 )
    {
        for ( size_t c=0; c<csize; c+=16 )
        {
            uv[0]  = u[0];
            uv[2]  = u[1];
            uv[4]  = u[2];
            uv[6]  = u[3];
            uv[8]  = u[4];
            uv[10] = u[5];
            uv[12] = u[6];
            uv[14] = u[7];
            uv[16] = u[8];
            uv[18] = u[9];
            uv[20] = u[10];
            uv[22] = u[11];
            uv[24] = u[12];
            uv[26] = u[13];
            uv[28] = u[14];
            uv[30] = u[15];
            uv[1]  = v[0];
            uv[3]  = v[1];
            uv[5]  = v[2];
            uv[7]  = v[3];
            uv[9]  = v[4];
            uv[11] = v[5];
            uv[13] = v[6];
            uv[15] = v[7];
            uv[17] = v[8];
            uv[19] = v[9];
            uv[21] = v[10];
            uv[23] = v[11];
            uv[25] = v[12];
            uv[27] = v[13];
            uv[29] = v[14];
            uv[31] = v[15];

            uv += 32;
            u  += 16;
            v  += 16;
        }
    }
    else if ( (csize % 16)==0 )
    {
        for ( size_t c=0; c<csize; c+=8 )
        {
            uv[0]  = u[0];
            uv[2]  = u[1];
            uv[4]  = u[2];
            uv[6]  = u[3];
            uv[8]  = u[4];
            uv[10] = u[5];
            uv[12] = u[6];
            uv[14] = u[7];
            uv[1]  = v[0];
            uv[3]  = v[1];
            uv[5]  = v[2];
            uv[7]  = v[3];
            uv[9]  = v[4];
            uv[11] = v[5];
            uv[13] = v[6];
            uv[15] = v[7];

            uv += 16;
            u  += 8;
            v  += 8;
        }
    }
    else if ( (csize % 8)==0 )
    {
        for ( size_t c=0; c<csize; c+=4 )
        {
            uv[0] = u[0];
            uv[2] = u[1];
            uv[4] = u[2];
            uv[6] = u[3];
            uv[1] = v[0];
            uv[3] = v[1];
            uv[5] = v[2];
            uv[7] = v[3];

            uv += 8;
            u  += 4;
            v  += 4;
        }
    }
    else if ( (csize % 4)==0 )
    {
        for ( size_t c=0; c<csize; c+=2 )
        {
            uv[0] = u[0];
            uv[2] = u[1];
            uv[1] = v[0];
            uv[3] = v[1];

            uv += 4;
            u  += 2;
            v  += 2;
        }
    }
    else
    {
        for ( size_t c=0; c<csize; c++ )
        {
            *uv++ = *u++;
            *uv++ = *v++;
        }
    }

    env->ReleaseByteArrayElements(yv12, (jbyte*)pYV12, 0);
    env->ReleaseByteArrayElements(yuv420, (jbyte*)pYUV420, 0);
}

#ifdef __cplusplus
}
#endif
