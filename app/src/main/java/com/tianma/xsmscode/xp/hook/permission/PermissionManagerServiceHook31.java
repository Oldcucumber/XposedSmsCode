package com.tianma.xsmscode.xp.hook.permission;

import static com.tianma.xsmscode.common.constant.PermConst.PACKAGE_PERMISSIONS;

import android.os.UserHandle;

import androidx.annotation.RequiresApi;

import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.xp.helper.MethodHookWrapper;
import com.tianma.xsmscode.xp.hook.BaseSubHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.tianma.xsmscode.xp.modern.HookCallback;
import com.tianma.xsmscode.xp.modern.HookRuntime;
import com.tianma.xsmscode.xp.modern.Reflector;

/**
 * Since Android 12 & 12L(API 31~32)<br/>
 * Hook com.android.server.pm.permission.PermissionManagerService
 */
public class PermissionManagerServiceHook31 extends BaseSubHook {
    // IMPORTANT: There are two types of permissions: install and runtime.

    // Android 12, API 31
    private static final String CLASS_PERMISSION_MANAGER_SERVICE = "com.android.server.pm.permission.PermissionManagerService";

    private static final String CLASS_ANDROID_PACKAGE = "com.android.server.pm.parsing.pkg.AndroidPackage";
    private static final String CLASS_PERMISSION_CALLBACK = "com.android.server.pm.permission.PermissionManagerService.PermissionCallback";


    public PermissionManagerServiceHook31(ClassLoader classLoader) {
        super(classLoader);
    }

    @RequiresApi(31)
    @Override
    public void startHook() {
        try {
            hookGrantPermissions();
        } catch (Throwable e) {
            XLog.e("Failed to hook PermissionManagerService", e);
        }
    }

    private void hookGrantPermissions() {
        XLog.d("Hooking grantPermissions() for Android 31+");
        Method method = findTargetMethod();
        if (method == null) {
            XLog.e("Cannot find the method to grant relevant permission");
        }
        HookRuntime.hookMethod(method, new MethodHookWrapper() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                afterGrantPermissionsSinceAndroid12(param);
            }
        });
    }

    private Method findTargetMethod() {
        Class<?> pmsClass = Reflector.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader);
        Class<?> androidPackageClass = Reflector.findClass(CLASS_ANDROID_PACKAGE, mClassLoader);
        Class<?> callbackClass = Reflector.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader);

        // 精确匹配
        Method method = Reflector.findMethodExactIfExists(pmsClass, "restorePermissionState",
                /* AndroidPackage pkg          */ androidPackageClass,
                /* boolean replace             */ boolean.class,
                /* String packageOfInterest    */ String.class,
                /* PermissionCallback callback */ callbackClass,
                /* int filterUserId            */ int.class);

        if (method == null) { // method restorePermissionState() not found
            // 参数类型精确匹配
            Method[] _methods = Reflector.findMethodsByExactParameters(pmsClass, Void.TYPE,
                    /* AndroidPackage pkg          */ androidPackageClass,
                    /* boolean replace             */ boolean.class,
                    /* String packageOfInterest    */ String.class,
                    /* PermissionCallback callback */ callbackClass,
                    /* int filterUserId            */ int.class);
            if (_methods != null && _methods.length > 0) {
                method = _methods[0];
            }
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    private void afterGrantPermissionsSinceAndroid12(HookCallback.MethodHookParam param) {
        // com.android.server.pm.parsing.pkg.AndroidPackage 对象
        Object pkg = param.args[0];

        final String _packageName = (String) Reflector.callMethod(pkg, "getPackageName");

        Set<String> packageSet = PACKAGE_PERMISSIONS.keySet();
        for (String packageName : packageSet) {
            if (packageName.equals(_packageName)) {
                XLog.d("PackageName: %s", packageName);

                // PermissionManagerService 对象
                Object permissionManagerService = param.thisObject;
                // PackageManagerInternal 对象 mPackageManagerInt
                Object mPackageManagerInt = Reflector.getObjectField(permissionManagerService, "mPackageManagerInt");

                // PackageSetting 对象 ps
                // final PackageSetting ps = (PackageSetting) mPackageManagerInt.getPackageSetting(pkg.getPackageName());
                final Object ps = Reflector.callMethod(mPackageManagerInt, "getPackageSetting", packageName);

                // Manifest.xml 中声明的permission列表
                // List<String> requestPermissions = pkg.getRequestPermissions();
                final List<String> requestedPermissions = (List<String>)
                        Reflector.callMethod(pkg, "getRequestedPermissions");

                // com.android.server.pm.permission.DevicePermissionState 对象
                final Object mState = Reflector.getObjectField(permissionManagerService, "mState");

                // UserHandle.USER_ALL
                int filterUserId = (int) param.args[4];
                final int USER_ALL = Reflector.getStaticIntField(UserHandle.class, "USER_ALL");
                final int[] userIds = filterUserId == USER_ALL
                        ? (int[]) Reflector.callMethod(permissionManagerService, "getAllUserIds")
                        : new int[]{filterUserId};

                List<String> permissionsToGrant = PACKAGE_PERMISSIONS.get(packageName);

                if (userIds != null) {
                    for (final int userId : userIds) {
                        // com.android.server.pm.permission.UserPermissionState 对象
                        Object userState = Reflector.callMethod(mState, "getOrCreateUserState", userId);
                        int appId = (int) Reflector.callMethod(ps, "getAppId");
                        //  com.android.server.pm.permission.UidPermissionState 对象
                        Object uidState = Reflector.callMethod(userState, "getOrCreateUidState", appId);

                        // com.android.server.pm.permission.PermissionRegistry 对象
                        Object mRegistry = Reflector.getObjectField(permissionManagerService, "mRegistry");

                        for (String permissionToGrant : permissionsToGrant) {
                            if (!requestedPermissions.contains(permissionToGrant)) {
                                boolean granted = (boolean) Reflector.callMethod(uidState, "isPermissionGranted", permissionToGrant);
                                if (!granted) {
                                    // permission not grant before
                                    final Object bpToGrant = Reflector.callMethod(mRegistry, "getPermission", permissionToGrant);
                                    boolean result = (boolean) Reflector.callMethod(uidState, "grantPermission", bpToGrant);
                                    XLog.d("Add " + permissionToGrant + "; result = " + result);
                                } else {
                                    // permission has been granted already
                                    XLog.d("Already have " + permissionToGrant + " permission");
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}
