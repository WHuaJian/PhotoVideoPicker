/**
 * @author kenping.liu
 * @date   2014.08.15
 */

#include "ulavffmpegts.h"

#define OUTPUT_FORMAT_NAME  "mpegts"
#define VIDEO_PIX_FMT       PIX_FMT_YUV420P
#define VIDEO_CODEC_ID      AV_CODEC_ID_H264
#define AUDIO_CODEC_ID      AV_CODEC_ID_AAC
#define AUDIO_SAMPLE_FMT    AV_SAMPLE_FMT_S16

char g_emptyStr[] = "";

ULAVFFMpegTS::ULAVFFMpegTS()
{
    m_inited  = false;
    m_running = false;

    m_outputFormatContext = NULL;

    m_audioStreamIndex    = -1;
    m_audioStream         = NULL;
    m_audioCodec          = NULL;
    m_audioCodecCtx       = NULL;
    m_audioSourceTimeBase = NULL;

    m_videoStreamIndex    = -1;
    m_videoStream         = NULL;
    m_videoCodec          = NULL;
    m_videoCodecCtx       = NULL;
    m_videoSourceTimeBase = NULL;
}

ULAVFFMpegTS::~ULAVFFMpegTS()
{
    ReleaseFFMpeg();
}

void ULAVFFMpegTS::SetAVOptions(int videoWidth, int videoHeight, int videoFPS, int audioSampleRate, int audioChannels)
{
    LOGI("ULAVFFMpegTS::SetAVOptions, running=%d", m_running);
    // Open后不能再修改参数
    if (!m_running)
    {
        LOGI("ULAVFFMpegTS::SetAVOptions, width=%d,height=%d,fps=%d; sampleRate=%d,channels=%d",
                videoWidth, videoHeight, videoFPS, audioSampleRate, audioChannels);
        m_videoWidth  = videoWidth;
        m_videoHeight = videoHeight;
        m_videoFPS    = videoFPS;
        m_audioSampleRate = audioSampleRate;
        m_audioChannels   = audioChannels;
    }
}

void ULAVFFMpegTS::Open(const char* filepath)
{
    InitFFMpeg();
    Close();

    sprintf(m_outputFile, "%s", filepath);

    LOGI("ULAVFFMpegTS::Open: %s", m_outputFile);

    // Start and initialize
    // Create AVRational that expects timestamps in microseconds
    m_videoSourceTimeBase = (AVRational*)av_malloc(sizeof(AVRational));
    m_videoSourceTimeBase->num = 1;
    m_videoSourceTimeBase->den = 1000000;

    m_audioSourceTimeBase = (AVRational*)av_malloc(sizeof(AVRational));
    m_audioSourceTimeBase->num = 1;
    m_audioSourceTimeBase->den = 1000000;

    while (true)
    {
        LOGI("ULAVFFMpegTS::Open: avformat_alloc_output_context2");
        int ret = avformat_alloc_output_context2(&m_outputFormatContext, NULL, OUTPUT_FORMAT_NAME, m_outputFile);
        if (ret < 0)
        {
            LOGI("ULAVFFMpegTS::Open: avformat_alloc_output_context2 error=%s", GetErrorMsg(ret));
            break;
        }

        LOGI("ULAVFFMpegTS::Open: AddVideoStream/AddAudioStream");
        AddVideoStream();
        AddAudioStream();

        LOGI("ULAVFFMpegTS::Open: avio_open");
        if (!(m_outputFormatContext->oformat->flags & AVFMT_NOFILE)
                && (ret = avio_open(&m_outputFormatContext->pb, m_outputFile, AVIO_FLAG_WRITE)) < 0)
        {
            LOGI("ULAVFFMpegTS::Open: avio_open error=%s", GetErrorMsg(ret));
            break;
        }

        LOGI("ULAVFFMpegTS::Open: WriteFileHeader");
        if ((ret = WriteFileHeader()) < 0)
        {
            break;
        }

        m_running = true;
        break;
    }
    LOGI("ULAVFFMpegTS::Open: m_running=%d", m_running);
    if (!m_running)
    {
        Close();
    }
}

void ULAVFFMpegTS::WriteData(int isVideo, uint8_t* data, int offset, int size, long pts)
{
    if (!m_running)
    {
        return;
    }
    // LOGI("ULAVFFMpegTS::WriteData: isvideo=%d, size=%d, pts=%ld", isVideo, size, pts);
    AVPacket* packet = (AVPacket*)av_malloc(sizeof(AVPacket));
    av_init_packet(packet);

    if (isVideo)
    {
        packet->stream_index = m_videoStreamIndex;
    }
    else
    {
        packet->stream_index = m_audioStreamIndex;
    }
    packet->size = size;
    packet->data = data;
    packet->pts = av_rescale_q(pts, *m_videoSourceTimeBase,
            m_outputFormatContext->streams[packet->stream_index]->time_base);
    int ret = av_interleaved_write_frame(m_outputFormatContext, packet);
    if (ret < 0)
    {
        // LOG ERROR
        LOGE("ULAVFFMpegTS::WriteData: failed=%s", GetErrorMsg(ret));
    }
    av_free_packet(packet);
}

