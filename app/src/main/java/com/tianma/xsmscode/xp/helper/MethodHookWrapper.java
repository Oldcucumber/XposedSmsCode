package com.tianma.xsmscode.xp.helper;


import com.tianma.xsmscode.common.utils.XLog;

import com.tianma.xsmscode.xp.modern.HookCallback;

public abstract class MethodHookWrapper extends HookCallback {

    @Override
    final protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            before(param);
        } catch (Throwable t) {
            XLog.d("Error in hook %s", param.method.getName(), t);
        }
    }

    protected void before(MethodHookParam param) throws Throwable {
    }

    @Override
    final protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        try {
            after(param);
        } catch (Throwable t) {
            XLog.e("Error in hook %s", param.method.getName(), t);
        }
    }

    protected void after(MethodHookParam param) throws Throwable {
    }
}
