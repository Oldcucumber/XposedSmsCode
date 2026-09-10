package com.tianma.xsmscode.xp.modern;

import com.google.gson.JsonParser;
import com.tianma.xsmscode.feature.config.*;
import com.tianma.xsmscode.common.utils.XLog;

public final class HookConfiguration {
    private static String revision;
    private static ConfigSnapshot last;
    private static boolean missingReported;
    public static synchronized ConfigSnapshot current() {
        try {
            String json = HookRuntime.api.getRemotePreferences(ConfigStore.REMOTE_GROUP).getString(ConfigStore.SNAPSHOT_KEY, null);
            if (json == null && last == null && !missingReported) {
                XLog.w("No remote configuration; open module app after enabling it, then retry SMS");
                missingReported = true;
            }
            if (json != null && !json.equals(revision)) {
                ConfigSnapshot next = new ConfigSnapshot(JsonParser.parseString(json).getAsJsonObject());
                last = next; revision = json;
                missingReported = false;
                XLog.i("Remote configuration loaded: rules=%d, blockedApps=%d", next.rules.size(), next.blockedApps.size());
            }
        } catch (RuntimeException e) { XLog.e("Keep last valid configuration", e); }
        return last;
    }
}
