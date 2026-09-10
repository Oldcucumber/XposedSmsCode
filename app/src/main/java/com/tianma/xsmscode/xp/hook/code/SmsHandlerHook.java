package com.tianma.xsmscode.xp.hook.code;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Telephony;

import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.github.tianma8023.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.NotificationConst;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.NotificationUtils;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.xp.helper.XposedWrapper;
import com.tianma.xsmscode.xp.hook.BaseHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tianma.xsmscode.xp.modern.HookCallback;
import android.content.SharedPreferences;
import com.tianma.xsmscode.xp.modern.HookRuntime;
import com.tianma.xsmscode.xp.modern.Reflector;
import com.tianma.xsmscode.xp.modern.LoadedPackage;

/**
 * Hook class com.android.internal.telephony.InBoundSmsHandler
 */
public class SmsHandlerHook extends BaseHook {

    public static final String ANDROID_PHONE_PACKAGE = "com.android.phone";

    private static final String TELEPHONY_PACKAGE = "com.android.internal.telephony";
    private static final String SMS_HANDLER_CLASS = TELEPHONY_PACKAGE + ".InboundSmsHandler";
    private static final String SMSCODE_PACKAGE = BuildConfig.APPLICATION_ID;

    // Host App Context
    private Context mPhoneContext;
    // Plugin App Context
    private Context mPluginContext;
    private boolean mNotificationChannelReady;
    private boolean mCopyReceiverReady;

    @Override
    public void onLoadPackage(LoadedPackage lpparam) {
        if (ANDROID_PHONE_PACKAGE.equals(lpparam.packageName)) {
            XLog.i("SmsCode initializing");
            printDeviceInfo();
            try {
                hookSmsHandler(lpparam.classLoader);
            } catch (Throwable e) {
                XLog.e("Failed to hook SmsHandler", e);
            }
            XLog.i("SmsCode initialize completely");
        }
    }

