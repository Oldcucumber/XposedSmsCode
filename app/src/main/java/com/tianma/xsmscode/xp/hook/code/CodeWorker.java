package com.tianma.xsmscode.xp.hook.code;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.xp.hook.code.action.impl.AutoInputAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.CancelNotifyAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.CopyToClipboardAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.KillMeAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.NotifyAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.OperateSmsAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.RecordSmsAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.SmsParseAction;
import com.tianma.xsmscode.xp.hook.code.action.impl.ToastAction;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;

public class CodeWorker {

    private final Context mPhoneContext;
    private final Context mPluginContext;
    private final SharedPreferences xsp;
    private final Intent mSmsIntent;

    private final Handler mUIHandler;

    private static final com.tianma.xsmscode.core.BoundedParser PARSER = new com.tianma.xsmscode.core.BoundedParser();
    private static final ScheduledExecutorService ACTIONS = Executors.newScheduledThreadPool(2);
    private static final java.util.concurrent.Semaphore CAPACITY = new java.util.concurrent.Semaphore(128);
    private static SmsMsg previous;
    private static synchronized boolean duplicated(SmsMsg msg) {
        boolean duplicate = previous != null && Math.abs(msg.getDate() - previous.getDate()) <= 15000
            && ((msg.getSender().equals(previous.getSender()) && msg.getSmsCode().equals(previous.getSmsCode()))
            || msg.getBody().equals(previous.getBody()));
        previous = msg;
        return duplicate;
    }
    private static void schedule(Runnable action, long delay) {
        if (!CAPACITY.tryAcquire()) { XLog.e("Action queue full; skip optional action"); return; }
        try {
            ACTIONS.schedule(() -> { try { action.run(); } finally { CAPACITY.release(); } },
                Math.max(0, delay), TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) { CAPACITY.release(); XLog.e("Schedule failed", e); }
    }

    CodeWorker(Context pluginContext, Context phoneContext, Intent smsIntent) {
        mPluginContext = pluginContext;
        mPhoneContext = phoneContext;
        xsp = com.tianma.xsmscode.xp.modern.HookConfiguration.current();
        mSmsIntent = smsIntent;

        mUIHandler = new Handler(Looper.getMainLooper());


    }

    public ParseResult parse() {
        if (xsp == null || !XSPUtils.isEnabled(xsp)) {
            XLog.i("XposedSmsCode disabled, exiting");
            return null;
        }

        boolean verboseLog = XSPUtils.isVerboseLogMode(xsp);
        if (verboseLog) {
            XLog.setLogLevel(Log.VERBOSE);
        } else {
            XLog.setLogLevel(BuildConfig.LOG_LEVEL);
        }

        SmsParseAction smsParseAction = new SmsParseAction(mPluginContext, mPhoneContext, null, xsp);
        smsParseAction.setSmsIntent(mSmsIntent);
        Bundle parsed = PARSER.evaluate(smsParseAction, 200);
        if (parsed == null) return null;
        final SmsMsg smsMsg = parsed.getParcelable(SmsParseAction.SMS_MSG);
        if (smsMsg == null) return null;
        if (XSPUtils.deduplicateSms(xsp) && duplicated(smsMsg)) return buildParseResult();

        mUIHandler.post(new CopyToClipboardAction(mPluginContext, mPhoneContext, smsMsg, xsp));

        // 显示Toast Action
        mUIHandler.post(new ToastAction(mPluginContext, mPhoneContext, smsMsg, xsp));

        // 自动输入 Action
        if (XSPUtils.autoInputCodeEnabled(xsp)) {
            AutoInputAction autoInputAction = new AutoInputAction(mPluginContext, mPhoneContext, smsMsg, xsp);
            long autoInputDelay = XSPUtils.getAutoInputCodeDelay(xsp) * 1000L;
            schedule(() -> autoInputAction.call(), autoInputDelay);
        }

        // 显示通知 Action
        schedule(() -> {
            Bundle notification = new NotifyAction(mPluginContext, mPhoneContext, smsMsg, xsp).call();
            if (notification != null && notification.containsKey(NotifyAction.NOTIFY_RETENTION_TIME)) {
                CancelNotifyAction cancel = new CancelNotifyAction(mPluginContext, mPhoneContext, smsMsg, xsp);
                cancel.setNotificationId(notification.getInt(NotifyAction.NOTIFY_ID));
                schedule(() -> cancel.call(), notification.getLong(NotifyAction.NOTIFY_RETENTION_TIME));
            }
        }, 0);
        schedule(() -> new RecordSmsAction(mPluginContext, mPhoneContext, smsMsg, xsp).call(), 0);
        schedule(() -> new OperateSmsAction(mPluginContext, mPhoneContext, smsMsg, xsp).call(), 3000);
        schedule(() -> new KillMeAction(mPluginContext, mPhoneContext, smsMsg, xsp).call(), 4000);

        return buildParseResult();
    }

    private ParseResult buildParseResult() {
        ParseResult parseResult = new ParseResult();
        parseResult.setBlockSms(XSPUtils.blockSmsEnabled(xsp));
        return parseResult;
    }
}
