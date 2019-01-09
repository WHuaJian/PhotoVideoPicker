package com.unisky.live.mlive;

import android.os.SystemClock;
import android.util.Log;

import com.unisky.live.ULAVCodecOption;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * <ul>
 * <li>1、连接socket</li>
 * <li>2、连接成功后发送start</li>
 * <li>3、start成功后持续发数据</li>
 * <li>4、如果read侦听到配置变化，通知相关模块</li>
 * <li>5、数据完成后发送end</li>
 * </ul>
 * 
 * @author kenping.liu
 * @date 2014-8-20
 */
public class ULMLiveSession
{
    // 所有发起都不是UI主线程，如果需要在UI主线程处理，则需要通过Handler
    public interface MLiveConfigListener
    {
        public void onMLiveConfigChanged(List<ULAVCodecOption.VideoProfile> profiles);
    }

    // 所有发起都不是UI主线程，如果需要在UI主线程处理，则需要通过Handler
    public interface MLiveSessionListener
    {
        public void onStart(ULMLiveTask task);

        public void onProgress(ULMLiveTask task, int speed, int sendBytes);

        public void onError(ULMLiveTask task, int errcode, String errmsg);

        public void onEnd(ULMLiveTask task);
    }

    // 配置命令填值1601
    public static final int      TAG_CONFIG        = 1601;
    // TS数据填值1602
    public static final int      TAG_TS            = 1602;

    // 用在XML内容中的命令
    public static final String CMD_CONFIG        = "config";
    public static final String CMD_TS_START      = "start";
    public static final String CMD_TS_END        = "end";
    public static final String CMD_GETATT        = "getatt";
    public static final String CMD_ADDATT        = "addatt";

    public static final int      TS_PACKAGE_SIZE   = 1316;

    // 0 成功
    public static final int      ERR_NONE          = 0;
    // 1 用户认证失败
    public static final int      ERR_AUTH          = 1;
    // 2 文件续传失败
    public static final int      ERR_RE_TRANS      = 2;
    // 3 操作失败
    public static final int      ERR_OPERATION     = 3;
    // 4 帐号已经在使用
    public static final int      ERR_ACCOUNT_USING = 4;

    // Session State
    // 空闲，连接未开
    public static final int     STATE_IDLE        = 0;
    // 已经连接成功，并成功发送了start
    public static final int     STATE_STARTING    = 1;
    // 成功接收了start响应，持续发数据中
    public static final int     STATE_SENDING     = 2;
    // 完成数据发送，并成功发送了end
    public static final int     STATE_ENDING      = 3;
    // 成功接收了end响应
    public static final int     STATE_COMPLETED   = 4;
    private  final String TAG = ULMLiveSession.class.getSimpleName();
    private boolean error;

    private ULMLiveLoginInfo mLoginInfo;
    private ULMLiveTask mTask;
    private File mFile;

    private MLiveConfigListener  mConfigListener;
    private MLiveSessionListener mSessionListener;

    private int                  mState;
    public  boolean              mRunning;
    public ULMLiveSocket mMLiveSocket;
    private int                  mErrCode;

    private Thread mSendThread;
    private Thread mRecvThread;
    private int                  mPkgCount;
    private int                  mSpeed;
    private long                 mTimeBegin;
    private boolean              isPause;

    public ULMLiveSession(ULMLiveTask task, ULMLiveLoginInfo info)
    {
        mConfigListener = null;
        mSessionListener = null;
        mSendThread = null;
        mRecvThread = null;
        mState = STATE_IDLE;
        mTask = task;
        mLoginInfo = info;
        mMLiveSocket = new ULMLiveSocket();
        if (null != mTask)
        {
            mFile = new File(mTask.filepath);
        }
    }

    public ULMLiveSession setTask(ULMLiveTask task)
    {
        mTask = task;
        mFile = new File(mTask.filepath);
        return this;
    }

    public void setPause(boolean pause){
    	isPause = pause;
    }
    public ULMLiveSession setConfigListener(MLiveConfigListener configListener)
    {
        mConfigListener = configListener;
        return this;
    }

    public ULMLiveSession setSessionListener(MLiveSessionListener sessionListener)
    {
        mSessionListener = sessionListener;
        return this;
    }

