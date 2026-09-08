package com.tianma.xsmscode.xp.hook.permission;

import android.content.pm.PackageManager;
import com.tianma.xsmscode.common.constant.PermConst;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.xp.hook.code.SmsHandlerHook;
import com.tianma.xsmscode.xp.modern.*;
import java.lang.reflect.Method;
import java.util.*;

/** Android 15/16 access-state service. No mutation of obsolete mState fields. */
public final class PermissionServiceHook35 {
    public static boolean install(ClassLoader loader) {
        Class<?> service = Reflector.findClassIfExists("com.android.server.permission.access.permission.PermissionService", loader);
        if (service == null) return false;
        Method uid = Reflector.findMethodExactIfExists(service, "checkUidPermission", int.class, String.class, String.class);
        Method pkg = Reflector.findMethodExactIfExists(service, "checkPermission", String.class, String.class, String.class, int.class);
        if (uid == null || pkg == null) return false;
        HookRuntime.hookMethod(uid, new HookCallback() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                int targetUid = (int)p.args[0];
                if (targetUid % 100000 != 1001 || !phonePermission((String)p.args[1]) || p.hasThrowable()) return;
                Object pm = Reflector.getObjectField(p.thisObject, "packageManagerInternal");
                Object app = Reflector.callMethod(pm, "getPackage", targetUid);
                if (app != null && SmsHandlerHook.ANDROID_PHONE_PACKAGE.equals(Reflector.callMethod(app, "getPackageName"))
                        && userExists(p.thisObject, targetUid / 100000)) p.setResult(PackageManager.PERMISSION_GRANTED);
            }
        });
        HookRuntime.hookMethod(pkg, new HookCallback() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!p.hasThrowable() && SmsHandlerHook.ANDROID_PHONE_PACKAGE.equals(p.args[0])
                        && phonePermission((String)p.args[1]) && userExists(p.thisObject, (int)p.args[3]))
                    p.setResult(PackageManager.PERMISSION_GRANTED);
            }
        });
        XLog.i("Android 15/16 PermissionService hooks installed");
        return true;
    }
    private static boolean userExists(Object service, int id) {
        return (boolean) Reflector.callMethod(Reflector.getObjectField(service, "userManagerInternal"), "exists", id);
    }
    private static boolean phonePermission(String permission) {
        return PermConst.PACKAGE_PERMISSIONS.get(SmsHandlerHook.ANDROID_PHONE_PACKAGE).contains(permission);
    }
}
