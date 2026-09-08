package com.tianma.xsmscode.xp.hook.permission;

import android.os.Build;

import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.xp.helper.MethodHookWrapper;
import com.tianma.xsmscode.xp.helper.XposedWrapper;
import com.tianma.xsmscode.xp.hook.BaseSubHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import androidx.annotation.RequiresApi;
import com.tianma.xsmscode.xp.modern.HookCallback;
import com.tianma.xsmscode.xp.modern.HookRuntime;
import com.tianma.xsmscode.xp.modern.Reflector;

import static com.tianma.xsmscode.common.constant.PermConst.PACKAGE_PERMISSIONS;

/**
 * Since Android P(API 28)<br/>
 * Hook com.android.server.pm.permission.PermissionManagerService
 */
public class PermissionManagerServiceHook extends BaseSubHook {
    // IMPORTANT: There are two types of permissions: install and runtime.

    // for Android 28+
    private static final String CLASS_PERMISSION_MANAGER_SERVICE = "com.android.server.pm.permission.PermissionManagerService";
    private static final String CLASS_PERMISSION_CALLBACK = "com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    // for MIUI 10 Android Q
    private static final String CLASS_PERMISSION_CALLBACK_Q = "com.android.server.pm.permission.PermissionManagerServiceInternal.PermissionCallback";

    public PermissionManagerServiceHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Override
    public void startHook() {
        try {
            hookGrantPermissions();
        } catch (Throwable e) {
            XLog.e("Failed to hook PermissionManagerService", e);
        }
    }

    private void hookGrantPermissions() {
        XLog.d("Hooking grantPermissions() for Android 28+");
        Method method = findTargetMethod();
        HookRuntime.hookMethod(method, new MethodHookWrapper() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                afterGrantPermissionsSinceP(param);
            }
        });
    }

    private Method findTargetMethod() {
        Class<?> pmsClass = Reflector.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader);
        Class<?> packageClass = Reflector.findClass(CLASS_PACKAGE_PARSER_PACKAGE, mClassLoader);
        Class<?> callbackClass = Reflector.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader);
        if (callbackClass == null) {
            // Android Q PermissionCallback 不一样
            callbackClass = XposedWrapper.findClass(CLASS_PERMISSION_CALLBACK_Q, mClassLoader);
        }

        Method method = Reflector.findMethodExactIfExists(pmsClass, "grantPermissions",
                /* PackageParser.Package pkg   */ packageClass,
                /* boolean replace             */ boolean.class,
                /* String packageOfInterest    */ String.class,
                /* PermissionCallback callback */ callbackClass);

        if (method == null) { // method grantPermissions() not found
            // Android Q
            method = Reflector.findMethodExactIfExists(pmsClass, "restorePermissionState",
                    /* PackageParser.Package pkg   */ packageClass,
                    /* boolean replace             */ boolean.class,
                    /* String packageOfInterest    */ String.class,
                    /* PermissionCallback callback */ callbackClass);
            if (method == null) { // method restorePermissionState() not found
                Method[] _methods = Reflector.findMethodsByExactParameters(pmsClass, Void.TYPE,
                        /* PackageParser.Package pkg   */ packageClass,
                        /* boolean replace             */ boolean.class,
                        /* String packageOfInterest    */ String.class,
                        /* PermissionCallback callback */ callbackClass);
                if (_methods != null && _methods.length > 0) {
                    method = _methods[0];
                }
            }
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    private void afterGrantPermissionsSinceP(HookCallback.MethodHookParam param) {
        // android.content.pm.PackageParser.Package 对象
        Object pkg = param.args[0];

        final String _packageName = (String) Reflector.getObjectField(pkg, "packageName");

        Set<String> packageSet = PACKAGE_PERMISSIONS.keySet();
        for (String packageName : packageSet) {
            if (packageName.equals(_packageName)) {
                XLog.d("PackageName: %s", packageName);
                // PackageParser$Package.mExtras 实际上是 com.android.server.pm.PackageSetting mExtras 对象
                final Object extras = Reflector.getObjectField(pkg, "mExtras");
                // com.android.server.pm.permission.PermissionsState 对象
                final Object permissionsState = Reflector.callMethod(extras, "getPermissionsState");

                // Manifest.xml 中声明的permission列表
                final List<String> requestedPermissions = (List<String>)
                        Reflector.getObjectField(pkg, "requestedPermissions");

                // com.android.server.pm.permission.PermissionSettings mSettings 对象
                final Object settings = Reflector.getObjectField(param.thisObject, "mSettings");
                // ArrayMap<String, com.android.server.pm.permission.BasePermission> mPermissions 对象
                final Object permissions = Reflector.getObjectField(settings, "mPermissions");

                List<String> permissionsToGrant = PACKAGE_PERMISSIONS.get(packageName);
                for (String permissionToGrant : permissionsToGrant) {
                    if (!requestedPermissions.contains(permissionToGrant)) {
                        boolean granted = (boolean) Reflector.callMethod(
                                permissionsState, "hasInstallPermission", permissionToGrant);
                        // grant permissions
                        if (!granted) {
                            // com.android.server.pm.permission.BasePermission bpToGrant
                            final Object bpToGrant = Reflector.callMethod(permissions, "get", permissionToGrant);
                            int result = (int) Reflector.callMethod(permissionsState, "grantInstallPermission", bpToGrant);
                            XLog.d("Add " + bpToGrant + "; result = " + result);
                        } else {
                            XLog.d("Already have " + permissionToGrant + " permission");
                        }
                        // revoke permissions
                        // if (!granted) {
                        //     XLog.d("Don't have " + permissionToGrant + " permission");
                        // } else {
                        //     XLog.d("Already have " + permissionToGrant + " permission");
                        //     // com.android.server.pm.permission.BasePermission bpToGrant
                        //     final Object bpToGrant = Reflector.callMethod(permissions, "get", permissionToGrant);
                        //     int result = (int) Reflector.callMethod(permissionsState, "revokeInstallPermission", bpToGrant);
                        //     XLog.d("Remove permission " + bpToGrant + "; result = " + result);
                        // }
                    }
                }
            }
        }
    }


}