    // 开始
    public void start()
    {
//        close();
        mRunning = true;
        mRecvThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                recvRoutine();
            }
        });
        mSendThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                sendRoutine();
            }
        });
        mRecvThread.start();
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

    /**
     * 发送数据线程主函数
     */
    private void sendRoutine()
    {
        mPkgCount = 0;
        mTimeBegin = System.currentTimeMillis();

        mSessionListener.onStart(mTask);
        while (mRunning)
        {
//            Log.i(TAG, "sendRoutine  mState:"+mState);
            switch (mState)
            {
            case STATE_IDLE:
                if (open())
                {
                    switchToState(STATE_STARTING);
                    error = false;
                }
                else
                {
                    // 连接失败
//                    mRunning = false;
                    if (null != mSessionListener)
                    {
                    	error = true;
                    	if(error){
                    		mSessionListener.onError(mTask, mErrCode, "连接服务器失败重试中");
                    	}
                    }
                }
                break;
            case STATE_STARTING:
                try
                {
                    Thread.sleep(5);
                }
                catch (Exception ex)
                {
                }
                break;
            case STATE_SENDING:
                if (mTask.state == ULMLiveTask.STATE_RECORDING || mTask.send_bytes < mTask.filesize)
                {
                    if (writeTSData())
                    {
                        mPkgCount++;
                        // 每发100个包才报告1次进度，避免太频繁
                        if (null != mSessionListener && (mPkgCount % 100) == 1)
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
                            mSessionListener.onProgress(mTask, mSpeed, mTask.send_bytes);
                        }
                    }
                    else
                    {
                    	if (null != mSessionListener)
                        {
                    		if(!isPause){
                    			mSessionListener.onError(mTask, mErrCode, "连接服务器失败，请稍候重试");
                    		}
                        }
                        // 写失败，需要重连接
                        switchToState(STATE_IDLE);
                    }
                }
                else
                {
                    switchToState(STATE_ENDING);
                    Log.d("xxx", "xxx");
                }
                break;
            case STATE_ENDING:
                try
                {
                    Thread.sleep(5);
                }
                catch (Exception ex)
                {
                }
                break;
            case STATE_COMPLETED:
                mRunning = false;
                break;
            default:
                try
                {
                    Thread.sleep(5);
                }
                catch (Exception ex)
                {
                }
                break;
            }
        }
    }

    /**
     * 接收数据线程主函数
     */
    public void recvRoutine()
    {
        while (mRunning)
        {
            try
            {
//                Log.i(TAG, "recvRoutine  mState:"+mState);
                switch (mState)
                {
                case STATE_IDLE:
                    try
                    {
                        Thread.sleep(5);
                    }
                    catch (Exception ex)
                    {
                    }
                    break;
                case STATE_STARTING:
                    ULMLiveSocket.Msg msg = mMLiveSocket.readMsg();
                    KPQNode rspNode = KPQNode.parseNode(msg.value);
                    mErrCode = rspNode.getAttrInt("ret");
                    if (mErrCode == 0)
                    {
                        KPQNode node = rspNode.getOneChildByTag("session");
                        mTask.pkg_index = (null != node) ? node.getAttrInt("index", 0) : 0;
                        mTask.send_bytes = mTask.pkg_index * TS_PACKAGE_SIZE;
                        switchToState(STATE_SENDING);
                    }
                    else
                    {
                        mRunning = false;
                        mSessionListener.onError(mTask, 4, "配置获取失败！");
                    }
                    break;
                case STATE_SENDING:
                    SystemClock.sleep(10);
                    // config message
//                    if (null != mConfigListener && "config".equals(rspNode.getAttrString("cmd")) && 0 == mErrCode)
//                    {
//                        // <cfg av_standard=”0” bitrate="1000"/>
//                        List<ULAVCodecOption.VideoProfile> profiles = new ArrayList<ULAVCodecOption.VideoProfile>();
//                        for (KPQNode node : rspNode.children)
//                        {
//                            ULAVCodecOption.VideoProfile p = new ULAVCodecOption.VideoProfile();
//                            p.resolution = node.getAttrInt("av_standard");
//                            // 服务端返回的都是k，因此这里需要乘1000
//                            p.bitrate = node.getAttrInt("bitrate") * 1000;
//                            profiles.add(p);
//                        }
//                        mConfigListener.onMLiveConfigChanged(profiles);
//                    }
                    break;
                case STATE_ENDING:
                    // 是否判断返回码？还是直接切换状态
//                     if ( mErrCode == 0 )
//                     {
                    switchToState(STATE_COMPLETED);
//                     }
                    break;
                case STATE_COMPLETED:
                    mRunning = false;
                    break;
                default:
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (Exception ex)
                    {
                    }
                    break;
                }
            }
            catch (Exception ex)
            {
                mSessionListener.onError(mTask, mErrCode, "读失败，需要重连接");
                // 读失败，需要重连接
                switchToState(STATE_IDLE);
            }
        }
    }

    public void switchToState(int state)
    {
        switch (state)
        {
        case STATE_IDLE:
            break;
        case STATE_STARTING:
            if (!cmdStart())
            {
                // 开始失败
                mRunning = false;
                if (null != mSessionListener)
                {
                    mSessionListener.onError(mTask, mErrCode, "启动失败");
                }
                return;
            }
            break;
        case STATE_SENDING:
            break;
        case STATE_ENDING:
            if (!cmdEnd())
            {
                // 结束失败
                mRunning = false;
                if (null != mSessionListener)
                {
                    mSessionListener.onError(mTask, mErrCode, "结束失败");
                }
            }
            break;
        case STATE_COMPLETED:
            mRunning = false;
            if (null != mSessionListener)
            {
                mSessionListener.onEnd(mTask);
            }
            break;
        default:
            break;
        }
        mState = state;
    }

    /*
     * 获取文稿文稿消息
     */
    public boolean cmdGetatt(String session)
    {
        KPQNode reqNode = new KPQNode("request");
        reqNode.attr.putString("cmd", CMD_GETATT);

        KPQNode userNode = reqNode.addChild("user");
        userNode.attr.putString("id", mLoginInfo.username);
        userNode.attr.putString("password", mLoginInfo.password);

        KPQNode sessionNode = reqNode.addChild("att");
        sessionNode.attr.putString("session", session);

        ULMLiveSocket.Msg msg = ULMLiveSocket.buildMsg(TAG_CONFIG, -1, KPQNode.buildNode(reqNode));
        try
        {
            mMLiveSocket.writeMsg(msg);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return false;
    }
    /*
     * 发送文稿文稿消息
     */
    public boolean cmdAddatt(String session, String name, String ctime, String userName,
                             String userId, String column, String phone, String content)
    {
        KPQNode reqNode = new KPQNode("request");
        reqNode.attr.putString("cmd", CMD_ADDATT);

        KPQNode userNode = reqNode.addChild("user");
        userNode.attr.putString("id", mLoginInfo.username);
        userNode.attr.putString("password", mLoginInfo.password);

        KPQNode sessionNode = reqNode.addChild("att");
        sessionNode.attr.putString("session", session);
        sessionNode.attr.putString("name", name);
        sessionNode.attr.putString("ctime", ctime);
        sessionNode.attr.putString("usr", userName);
        sessionNode.attr.putString("usr_id", userId);
        sessionNode.attr.putString("column", column);
        sessionNode.attr.putString("phone", phone);
        KPQNode contentNode = sessionNode.addChild("content");
        contentNode.value = content;

        ULMLiveSocket.Msg msg = ULMLiveSocket.buildMsg(TAG_CONFIG, -1, KPQNode.buildNode(reqNode));
        try
        {
            mMLiveSocket.writeMsg(msg);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean cmdStart()
    {
        /**
         * <code>
         * <request cmd="start">
         *     <user id="13912345678" password="123456" />
         *     <session id="年月日时分秒毫秒 + 3位随机数" option="0" name="testname" phone_id="devid" />
         * </request>
         * </code>
         */
        KPQNode reqNode = new KPQNode("request");
        reqNode.attr.putString("cmd", CMD_TS_START);

        KPQNode userNode = reqNode.addChild("user");
        userNode.attr.putString("id", mLoginInfo.username);
        userNode.attr.putString("password", mLoginInfo.password);

        KPQNode sessionNode = reqNode.addChild("session");
        sessionNode.attr.putString("id", mTask.id);
        sessionNode.attr.putString("name", mTask.name);
        sessionNode.attr.putString("option", mTask.send_bytes > 0 ? "1" : "0");
        sessionNode.attr.putString("phone_id", mLoginInfo.devid);
        sessionNode.attr.putString("os", "android");// add os

        ULMLiveSocket.Msg msg = ULMLiveSocket.buildMsg(TAG_CONFIG, -1, KPQNode.buildNode(reqNode));
        try
        {
            mMLiveSocket.writeMsg(msg);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean cmdEnd()
    {
        /**
         * <code>
         *  <request cmd="end">
         *      <session id="0001"/>
         *  </request>
         * </code>
         */
        KPQNode reqNode = new KPQNode("request");
        reqNode.attr.putString("cmd", CMD_TS_END);

        KPQNode sessionNode = reqNode.addChild("session");
        sessionNode.attr.putString("id", mTask.id);

        ULMLiveSocket.Msg msg = ULMLiveSocket.buildMsg(TAG_CONFIG, -1, KPQNode.buildNode(reqNode));
        try
        {
            mMLiveSocket.writeMsg(msg);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return true;
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
            ByteBuffer buffer = ByteBuffer.allocate(8 + 1316);
            buffer.order(ByteOrder.BIG_ENDIAN).putInt(mTask.pkg_index).putInt(mTask.filesize);
            buffer.put(data);
            ULMLiveSocket.Msg msg = ULMLiveSocket.buildMsg(TAG_TS, -1, buffer.array());
            try
            {
                mMLiveSocket.writeMsg(msg);
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
            switchToState(STATE_ENDING);
        }

        return true;
    }

    public void close()
    {
        mRunning = false;
        mMLiveSocket.close();
    }

    public boolean open()
    {
//        close();
        try
        {
            mMLiveSocket.connect(mLoginInfo.svr_ip, mLoginInfo.svr_port);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return false;
    }
}
