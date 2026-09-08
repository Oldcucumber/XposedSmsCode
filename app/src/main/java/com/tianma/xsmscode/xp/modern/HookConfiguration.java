package com.tianma.xsmscode.xp.modern;

import com.google.gson.JsonParser;
import com.tianma.xsmscode.feature.config.*;
import com.tianma.xsmscode.common.utils.XLog;

public final class HookConfiguration {
    private static String revision;
    private static ConfigSnapshot last;
    public static synchronized ConfigSnapshot current() {
        try {
            String json = HookRuntime.api.getRemotePreferences(ConfigStore.REMOTE_GROUP).getString(ConfigStore.SNAPSHOT_KEY, null);
            if (json != null && !json.equals(revision)) {
                ConfigSnapshot next = new ConfigSnapshot(JsonParser.parseString(json).getAsJsonObject());
                last = next; revision = json;
            }
        } catch (RuntimeException e) { XLog.e("Keep last valid configuration", e); }
        return last;
    }
}
