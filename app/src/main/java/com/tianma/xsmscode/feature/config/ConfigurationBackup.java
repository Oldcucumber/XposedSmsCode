package com.tianma.xsmscode.feature.config;

import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.google.gson.*;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.db.DBManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ConfigurationBackup {
    public static JsonObject read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Cannot open backup");
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > ConfigDocument.MAX_BYTES) throw new IOException("Backup exceeds 4 MiB");
                out.write(buffer, 0, n);
            }
            JsonObject doc = JsonParser.parseString(out.toString(StandardCharsets.UTF_8.name())).getAsJsonObject();
            ConfigDocument.validate(doc);
            for (String group : doc.getAsJsonObject("preferences").keySet())
                if (!ConfigStore.isUserGroup(group)) throw new IOException("Unknown settings group");
            return doc;
        } catch (RuntimeException e) { throw new IOException("Invalid configuration backup", e); }
    }
    public static void export(Context c, Uri uri) throws IOException {
        synchronized (ConfigStore.class) {
            byte[] bytes = new GsonBuilder().setPrettyPrinting().create().toJson(ConfigStore.capture(c)).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > ConfigDocument.MAX_BYTES) throw new IOException("Backup exceeds 4 MiB");
            try (OutputStream out = c.getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IOException("Cannot create backup"); out.write(bytes);
            }
        }
    }
    private static File journal(Context c) { return new File(c.getNoBackupFilesDir(), "apiupdate-restore.json"); }
    public static void recover(Context c) throws IOException {
        synchronized (ConfigStore.class) {
            if (!journal(c).isFile()) return;
            apply(c, read(new FileInputStream(journal(c))));
            if (!journal(c).delete()) throw new IOException("Cannot finish restore recovery");
        }
    }
    public static void restore(Context c, JsonObject document) throws IOException {
        synchronized (ConfigStore.class) {
            ConfigDocument.validate(document);
            for (String group : document.getAsJsonObject("preferences").keySet())
                if (!ConfigStore.isUserGroup(group)) throw new IOException("Unknown settings group");
            recover(c);
            JsonObject old = ConfigStore.capture(c);
            ConfigStore.writeAtomic(journal(c), old.toString());
            try {
                apply(c, document);
                if (!journal(c).delete()) throw new IOException("Cannot complete restore journal");
            } catch (Exception error) {
                try { apply(c, old); if (!journal(c).delete()) throw new IOException("Recovery journal retained"); }
                catch (Exception rollbackError) { error.addSuppressed(rollbackError); }
                throw new IOException("Restore failed; original configuration retained or recovery pending", error);
            }
            // Remote publication is retryable; local successful restore is authoritative.
            ConfigStore.changed();
        }
    }
    private static void apply(Context c, JsonObject doc) throws IOException {
        JsonObject prefs = doc.getAsJsonObject("preferences");
        Set<String> groups = new HashSet<>(ConfigStore.capture(c).getAsJsonObject("preferences").keySet());
        groups.addAll(prefs.keySet());
        SQLiteDatabase db = DBManager.get(c).getSQLiteDatabase();
        db.beginTransaction();
        try {
            db.delete("SMS_CODE_RULE", null, null);
            for (JsonElement e : doc.getAsJsonArray("rules")) {
                JsonObject r = e.getAsJsonObject(); ContentValues v = new ContentValues();
                v.put("COMPANY", r.get("company").getAsString()); v.put("CODE_KEYWORD", r.get("keyword").getAsString()); v.put("CODE_REGEX", r.get("regex").getAsString());
                db.insertOrThrow("SMS_CODE_RULE", null, v);
            }
            db.delete("APP_INFO", null, null);
            for (JsonElement e : doc.getAsJsonArray("blockedApps")) {
                JsonObject a = e.getAsJsonObject(); ContentValues v = new ContentValues();
                v.put("PACKAGE_NAME", a.get("packageName").getAsString()); v.put("LABEL", a.get("label").getAsString()); v.put("BLOCKED", a.get("blocked").getAsBoolean());
                db.insertOrThrow("APP_INFO", null, v);
            }
            for (String group : groups) {
                SharedPreferences sp = c.getSharedPreferences(group, Context.MODE_PRIVATE);
                Object internalVersion = sp.getAll().get("local_version_code");
                SharedPreferences.Editor editor = sp.edit().clear();
                if (internalVersion instanceof Integer) editor.putInt("local_version_code", (int)internalVersion);
                Map<String,Object> values = prefs.has(group) ? ConfigDocument.decodePreferences(prefs.getAsJsonObject(group)) : Collections.emptyMap();
                for (Map.Entry<String,Object> entry : values.entrySet()) put(editor, entry.getKey(), entry.getValue());
                if (!editor.commit()) throw new IOException("Preference commit failed");
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); DBManager.get(c).clearIdentityCache(); }
        applyLauncherState(c);
    }
    private static void put(SharedPreferences.Editor e, String k, Object v) {
        if (v instanceof String) e.putString(k, (String)v);
        else if (v instanceof Boolean) e.putBoolean(k, (boolean)v);
        else if (v instanceof Integer) e.putInt(k, (int)v);
        else if (v instanceof Long) e.putLong(k, (long)v);
        else if (v instanceof Float) e.putFloat(k, (float)v);
        else if (v instanceof Set) e.putStringSet(k, (Set<String>)v);
    }
    public static void applyLauncherState(Context c) {
        boolean hidden = c.getSharedPreferences(PrefConst.PREF_NAME, Context.MODE_PRIVATE).getBoolean(PrefConst.KEY_HIDE_LAUNCHER_ICON, false);
        c.getPackageManager().setComponentEnabledSetting(new ComponentName(c, c.getPackageName() + ".HomeActivityAlias"),
                hidden ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP);
    }
}