    @SuppressWarnings("deprecation")
    private static void printDeviceInfo() {
        XLog.i("Phone manufacturer: %s", Build.MANUFACTURER);
        XLog.i("Phone model: %s", Build.MODEL);
        XLog.i("Android version: %s", Build.VERSION.RELEASE);
        XLog.i("Android SDK: %d; build: %s", Build.VERSION.SDK_INT, Build.DISPLAY);
        int xposedVersion;
        try {
            xposedVersion = HookRuntime.getXposedVersion();
        } catch (Throwable e) {
            xposedVersion = HookRuntime.XPOSED_BRIDGE_VERSION;
        }
        XLog.i("Xposed bridge version: %d", xposedVersion);
        XLog.i("SmsCode version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    private void hookSmsHandler(ClassLoader classloader) {
        try { hookConstructor(classloader); }
        catch (Throwable e) { XLog.e("Constructor hook unavailable; try context at SMS dispatch", e); }
        hookDispatchIntent(classloader);
    }

    private void hookConstructor(ClassLoader classloader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            hookConstructor34(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            hookConstructor30(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 ~ 10 (api 24 - 29)
            hookConstructor24(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4 ~ 6.1 (api 19 - 23)
            hookConstructor19(classloader);
        }
    }

    // Android 14+
    private void hookConstructor34(ClassLoader classLoader) {
        XLog.i("Hooking InboundSmsHandler constructor for android v34+");
//        Reflector.findAndHookConstructor(SMS_HANDLER_CLASS, classLoader,
//                /* name                 */ String.class,
//                /* context              */ Context.class,
//                /* storageMonitor       */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
//                /* phone                */ TELEPHONY_PACKAGE + ".Phone",
//                /* cellBroadcastHandler */ TELEPHONY_PACKAGE + ".CellBroadcastHandler",
//                /* smsDispatchersController */ TELEPHONY_PACKAGE + ".SmsDispatchersController",
//                new ConstructorHook());
        Class<?> smsHandlerClazz = XposedWrapper.findClass(SMS_HANDLER_CLASS, classLoader);
        if (smsHandlerClazz != null) {
            HookRuntime.hookAllConstructors(smsHandlerClazz, new ConstructorHook());
        }
    }

    // Android 11+
    private void hookConstructor30(ClassLoader classloader) {
        XLog.i("Hooking InboundSmsHandler constructor for android v30+");
        Reflector.findAndHookConstructor(SMS_HANDLER_CLASS, classloader,
                /* name                 */ String.class,
                /* context              */ Context.class,
                /* storageMonitor       */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
                /* phone                */ TELEPHONY_PACKAGE + ".Phone",
                new ConstructorHook());
    }

    // Android N+
    private void hookConstructor24(ClassLoader classloader) {
        XLog.i("Hooking InboundSmsHandler constructor for android v24+");
        Reflector.findAndHookConstructor(SMS_HANDLER_CLASS, classloader,
                /* name                 */ String.class,
                /* context              */ Context.class,
                /* storageMonitor       */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
                /* phone                */ TELEPHONY_PACKAGE + ".Phone",
                /* cellBroadcastHandler */ TELEPHONY_PACKAGE + ".CellBroadcastHandler",
                new ConstructorHook());
    }

    // Android KitKat+
    private void hookConstructor19(ClassLoader classloader) {
        XLog.i("Hooking InboundSmsHandler constructor for Android v19+");
        Reflector.findAndHookConstructor(SMS_HANDLER_CLASS, classloader,
                /*                 name */ String.class,
                /*              context */ Context.class,
                /*       storageMonitor */ TELEPHONY_PACKAGE + ".SmsStorageMonitor",
                /*                phone */ TELEPHONY_PACKAGE + ".PhoneBase",
                /* cellBroadcastHandler */ TELEPHONY_PACKAGE + ".CellBroadcastHandler",
                new ConstructorHook());
    }

    private void hookDispatchIntent(ClassLoader classloader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hookDispatchIntent29(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookDispatchIntent23(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookDispatchIntent21(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookDispatchIntent19(classloader);
        }
    }

    // Android K
    private void hookDispatchIntent19(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v19+");
        Reflector.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /* resultReceiver */ BroadcastReceiver.class,
                new DispatchIntentHook(3));
    }

    // Android L+
    private void hookDispatchIntent21(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v21+");
        Reflector.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /* resultReceiver */ BroadcastReceiver.class,
                /*           user */ UserHandle.class,
                new DispatchIntentHook(3));
    }

    // Android M+
    private void hookDispatchIntent23(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v23+");
        Reflector.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /*           opts */ Bundle.class,
                /* resultReceiver */ BroadcastReceiver.class,
                /*           user */ UserHandle.class,
                new DispatchIntentHook(4));
    }

    // Android 10+
    private void hookDispatchIntent29(ClassLoader classLoader) {
        XLog.d("Hooking dispatchIntent() for Android v29+");
        // 实际上这是一个通用的方式，不再使用精确匹配来找到对应的 Method，而使用模糊搜索的方式
        // 但是之前分 API 匹配的逻辑在以往 Android 版本的系统之中已经验证通过，故而保留原有逻辑

        Class<?> inboundSmsHandlerClass = XposedWrapper.findClass(SMS_HANDLER_CLASS, classLoader);
        if (inboundSmsHandlerClass == null) {
            XLog.e("Class: %s cannot found", SMS_HANDLER_CLASS);
            return;
        }

        Method target = null;
        int receiverIndex = -1;
        for (Method method : inboundSmsHandlerClass.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (!method.getName().equals("dispatchIntent") || method.getReturnType() != void.class
                    || types.length == 0 || types[0] != Intent.class) continue;
            int found = -1;
            for (int i = 1; i < types.length; i++) if (BroadcastReceiver.class.isAssignableFrom(types[i])) {
                if (found != -1) { found = -2; break; }
                found = i;
            }
            if (found < 0) continue;
            if (target != null) { XLog.e("Ambiguous dispatchIntent signature; preserve SMS delivery"); return; }
            target = method; receiverIndex = found;
        }
        if (target == null) { XLog.e("No supported SMS dispatch signature"); return; }
        XposedWrapper.hookMethod(target, new DispatchIntentHook(receiverIndex));
        XLog.i("SMS dispatch hook installed: %s", target.toGenericString());
    }

    private class ConstructorHook extends HookCallback {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                afterConstructorHandler(param);
            } catch (Throwable e) {
                XLog.e("Error occurred in constructor hook", e);
                throw e;
            }
        }
    }

    private void afterConstructorHandler(HookCallback.MethodHookParam param) {
        if (param.hasThrowable()) return;
        Context context = null;
        for (Object arg : param.args) if (arg instanceof Context) { context = (Context) arg; break; }
        if (context == null) return;
        initializeContext(context);
    }

    private synchronized void initializeContext(Context context) {
        if (mPhoneContext == null) mPhoneContext = context;
        if (getPluginContext() == null) return;
        if (!mNotificationChannelReady) {
            try { initNotificationChannel(); mNotificationChannelReady = true; }
            catch (Exception e) { XLog.e("Notification channel initialization deferred", e); }
        }
        if (!mCopyReceiverReady) {
            try { registerCopyCodeReceiver(); mCopyReceiverReady = true; }
            catch (Exception e) { XLog.e("Copy receiver initialization deferred", e); }
        }
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = NotificationConst.CHANNEL_ID_SMSCODE_NOTIFICATION;
            String channelName = getPluginContext().getString(R.string.channel_name_smscode_notification);
            NotificationUtils.createNotificationChannel(mPhoneContext,
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            XLog.d("Init notification channel succeed");
        }
    }

    private void registerCopyCodeReceiver() {
        CopyCodeReceiver.registerMe(mPhoneContext);
        XLog.d("Register copy code receiver");
    }

    private class DispatchIntentHook extends HookCallback {
        private final int mReceiverIndex;