// 关闭并释放资源
void ULAVFFMpegTS::Close()
{
    LOGI("ULAVFFMpegTS::Close");
    if ( m_running )
    {
        WriteFileTrailer();
    }
    if ( NULL!=m_audioCodecCtx )
    {
        avcodec_close(m_audioCodecCtx);
    }
    if ( NULL!=m_videoCodecCtx )
    {
        avcodec_close(m_videoCodecCtx);
    }
    if ( NULL!=m_audioSourceTimeBase )
    {
        av_free(m_audioSourceTimeBase);
    }
    if ( NULL!=m_videoSourceTimeBase )
    {
        av_free(m_videoSourceTimeBase);
    }
    if ( NULL!=m_outputFormatContext )
    {
        avformat_free_context(m_outputFormatContext);
    }
    m_outputFormatContext = NULL;
    m_audioStreamIndex    = -1;
    m_audioStream         = NULL;
    m_audioCodec          = NULL;
    m_audioCodecCtx       = NULL;
    m_audioSourceTimeBase = NULL;
    m_videoStreamIndex    = -1;
    m_videoStream         = NULL;
    m_videoCodec          = NULL;
    m_videoCodecCtx       = NULL;
    m_videoSourceTimeBase = NULL;
    m_running             = false;
}

void ULAVFFMpegTS::InitFFMpeg()
{
    if ( !m_inited )
    {
        av_register_all();
        avformat_network_init();
        avcodec_register_all();
    }
    m_inited = true;
}

void ULAVFFMpegTS::ReleaseFFMpeg()
{
    Close();
}

char* ULAVFFMpegTS::GetErrorMsg(int errcode)
{
    return av_strerror(errcode, m_errmsg, AV_ERROR_MAX_STRING_SIZE)? g_emptyStr : m_errmsg;
}

int ULAVFFMpegTS::WriteFileHeader()
{
    LOGI("ULAVFFMpegTS::WriteFileHeader");
    AVDictionary *options = NULL;
    // Write header for output file
    int ret = avformat_write_header(m_outputFormatContext, &options);
    if ( ret<0 )
    {
        LOGE("ULAVFFMpegTS::WriteFileHeader: avformat_write_header error=%s", GetErrorMsg(ret));
    }
    av_dict_free(&options);
    return ret;
}

int ULAVFFMpegTS::WriteFileTrailer()
{
    return av_write_trailer(m_outputFormatContext);
}

int ULAVFFMpegTS::AddVideoStream()
{
    /* find the video encoder */
    m_videoCodec = avcodec_find_encoder(VIDEO_CODEC_ID);
    if ( !m_videoCodec )
    {
        LOGI("add_video_stream codec not found, as expected. No encoding necessary");
    }

    m_videoStream = avformat_new_stream(m_outputFormatContext, m_videoCodec);
    if ( !m_videoStream )
    {
        LOGE("add_video_stream could not alloc stream");
    }

    m_videoStreamIndex = m_videoStream->index;
    LOGI("addVideoStream at index %d", m_videoStreamIndex);
    m_videoCodecCtx = m_videoStream->codec;

    avcodec_get_context_defaults3(m_videoCodecCtx, m_videoCodec);

    m_videoCodecCtx->codec_id = VIDEO_CODEC_ID;

    /* Resolution must be a multiple of two. */
    m_videoCodecCtx->width = m_videoWidth;
    m_videoCodecCtx->height = m_videoHeight;

    /**
     * timebase: This is the fundamental unit of time (in seconds) in terms
     * of which frame timestamps are represented. For fixed-fps content,
     * timebase should be 1/framerate and timestamp increments should be
     * identical to 1.
     */
    m_videoCodecCtx->time_base.den = 1000/m_videoFPS;
    m_videoCodecCtx->time_base.num = 1;
    m_videoCodecCtx->pix_fmt = VIDEO_PIX_FMT;

    /* Some formats want stream headers to be separate. */
    if ( m_outputFormatContext->oformat->flags&AVFMT_GLOBALHEADER )
    {
        m_videoCodecCtx->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    return 0;
}

int ULAVFFMpegTS::AddAudioStream()
{
    /* find the audio encoder */
    m_audioCodec = avcodec_find_encoder(AUDIO_CODEC_ID);
    if ( !m_audioCodec )
    {
        LOGE("add_audio_stream codec not found");
    }
    //LOGI("add_audio_stream found codec_id: %d",codec_id);
    m_audioStream = avformat_new_stream(m_outputFormatContext, m_audioCodec);
    if ( !m_audioStream )
    {
        LOGE("add_audio_stream could not alloc stream");
    }
    m_audioStreamIndex = m_audioStream->index;

    m_audioCodecCtx = m_audioStream->codec;
    avcodec_get_context_defaults3(m_audioCodecCtx, m_audioCodec);
    m_audioCodecCtx->strict_std_compliance = FF_COMPLIANCE_UNOFFICIAL; // for native aac support
    /* put sample parameters */
    m_audioCodecCtx->sample_fmt    = AUDIO_SAMPLE_FMT;
    m_audioCodecCtx->time_base.den = 44100;
    m_audioCodecCtx->time_base.num = 1;
    m_audioCodecCtx->sample_rate   = m_audioSampleRate;
    m_audioCodecCtx->channels      = m_audioChannels;
    LOGI("addAudioStream sample_rate %d index %d", m_audioCodecCtx->sample_rate, m_audioStream->index);
    //LOGI("add_audio_stream parameters: sample_fmt: %d bit_rate: %d sample_rate: %d", codec_audio_sample_fmt, bit_rate, audio_sample_rate);
    // some formats want stream headers to be separate
    if ( m_outputFormatContext->oformat->flags & AVFMT_GLOBALHEADER )
    {
        m_audioCodecCtx->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    return 0;
}
