package com.tianma.xsmscode.common.utils;

import android.util.Log;

import com.github.tianma8023.xposed.smscode.BuildConfig;

public class XLog {

    private static final String LOG_TAG = BuildConfig.LOG_TAG;
    private static int sLogLevel = BuildConfig.LOG_LEVEL;
    private static final boolean LOG_TO_XPOSED = BuildConfig.LOG_TO_XPOSED;
    private static volatile FrameworkLogger frameworkLogger;

    // The standalone app has no libxposed API classes. Install this bridge only
    // from the module entry, rather than loading HookRuntime from app logging.
    public interface FrameworkLogger {
        void log(int priority, String tag, String message);
    }

    public static void setFrameworkLogger(FrameworkLogger logger) {
        frameworkLogger = logger;
    }

    private XLog() {
    }

    private static void log(int priority, String message, Object... args) {
        if (priority < sLogLevel)
            return;

        message = String.format(message, args);

        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Throwable throwable = (Throwable) args[args.length - 1];
            String stacktraceStr = Log.getStackTraceString(throwable);
            message += '\n' + stacktraceStr;
        }

        // Write to the default log tag
        Log.println(priority, LOG_TAG, message);

        // Duplicate to the Xposed log if enabled
        if (LOG_TO_XPOSED) {
            FrameworkLogger logger = frameworkLogger;
            if (logger != null) {
                try { logger.log(priority, LOG_TAG, message); }
                catch (RuntimeException | LinkageError error) {
                    // Logging failures must not disrupt SMS delivery or app startup.
                    Log.e(LOG_TAG, "Framework logging unavailable", error);
                }
            }
        }
    }

    public static void v(String message, Object... args) {
        log(Log.VERBOSE, message, args);
    }

    public static void d(String message, Object... args) {
        log(Log.DEBUG, message, args);
    }

    public static void i(String message, Object... args) {
        log(Log.INFO, message, args);
    }

    public static void w(String message, Object... args) {
        log(Log.WARN, message, args);
    }

    public static void e(String message, Object... args) {
        log(Log.ERROR, message, args);
    }

    public static void setLogLevel(int logLevel) {
        sLogLevel = logLevel;
    }
}
