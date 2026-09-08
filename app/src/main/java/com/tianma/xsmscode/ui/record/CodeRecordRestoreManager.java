package com.tianma.xsmscode.ui.record;

import android.annotation.SuppressLint;
import android.content.Context;

import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.JsonUtils;
import com.tianma.xsmscode.common.utils.StorageUtils;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.data.db.DBManager;
import com.tianma.xsmscode.data.db.entity.SmsMsg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CodeRecordRestoreManager {

    private static final String RECORD_FILE_PREFIX = "CodeRecord_";

    /**
     * Export code record to file
     */
    /**
     * Import code records to database
     */
    public static boolean importToDatabase(Context context) {
        File[] files = getRecordFiles();
        if (files == null || files.length == 0) return true;
        android.database.sqlite.SQLiteDatabase db = DBManager.get(context).getSQLiteDatabase();
        try {
            db.beginTransaction();
            try {
                for (File file : files) {
                    SmsMsg msg = loadFromFile(file);
                    if (msg == null || msg.getSender() == null || msg.getBody() == null) throw new IllegalArgumentException("Invalid pending record");
                    try (android.database.Cursor c = db.query("SMS_MSG", new String[]{"_id"}, "SENDER=? AND BODY=? AND DATE=?",
                            new String[]{msg.getSender(), msg.getBody(), Long.toString(msg.getDate())}, null, null, null)) {
                        if (!c.moveToFirst()) { msg.setId(null); DBManager.get(context).addSmsMsg(msg); }
                    }
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            // A failed cleanup is harmless: subsequent imports are idempotent.
            for (File file : files) if (!file.delete()) XLog.w("Legacy record cleanup deferred");
            return true;
        } catch (Exception e) { XLog.e("Import legacy records failed", e); return false; }
    }

    /**
     * Get code record files
     */
    public static File[] getRecordFiles() {
        File filesDir = StorageUtils.getFilesDir();
        return filesDir.listFiles((dir, name) -> name.startsWith(RECORD_FILE_PREFIX));
    }

    private static SmsMsg loadFromFile(File recordFile) {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(
                    new FileInputStream(recordFile), StandardCharsets.UTF_8);

            return JsonUtils.entityFromJson(isr, SmsMsg.class, true);
        } catch (FileNotFoundException e) {
            XLog.e("", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
