package com.learning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecordReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppController.getObserver().UpdateNotification(intent.getAction());
    }
}
