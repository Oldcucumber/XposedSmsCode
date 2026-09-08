package com.tianma.xsmscode.xp.modern;

import com.tianma.xsmscode.common.utils.XLog;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.*;
import java.util.*;

public final class HookRuntime {
    public static XposedInterface api;
    private static final Set<Member> hooked = new HashSet<>();
    public static synchronized HookCallback.Unhook hookMethod(Member method, HookCallback callback) {
        if (!hooked.add(method)) return null;
        try {
            return new HookCallback.Unhook(api.hook((Executable) method).intercept(chain -> {
                HookCallback.MethodHookParam p = new HookCallback.MethodHookParam();
                p.method = method; p.thisObject = chain.getThisObject(); p.args = chain.getArgs().toArray();
                try { callback.beforeHookedMethod(p); }
                catch (Throwable error) { XLog.e("Before hook failed: %s", method, error); p.skip = false; p.throwable = null; }
                if (!p.skip) {
                    try { p.result = chain.proceed(p.args); }
                    catch (Throwable error) { p.throwable = error; }
                }
                Object result = p.result; Throwable error = p.throwable;
                try { callback.afterHookedMethod(p); }
                catch (Throwable hookError) { XLog.e("After hook failed: %s", method, hookError); p.result = result; p.throwable = error; }
                if (p.throwable != null) throw p.throwable;
                return p.result;
            }));
        } catch (Throwable e) { hooked.remove(method); throw e; }
    }
    public static Set<HookCallback.Unhook> hookAllConstructors(Class<?> type, HookCallback cb) {
        Set<HookCallback.Unhook> result = new HashSet<>();
        for (Constructor<?> c : type.getDeclaredConstructors()) result.add(hookMethod(c, cb));
        return result;
    }
    public static Set<HookCallback.Unhook> hookAllMethods(Class<?> type, String name, HookCallback cb) {
        Set<HookCallback.Unhook> result = new HashSet<>();
        for (Method m : type.getDeclaredMethods()) if (m.getName().equals(name)) result.add(hookMethod(m, cb));
        return result;
    }
    public static int getXposedVersion() { return api.getApiVersion(); }
    public static final int XPOSED_BRIDGE_VERSION = 102;
}
