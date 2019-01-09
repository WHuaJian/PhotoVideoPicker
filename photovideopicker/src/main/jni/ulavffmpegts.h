/**
 * @author kenping.liu
 * @date   2014.08.15
 */

#ifndef _KP_ULIVE_FFMPEG_WRAPPER_H
#define _KP_ULIVE_FFMPEG_WRAPPER_H

#include <android/log.h>
#include <string.h>
#include <fcntl.h>

#ifdef __cplusplus

#ifndef __STDC_CONSTANT_MACROS
#define __STDC_CONSTANT_MACROS
#endif
#ifndef __STDC_LIMIT_MACROS
#define __STDC_LIMIT_MACROS
#endif

#ifdef _STDINT_H
#undef _STDINT_H
#endif
# include <stdint.h>

extern "C"
{
#endif

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"

#ifdef __cplusplus
}
#endif


#define LOG_TAG     "uavts"
#define LOGI(...)   __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)   __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * http://www.roman10.net/how-to-build-ffmpeg-with-ndk-r9/
 *
 * FFMpeg的C++封装，用于将音视频裸流封装成TS文件，代码参考自：https://github.com/OnlyInAmerica/FFmpegTest
 */
class ULAVFFMpegTS
{
public:
    ULAVFFMpegTS();
    ~ULAVFFMpegTS();

public:
    void SetAVOptions(int videoWidth, int videoHeight, int videoFPS, int audioSampleRate, int audioChannels);
    void Open(const char* filepath);
    void WriteData(int isVideo, uint8_t* data, int offset, int count, long pts);
    void Close();

private:
    void  InitFFMpeg();
    void  ReleaseFFMpeg();

    char* GetErrorMsg(int errcode);
    int   WriteFileHeader();
    int   WriteFileTrailer();
    int   AddVideoStream();
    int   AddAudioStream();

private:
    char    m_outputFile[256];

    int     m_videoWidth;
    int     m_videoHeight;
    int     m_videoFPS;
    int     m_audioSampleRate;
    int     m_audioChannels;

    // FFMpeg structs
    bool             m_inited;
    bool             m_running;
    AVFormatContext *m_outputFormatContext;

    int              m_audioStreamIndex;
    AVStream        *m_audioStream;
    AVCodec         *m_audioCodec;
    AVCodecContext  *m_audioCodecCtx;
    AVRational      *m_audioSourceTimeBase;

    int              m_videoStreamIndex;
    AVStream        *m_videoStream;
    AVCodec         *m_videoCodec;
    AVCodecContext  *m_videoCodecCtx;
    AVRational      *m_videoSourceTimeBase;

    char            m_errmsg[AV_ERROR_MAX_STRING_SIZE];
};


#endif
