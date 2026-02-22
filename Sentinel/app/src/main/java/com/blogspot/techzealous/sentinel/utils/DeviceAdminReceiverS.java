package com.blogspot.techzealous.sentinel.utils;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

public class DeviceAdminReceiverS extends DeviceAdminReceiver {

    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
    }
}
