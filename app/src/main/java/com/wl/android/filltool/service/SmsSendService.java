package com.wl.android.filltool.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SmsSendService extends Service {
    public SmsSendService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
