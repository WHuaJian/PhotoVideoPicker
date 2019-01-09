package com.unisky.live;

/**
 * ns（nanosecond）：纳秒，时间单位。一秒的10亿分之一，即等于10的负9次方秒。常用作内存读写速度的单位，其前面数字越小则表示速度越快。<br>
 * 1纳秒 = 1000皮秒<br>
 * 1纳秒 = 0.001 微秒<br>
 * 1纳秒 = 0.000001毫秒<br>
 * 1纳秒 = 0.00000 0001秒<br>
 * 
 * @author kenping.liu
 * @date 2014.08.08
 * 
 */
public class ULAVPTS
{
    private long mStartWhen = System.nanoTime();

    public long startWhen()
    {
        return mStartWhen;
    }

    public void reset()
    {
        mStartWhen = System.nanoTime();
    }

    public long pts()
    {
        return (System.nanoTime() - mStartWhen) / 1000;
    }

    public long pts(long offset)
    {
        return (System.nanoTime() - offset - mStartWhen) / 1000;
    }
}
