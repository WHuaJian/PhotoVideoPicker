package com.unisky.live;

/**
 * 
 * @author kenping.liu
 * @date 2014.08.08
 * 
 */
public abstract class ULAVEncoder
{
    protected ULAVCodecOption mCodecOption;
    protected ULAVPTS mPTS;
    protected ULAVMuxer mMuxer;

    public ULAVEncoder(ULAVCodecOption codecOption, ULAVPTS pts, ULAVMuxer muxer)
    {
        mCodecOption = codecOption;
        mPTS = pts;
        mMuxer = muxer;
    }

    public void setCodecOption(ULAVCodecOption codecOption)
    {
        mCodecOption = codecOption;
    }

    public void setPTS(ULAVPTS pts)
    {
        mPTS = pts;
    }

    public void setMuxer(ULAVMuxer muxer)
    {
        mMuxer = muxer;
    }
    
    public abstract void open();
    public abstract void close();
    public abstract boolean drainEncodedData();
}
