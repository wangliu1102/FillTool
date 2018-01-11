package com.wl.android.filltool.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.wl.android.filltool.FillToolActivity;
import com.wl.android.filltool.MemFillTool;
import com.wl.android.filltool.R;
import com.wl.android.filltool.util.GetRamRomSdUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 为RAM内存填充设置的前台服务KeepLiveService
 */
public class RamFillKeepLiveService extends Service {

    public static final int NOTIFICATION_ID = 0x11;
    private static final String TAG = "RamFillKeepLiveService";
    private static final String id = "channel_1";
    private static final String name = "channel_name_1";

    private boolean mConnecting = false;
    private boolean mIsClickable = true;
    private MemFillTool mMemFillTool = MemFillTool.getInstance();
    private List<Long> mPList = new ArrayList<>();
    private long mPInt = 0;
    private NotificationManager manager;

    public RamFillKeepLiveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (flags == START_FLAG_REDELIVERY) {
            Log.d(TAG, "onStartCommand: " + "RAM填充应用被kill了,但是填充服务又重新启动");
            Toast.makeText(getApplicationContext(), "RAM填充应用被干掉,但是填充服务又重新启动啦！！！"
                    , Toast.LENGTH_SHORT).show();
        }
        try {
            mIsClickable = intent.getBooleanExtra("IsClickable", true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "onStartCommand: " + "应用被kill了");
            Toast.makeText(getApplicationContext(), "RAM填充应用被干掉啦！！！", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "onStartCommand: " + mIsClickable);
        return START_REDELIVER_INTENT; // kill后会被重启，同时重启调用onStartCommand（）时再次传入保存的Intent
//        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mConnecting = true;
        notificationThread();
        fillThread();
    }

    /**
     * 开启线程每隔一秒刷新通知
     */
    private void notificationThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mConnecting == true) {
                    Notification notify = getNotification();
                    startForeground(NOTIFICATION_ID, notify);

                    try {
                        Thread.sleep(1000); // 1秒刷新一次
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 开启线程在后台检查RAM大小，保持低内存状态
     */
    private void fillThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mConnecting == true) {
                    if (!mIsClickable &&
                            !GetRamRomSdUtil.getlowMemory(getApplicationContext())) {
                        mPInt = mMemFillTool.fillMem(1);//每次填充1M
                        Log.d(TAG, "run: " + mPInt);
                        mPList.add(mPInt);
                    }

                }

            }
        }).start();
    }

    /**
     * 获取通知
     *
     * @return
     */
    @NonNull
    private Notification getNotification() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(this, FillToolActivity.class));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pend = PendingIntent.getActivity(this, 0, intent, 0);
        Notification.Builder mBuilder;
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();
            mBuilder = new Notification.Builder(this, id)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new Notification.BigTextStyle())
                    .setContentText("是否低内存状态："
                            + GetRamRomSdUtil.getlowMemory(getApplicationContext())
                            + "\n\n" + "当前可用RAM大小："
                            + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getAvailMemory(getApplicationContext())))
                    .setContentIntent(pend);
        } else {
            mBuilder = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new Notification.BigTextStyle())
                    .setContentText("是否低内存状态："
                            + GetRamRomSdUtil.getlowMemory(getApplicationContext())
                            + "\n\n" + "当前可用RAM大小："
                            + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getAvailMemory(getApplicationContext())))
                    .setContentIntent(pend);
        }
        return mBuilder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mConnecting = false;
        stopForeground(true);
        Log.d(TAG, "onDestroy: " + mPList.toString());
        if (mPList.size() != 0) {
            for (int i = 0; i < mPList.size(); i++) {
                mMemFillTool.freeMem(mPList.get(i));
            }
        }
    }
}
