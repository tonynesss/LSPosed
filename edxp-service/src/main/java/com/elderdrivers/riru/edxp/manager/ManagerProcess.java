package com.elderdrivers.riru.edxp.manager;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.HandlerUtil;
import com.elderdrivers.riru.edxp.manager.dialog.ManagementDialog;
import com.elderdrivers.riru.edxp.util.Utils;

public class ManagerProcess implements KeepAll {
    public static String CONFIG_PATH = null;
    private static final BroadcastReceiver SHOW_MANAGEMENT_RECEIVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.logI("showManagement: action=" + (intent != null ? intent.getAction() : "(null)"));
            ManagementDialog.show();
        }
    };

    private static void register() {
        Context context = null;
        try {
            context = ActivityThread.currentActivityThread().getApplication();
        } catch (Exception e) {
            Utils.logW("getApplication", e);
        }

        if (context == null) {
            Utils.logW("application is null, wait 1s");
            HandlerUtil.getWorkerHandler().postDelayed(ManagerProcess::register, 1000);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SECRET_CODE");
        intentFilter.addDataAuthority("577577", null);
        intentFilter.addDataScheme("android_secret_code");

        try {
            context.registerReceiver(SHOW_MANAGEMENT_RECEIVER, intentFilter,
                    "android.permission.CONTROL_INCALL_EXPERIENCE", null);
            Utils.logI("registerReceiver android.provider.Telephony.SECRET_CODE");
        } catch (Exception e) {
            Utils.logI("registerReceiver android.provider.Telephony.SECRET_CODE", e);
        }
    }
}
