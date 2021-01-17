package com.elderdrivers.riru.edxp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class HandlerUtil {

    public static Handler getMainHandler() {
        return new Handler(Looper.getMainLooper());
    }

    public static Handler getWorkerHandler() {
        HandlerThread thread = new HandlerThread("Worker");
        thread.start();
        return new Handler(thread.getLooper());
    }
}
