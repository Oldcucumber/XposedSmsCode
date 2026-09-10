package com.tianma.xsmscode.xp;

import io.github.libxposed.api.XposedModule;
import com.tianma.xsmscode.xp.modern.*;
import com.tianma.xsmscode.xp.hook.code.SmsHandlerHook;
import com.tianma.xsmscode.xp.hook.permission.PermissionGranterHook;
import com.tianma.xsmscode.common.utils.XLog;

public final class HookEntry extends XposedModule {
    private String processName;
    @Override public void onModuleLoaded(ModuleLoadedParam p) {
        HookRuntime.api = this; processName = p.getProcessName();
        XLog.setFrameworkLogger((priority, tag, message) -> log(priority, tag, message));
        XLog.i("Modern API %d loaded in %s", getApiVersion(), processName);
    }
    @Override public void onPackageReady(PackageReadyParam p) {
        if (SmsHandlerHook.ANDROID_PHONE_PACKAGE.equals(p.getPackageName()))
            new SmsHandlerHook().onLoadPackage(new LoadedPackage(p.getPackageName(), processName, p.getClassLoader()));
    }
    @Override public void onSystemServerStarting(SystemServerStartingParam p) {
        XLog.i("System server permission hooks initializing");
        new PermissionGranterHook().onLoadPackage(new LoadedPackage("android", "android", p.getClassLoader()));
    }
}
