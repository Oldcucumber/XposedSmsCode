package com.tianma.xsmscode.xp.hook.permission;

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

import static com.tianma.xsmscode.common.constant.PermConst.PACKAGE_PERMISSIONS;

/**
 * Since Android 11(API 30)<br/>
 * Hook com.android.server.pm.permission.PermissionManagerService
 */
public class PermissionManagerServiceHook30 extends BaseSubHook {
    // IMPORTANT: There are two types of permissions: install and runtime.

    // Android 11, API 30
    private static final String CLASS_PERMISSION_MANAGER_SERVICE = "com.android.server.pm.permission.PermissionManagerService";

    private static final String CLASS_ANDROID_PACKAGE = "com.android.server.pm.parsing.pkg.AndroidPackage";
    private static final String CLASS_PERMISSION_CALLBACK = "com.android.server.pm.permission.PermissionManagerServiceInternal.PermissionCallback";



    public PermissionManagerServiceHook30(ClassLoader classLoader) {
        super(classLoader);
    }

    @RequiresApi(30)
    @Override
    public void startHook() {
        try {
            hookGrantPermissions();
        } catch (Throwable e) {
            XLog.e("Failed to hook PermissionManagerService", e);
        }
    }

    private void hookGrantPermissions() {
        XLog.d("Hooking grantPermissions() for Android 30+");
        Method method = findTargetMethod();
        if (method == null) {
            XLog.e("Cannot find the method to grant relevant permission");
        }
        HookRuntime.hookMethod(method, new MethodHookWrapper() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                afterGrantPermissionsSinceAndroid11(param);
            }
        });
    }

    private Method findTargetMethod() {
        Class<?> pmsClass = Reflector.findClass(CLASS_PERMISSION_MANAGER_SERVICE, mClassLoader);
        Class<?> androidPackageClass = Reflector.findClass(CLASS_ANDROID_PACKAGE, mClassLoader);
        Class<?> callbackClass = Reflector.findClassIfExists(CLASS_PERMISSION_CALLBACK, mClassLoader);

        Method method = Reflector.findMethodExactIfExists(pmsClass, "restorePermissionState",
                /* AndroidPackage pkg   */ androidPackageClass,
                /* boolean replace             */ boolean.class,
                /* String packageOfInterest    */ String.class,
                /* PermissionCallback callback */ callbackClass);

        if (method == null) { // method restorePermissionState() not found
            Method[] _methods = Reflector.findMethodsByExactParameters(pmsClass, Void.TYPE,
                    /* AndroidPackage pkg   */ androidPackageClass,
                    /* boolean replace             */ boolean.class,
                    /* String packageOfInterest    */ String.class,
                    /* PermissionCallback callback */ callbackClass);
            if (_methods != null && _methods.length > 0) {
                method = _methods[0];
            }
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    private void afterGrantPermissionsSinceAndroid11(HookCallback.MethodHookParam param) {
        // com.android.server.pm.parsing.pkg.AndroidPackage 对象
        Object pkg = param.args[0];

        // final String _packageName = (String) Reflector.getObjectField(pkg, "packageName");
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

                // com.android.server.pm.permission.PermissionsState 对象
                final Object permissionsState = Reflector.callMethod(ps, "getPermissionsState");

                // Manifest.xml 中声明的permission列表
                // List<String> requestPermissions = pkg.getRequestPermissions();
                final List<String> requestedPermissions = (List<String>)
                        Reflector.callMethod(pkg, "getRequestedPermissions");

                // com.android.server.pm.permission.PermissionSettings mSettings 对象
                final Object settings = Reflector.getObjectField(permissionManagerService, "mSettings");
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
