package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.DBProvider;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.data.db.entity.SmsMsgDao;
import com.tianma.xsmscode.ui.record.CodeRecordRestoreManager;
import com.tianma.xsmscode.xp.hook.code.action.CallableAction;

import java.util.ArrayList;

import android.content.SharedPreferences;

/**
 * 记录验证码短信
 */
public class RecordSmsAction extends CallableAction {

    public RecordSmsAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, SharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        if (XSPUtils.recordSmsCodeEnabled(xsp)) {
            recordSmsMsg(mSmsMsg);
        }
        return null;
    }

    private static final java.util.concurrent.ScheduledExecutorService RETRIES = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private static final java.util.concurrent.Semaphore SLOTS = new java.util.concurrent.Semaphore(32);
    private void recordSmsMsg(SmsMsg msg) {
        if (!SLOTS.tryAcquire()) { XLog.e("Record queue full"); return; }
        write(msg, 0);
    }
    private void write(SmsMsg msg, int attempt) {
        try {
            ContentValues values = new ContentValues();
            values.put(SmsMsgDao.Properties.Body.columnName, msg.getBody());
            values.put(SmsMsgDao.Properties.Company.columnName, msg.getCompany());
            values.put(SmsMsgDao.Properties.Date.columnName, msg.getDate());
            values.put(SmsMsgDao.Properties.Sender.columnName, msg.getSender());
            values.put(SmsMsgDao.Properties.SmsCode.columnName, msg.getSmsCode());
            if (mPhoneContext.getContentResolver().insert(DBProvider.SMS_MSG_CONTENT_URI, values) == null)
                throw new IllegalStateException("No record URI");
            SLOTS.release();
        } catch (Exception e) {
            if (attempt < 2) RETRIES.schedule(() -> write(msg, attempt + 1), attempt + 1, java.util.concurrent.TimeUnit.SECONDS);
            else { SLOTS.release(); XLog.e("Record unavailable after retry", e); }
        }
    }
}
