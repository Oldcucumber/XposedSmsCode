package com.tianma.xsmscode.feature.config;

import android.content.SharedPreferences;
import com.google.gson.*;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.core.CodeParser;
import java.util.*;

/** One immutable configuration revision, also used by delayed SMS actions. */
public final class ConfigSnapshot implements SharedPreferences {
    private final Map<String, Object> settings;
    public final List<CodeParser.Rule> rules;
    public final Set<String> blockedApps;
    public ConfigSnapshot(JsonObject document) {
        ConfigDocument.validate(document);
        JsonObject all = document.getAsJsonObject("preferences");
        settings = Collections.unmodifiableMap(ConfigDocument.decodePreferences(all.has(PrefConst.PREF_NAME)
                ? all.getAsJsonObject(PrefConst.PREF_NAME) : new JsonObject()));
        List<CodeParser.Rule> list = new ArrayList<>();
        for (JsonElement e : document.getAsJsonArray("rules")) {
            JsonObject r = e.getAsJsonObject();
            list.add(new CodeParser.Rule(r.get("company").getAsString(), r.get("keyword").getAsString(), r.get("regex").getAsString()));
        }
        rules = Collections.unmodifiableList(list);
        Set<String> apps = new HashSet<>();
        for (JsonElement e : document.getAsJsonArray("blockedApps")) {
            JsonObject app = e.getAsJsonObject();
            if (app.get("blocked").getAsBoolean()) apps.add(app.get("packageName").getAsString());
        }
        blockedApps = Collections.unmodifiableSet(apps);
    }
    @Override public Map<String, ?> getAll() { return settings; }
    @Override public String getString(String key, String def) { Object v = settings.get(key); return v instanceof String ? (String)v : def; }
    @Override public Set<String> getStringSet(String key, Set<String> def) { Object v = settings.get(key); return v instanceof Set ? Collections.unmodifiableSet((Set<String>)v) : def; }
    @Override public int getInt(String key, int def) { Object v = settings.get(key); return v instanceof Integer ? (int)v : def; }
    @Override public long getLong(String key, long def) { Object v = settings.get(key); return v instanceof Long ? (long)v : def; }
    @Override public float getFloat(String key, float def) { Object v = settings.get(key); return v instanceof Float ? (float)v : def; }
    @Override public boolean getBoolean(String key, boolean def) { Object v = settings.get(key); return v instanceof Boolean ? (boolean)v : def; }
    @Override public boolean contains(String key) { return settings.containsKey(key); }
    @Override public Editor edit() { throw new UnsupportedOperationException("Immutable snapshot"); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
}
