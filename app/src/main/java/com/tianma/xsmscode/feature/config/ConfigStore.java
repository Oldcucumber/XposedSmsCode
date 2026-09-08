package com.tianma.xsmscode.feature.config;

import android.content.*;
import android.util.AtomicFile;
import com.google.gson.*;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.data.db.DBManager;
import com.tianma.xsmscode.data.db.entity.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class ConfigStore {
    public static final String REMOTE_GROUP = "configuration_v1";
    public static final String SNAPSHOT_KEY = "snapshot";
    private static Context context;
    private static final ScheduledExecutorService IO = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> pending;
    private static final Set<String> observed = new HashSet<>();
    private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER = (p, k) -> changed();
    public static synchronized void init(Context c) {
        context = c.getApplicationContext();
        observe(PrefConst.PREF_NAME);
        changed();
    }
    private static void observe(String name) {
        if (observed.add(name)) context.getSharedPreferences(name, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(LISTENER);
    }
    public static synchronized void changed() {
        if (context == null) return;
        if (pending != null) pending.cancel(false);
        pending = IO.schedule(() -> {
            try { publish(capture(context)); }
            catch (Exception e) { XLog.e("Configuration sync failed", e); }
        }, 150, TimeUnit.MILLISECONDS);
    }
    public static synchronized JsonObject capture(Context c) {
        JsonObject doc = ConfigDocument.empty();
        JsonObject preferences = doc.getAsJsonObject("preferences");
        Set<String> groups = new TreeSet<>(); groups.add(PrefConst.PREF_NAME);
        File[] files = new File(c.getApplicationInfo().dataDir, "shared_prefs").listFiles();
        if (files != null) for (File file : files) if (file.getName().endsWith(".xml")) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            if (isUserGroup(name)) groups.add(name);
        }
        for (String name : groups) {
            Map<String, Object> values = new LinkedHashMap<>(c.getSharedPreferences(name, Context.MODE_PRIVATE).getAll());
            values.remove("local_version_code");
            preferences.add(name, ConfigDocument.encodePreferences(values));
            if (context != null) observe(name);
        }
        DBManager db = DBManager.get(c);
        for (SmsCodeRule rule : db.queryAll(SmsCodeRule.class)) {
            JsonObject r = new JsonObject(); r.addProperty("company", rule.getCompany() == null ? "" : rule.getCompany());
            r.addProperty("keyword", rule.getCodeKeyword()); r.addProperty("regex", rule.getCodeRegex());
            doc.getAsJsonArray("rules").add(r);
        }
        for (AppInfo app : db.queryAllBlockedApps()) {
            JsonObject a = new JsonObject(); a.addProperty("packageName", app.getPackageName());
            a.addProperty("label", app.getLabel() == null ? "" : app.getLabel()); a.addProperty("blocked", app.getBlocked());
            doc.getAsJsonArray("blockedApps").add(a);
        }
        ConfigDocument.validate(doc); return doc;
    }
    public static boolean isUserGroup(String name) {
        return name.equals(PrefConst.PREF_NAME) || name.toLowerCase(Locale.ROOT).contains("cyanea");
    }
    public static synchronized void publish(JsonObject doc) throws IOException {
        if (context == null) return;
        String json = doc.toString();
        if (json.getBytes(StandardCharsets.UTF_8).length > ConfigDocument.MAX_BYTES) throw new IOException("Configuration too large");
        writeAtomic(snapshotFile(context), json);
        FrameworkService.publish(json);
        new android.app.backup.BackupManager(context).dataChanged();
    }
    public static File snapshotFile(Context c) { return new File(c.getFilesDir(), "config-backup/configuration.json"); }
    public static void writeAtomic(File file, String content) throws IOException {
        File parent = file.getParentFile(); if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create config directory");
        AtomicFile atomic = new AtomicFile(file); FileOutputStream stream = null;
        try { stream = atomic.startWrite(); stream.write(content.getBytes(StandardCharsets.UTF_8)); atomic.finishWrite(stream); }
        catch (IOException e) { if (stream != null) atomic.failWrite(stream); throw e; }
    }
}