        DispatchIntentHook(int receiverIndex) {
            mReceiverIndex = receiverIndex;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                beforeDispatchIntentHandler(param, mReceiverIndex);
            } catch (Throwable e) {
                XLog.e("Error occurred in dispatchIntent() hook, ", e);
                throw e;
            }
        }
    }

    private void beforeDispatchIntentHandler(HookCallback.MethodHookParam param, int receiverIndex) {
        if (!(param.args[0] instanceof Intent)) return;
        Intent intent = (Intent) param.args[0];
        String action = intent.getAction();

        // We only care about the initial SMS_DELIVER intent,
        // the rest are irrelevant
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        if (mPhoneContext == null) {
            Object context = Reflector.getObjectField(param.thisObject, "mContext");
            if (!(context instanceof Context)) return;
            initializeContext((Context) context);
            XLog.i("Recovered phone context at SMS dispatch");
        }
        android.os.UserManager users = mPhoneContext.getSystemService(android.os.UserManager.class);
        if (users == null || !users.isUserUnlocked()) return;
        if (mPluginContext == null || !mNotificationChannelReady || !mCopyReceiverReady)
            initializeContext(mPhoneContext);
        if (mPluginContext == null) return;

        ParseResult parseResult = new CodeWorker(getPluginContext(), mPhoneContext, intent).parse();
        if (parseResult != null) {// parse succeed
            if (parseResult.isBlockSms()) {
                XLog.d("Blocking code SMS...");
                if (deleteRawTableAndSendMessage(param.thisObject, param.args[receiverIndex])) param.setResult(null);
            }
        }
    }

    private static final int EVENT_BROADCAST_COMPLETE = 3;

    private boolean deleteRawTableAndSendMessage(Object inboundSmsHandler, Object smsReceiver) {
        if (smsReceiver == null) return false;
        long token = Binder.clearCallingIdentity();
        try {
            // Validate the completion method before mutating the raw table.
            Reflector.findMethodBestMatch(inboundSmsHandler.getClass(), "sendMessage", EVENT_BROADCAST_COMPLETE);
            Object where = Reflector.getObjectField(smsReceiver, "mDeleteWhere");
            Object args = Reflector.getObjectField(smsReceiver, "mDeleteWhereArgs");
            if (!(where instanceof String) || ((String) where).isEmpty() || !(args instanceof String[])) return false;
            deleteFromRawTable(inboundSmsHandler, smsReceiver);
            sendEventBroadcastComplete(inboundSmsHandler);
            return true;
        } catch (Throwable e) {
            XLog.e("SMS interception failed; preserve original delivery", e);
            return false;
        } finally { Binder.restoreCallingIdentity(token); }
    }

    private void sendEventBroadcastComplete(Object inboundSmsHandler) {
        XLog.d("Send event(EVENT_BROADCAST_COMPLETE)");
        Reflector.callMethod(inboundSmsHandler, "sendMessage", EVENT_BROADCAST_COMPLETE);
    }

    private void deleteFromRawTable(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteFromRawTable24(inboundSmsHandler, smsReceiver);
        } else {
            deleteFromRawTable19(inboundSmsHandler, smsReceiver);
        }
    }

    private void deleteFromRawTable19(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        XLog.d("Delete raw SMS data from database on Android 19+");
        Object deleteWhere = Reflector.getObjectField(smsReceiver, "mDeleteWhere");
        Object deleteWhereArgs = Reflector.getObjectField(smsReceiver, "mDeleteWhereArgs");

        callDeclaredMethod(SMS_HANDLER_CLASS, inboundSmsHandler, "deleteFromRawTable",
                /* String deleteWhere       */ deleteWhere,
                /* String[] deleteWhereArgs */ deleteWhereArgs);
    }

    private void deleteFromRawTable24(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        XLog.d("Delete raw SMS data from database on Android 24+");
        Object deleteWhere = Reflector.getObjectField(smsReceiver, "mDeleteWhere");
        Object deleteWhereArgs = Reflector.getObjectField(smsReceiver, "mDeleteWhereArgs");
        final int MARK_DELETED = 2;

        callDeclaredMethod(SMS_HANDLER_CLASS, inboundSmsHandler, "deleteFromRawTable",
                /* String deleteWhere       */ deleteWhere,
                /* String[] deleteWhereArgs */ deleteWhereArgs,
                /* int deleteType           */ MARK_DELETED);
    }

    private static Object callDeclaredMethod(String className, Object obj, String methodName, Object... args) throws InvocationTargetException, IllegalAccessException {
        // Reflector#callMethod() 方法，不能反射调用 private 的方法
        // 而本方法可以反射调用指定类的 private 方法
        Class<?> clz = Reflector.findClass(className, obj.getClass().getClassLoader());
        Method method = Reflector.findMethodBestMatch(clz, methodName, args);
        return method.invoke(obj, args);
    }

    private Context getPluginContext() {
        if (mPluginContext == null) {
            try {
                mPluginContext = mPhoneContext.createPackageContext(SMSCODE_PACKAGE,
                        Context.CONTEXT_IGNORE_SECURITY);
            } catch (Exception e) {
                XLog.e("Create plugin context failed: %s", e);
            }
        }
        return mPluginContext;
    }

}
