package com.tianma.xsmscode.xp.helper;


import com.tianma.xsmscode.common.utils.XLog;

import java.lang.reflect.Member;
import java.util.Set;

import com.tianma.xsmscode.xp.modern.HookCallback;
import com.tianma.xsmscode.xp.modern.HookRuntime;
import com.tianma.xsmscode.xp.modern.Reflector;

/**
 * Xposed Wrapper Utils
 */
public class XposedWrapper {

    private XposedWrapper() {
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Reflector.findClass(className, classLoader);
        } catch (Throwable t) {
            XLog.e("Class not found: %s", className);
            return null;
        }
    }

    public static HookCallback.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            return Reflector.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            XLog.e("Error in hook %s#%s", className, methodName, t);
            return null;
        }
    }

    public static HookCallback.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            return Reflector.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            XLog.e("Error in hook %s#%s", clazz.getName(), methodName, t);
            return null;
        }
    }

    public static HookCallback.Unhook hookMethod(Member hookMethod, HookCallback callback) {
        try {
            return HookRuntime.hookMethod(hookMethod, callback);
        } catch (Throwable t) {
            XLog.e("Error in hookMethod: %s", hookMethod.getName(), t);
            return null;
        }
    }

    public static Set<HookCallback.Unhook> hookAllConstructors(Class<?> hookClass, HookCallback callback) {
        try {
            return HookRuntime.hookAllConstructors(hookClass, callback);
        } catch (Throwable t) {
            XLog.e("Error in hookAllConstructors: %s", hookClass.getName(), t);
            return null;
        }
    }

    public static Set<HookCallback.Unhook> hookAllMethods(Class<?> hookClass, String methodName, HookCallback callback) {
        try {
            return HookRuntime.hookAllMethods(hookClass, methodName, callback);
        } catch (Throwable t) {
            XLog.e("Error in hookAllMethods: %s", hookClass.getName(), t);
            return null;
        }
    }

}
