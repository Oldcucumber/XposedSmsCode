package com.tianma.xsmscode.xp.modern;

import java.lang.reflect.Member;

/** Internal before/after adapter, backed exclusively by modern interception. */
public abstract class HookCallback {
    protected void beforeHookedMethod(MethodHookParam p) throws Throwable { }
    protected void afterHookedMethod(MethodHookParam p) throws Throwable { }
    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        Object result;
        Throwable throwable;
        boolean skip;
        public Object getResult() { return result; }
        public Throwable getThrowable() { return throwable; }
        public boolean hasThrowable() { return throwable != null; }
        public void setResult(Object value) { result = value; throwable = null; skip = true; }
        public void setThrowable(Throwable error) { throwable = error; skip = true; }
    }
    public static final class Unhook {
        private final io.github.libxposed.api.XposedInterface.HookHandle handle;
        Unhook(io.github.libxposed.api.XposedInterface.HookHandle handle) { this.handle = handle; }
        public void unhook() { handle.unhook(); }
    }
}
