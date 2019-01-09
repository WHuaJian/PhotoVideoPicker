package com.unisky.live;

/**
 * 
 * @author kenping.liu
 * @date 2014-08-08
 */
public class ULAVData
{
    public boolean isvideo = true;
    public byte[]  data    = null;
    public int     count   = 0;
    public long    pts     = 0;

    public ULAVData()
    {
    }

    public ULAVData(boolean isvideo, byte[] data, long pts)
    {
        this.isvideo = isvideo;
        this.data = data;
        this.count = null != data ? data.length : 0;
        this.pts = pts;
    }
}
