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
        try {
            File[] recordFiles = getRecordFiles();

            List<SmsMsg> smsMsgList = new ArrayList<>();
            for (File recordFile : recordFiles) {
                SmsMsg smsMsg = loadFromFile(recordFile);
                if (smsMsg != null) {
                    smsMsgList.add(smsMsg);
                    recordFile.delete();
                }
            }

            if (!smsMsgList.isEmpty()) {
                DBManager dbManager = DBManager.get(context);
                dbManager.addSmsMsgList(smsMsgList);
                XLog.d("Import code records to database succeed");

                List<SmsMsg> allMsgList = dbManager.queryAllSmsMsg();
                if (allMsgList.size() > PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT) {
                    List<SmsMsg> outdatedMsgList = new ArrayList<>();
                    for (int i = PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT; i < allMsgList.size(); i++) {
                        outdatedMsgList.add(allMsgList.get(i));
                    }
                    dbManager.removeSmsMsgList(outdatedMsgList);
                    XLog.d("Remove outdated code records succeed");
                }
            }
            return true;
        } catch (Throwable t) {
            XLog.e("Import code records to database failed.", t);
        }
        return false;
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
