package com.unisky.live.mlive;

import android.util.Log;

import com.whj.photovideopicker.PhotoPicker;

import java.io.File;
import java.io.RandomAccessFile;


/**
 * Created by wu_curry on 2018/3/27.
 */

public class UdpSendSesstion {
    public static final int      TS_PACKAGE_SIZE   = 1316;

    private ULMLiveTask mTask;
    private File mFile;

    public boolean mRunning;
    public ULMLiveSocket mMLiveSocket;

    private Thread mSendThread;
    private Thread mRecvThread;
    private int mPkgCount;
    private int mSpeed;
    private long mTimeBegin;
    private boolean isPause;

    public UdpSendSesstion()
    {
        mSendThread = null;
        mRecvThread = null;
        mMLiveSocket = new ULMLiveSocket();
        if (null != mTask)
        {
            mFile = new File(mTask.filepath);
        }
    }

    public UdpSendSesstion setTask(ULMLiveTask task)
    {
        mTask = task;
        mFile = new File(mTask.filepath);
        return this;
    }


    // 开始
    public void start()
    {
//        close();
        open();
        mRunning = true;
        mSendThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                sendRoutine();
            }
        });
//        mRecvThread.start();
        mSendThread.start();
    }


    // 终止
    public void stop()
    {
        mRunning = false;
        close();

        KPKit.terminalThread(mRecvThread);
        mRecvThread = null;
        KPKit.terminalThread(mSendThread);
        mSendThread = null;
    }



    public void close()
    {
        mRunning = false;
//        mMLiveSocket.close();
        mMLiveSocket.closeUdpConnect();
    }

    /**
     * 发送数据线程主函数
     */
    private void sendRoutine(){
        mPkgCount = 0;
        mTimeBegin = System.currentTimeMillis();

        while (mRunning){
            if (writeTSData())
            {
                mPkgCount++;
                // 每发100个包才报告1次进度，避免太频繁
                if ((mPkgCount % 100) == 1)
                {
                    // 计算速度
                    long now = System.currentTimeMillis();
                    int elapsed = (int) (now - mTimeBegin) / 1000;
                    if(elapsed == 0){
                        continue;
                    }
                    if (mPkgCount == 1)
                    {
                        // 第1个包计算1次，让界面快速显示
                        mSpeed = (mPkgCount * TS_PACKAGE_SIZE) / elapsed;
                    }
                    else
                    {
                        mSpeed = (100 * TS_PACKAGE_SIZE) / elapsed;
                    }
                    mTimeBegin = now;
                }
            }
        }
    }

    private boolean writeTSData()
    {
//        if (mTask.getState() == ULMLiveTask.STATE_RECORDING)
//        {
        mTask.filesize = (int) mFile.length() & Integer.MAX_VALUE;
        if ((mTask.filesize - mTask.send_bytes) < TS_PACKAGE_SIZE)
        {
            // 如果没有更多的数据，则sleep 5毫秒，然后返回true
            try
            {
                Thread.sleep(5);
            }
            catch (Exception ex)
            {
            }
            return true;
        }
//        }
        byte[] data = new byte[TS_PACKAGE_SIZE];
        int len = 0;
        try
        {
            //FIXME 这里是锁文件读操作，写入操作也会去锁文件就会出现数据丢失。
            RandomAccessFile file = new RandomAccessFile(mFile, "r");
            file.seek(mTask.pkg_index * TS_PACKAGE_SIZE);
            len = file.read(data);
            file.close();
            Log.d("writedata","data length = "+data.length);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            // 文件读取错误不是网络错误，无需重连接
            return true;
        }
        if (len > 0)
        {
            /**
             * 一个音视频数据分包的组成为：固定包头（type + len共8字节） + 数据包头（index + size共8字节 +
             * 纯数据（1316字节））。有可能最后一个音视频包不够1316，就填写实际长度。
             */
//            ByteBuffer buffer = ByteBuffer.allocate(8 + 1316);
//            buffer.order(ByteOrder.BIG_ENDIAN).putInt(mTask.pkg_index).putInt(mTask.filesize);
//            buffer.put(data);
            try
            {
                mMLiveSocket.udpSendPackage(data);
                mTask.pkg_index++;
                mTask.send_bytes += len;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }
        }
        else
        {
        }

        return true;
    }

    public boolean open()
    {
//        close();
        try
        {
            mMLiveSocket.udpConnect(Integer.parseInt(PhotoPicker.UDP_PORT));//Integer.parseInt(ZxingModelUtils.getInstance().getUDPPort())  65500
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return true;
    }
}
