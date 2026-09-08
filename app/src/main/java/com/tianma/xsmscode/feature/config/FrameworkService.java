package com.tianma.xsmscode.feature.config;

import android.content.Context;
import com.tianma.xsmscode.common.utils.XLog;
import io.github.libxposed.service.*;

public final class FrameworkService {
    private static volatile XposedService service;
    private static volatile String pending;
    public static void init(Context context) {
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService s) { service = s; ConfigStore.changed(); }
            @Override public void onServiceDied(XposedService s) { if (service == s) service = null; }
        });
    }
    public static boolean connected() {
        try { return service != null && service.getApiVersion() >= 102; }
        catch (RuntimeException e) { return false; }
    }
    public static synchronized void publish(String json) {
        pending = json;
        if (!connected()) return;
        try {
            if (!service.getRemotePreferences(ConfigStore.REMOTE_GROUP).edit().putString(ConfigStore.SNAPSHOT_KEY, json).commit())
                throw new IllegalStateException("Remote configuration commit failed");
            pending = null;
        } catch (RuntimeException e) { XLog.e("Remote configuration unavailable", e); }
    }
    public static boolean synced() { return connected() && pending == null; }
}
