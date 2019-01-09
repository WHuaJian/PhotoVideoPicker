package com.unisky.live.mlive;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class ULMLiveTask
{
    public static final int STATE_NONE      = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_RECORDED  = 2;

    public long             record_time;
    public String id;
    public String name;
    public String filepath;
    public int              state;

    public int              filesize;
    public int              send_bytes;
    public int              pkg_index;

    public static String generateID(long ms)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        Random random = new Random(ms);
        return String.format(Locale.ENGLISH, "%04d%02d%02d%02d%02d%02d%03d%03d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND),
                random.nextInt(1000));
    }
}
