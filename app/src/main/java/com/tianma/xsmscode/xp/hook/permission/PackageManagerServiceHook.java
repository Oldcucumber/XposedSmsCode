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
 * Android 4.4 ~ Android 8.1 (API 19 - 27)<br/>
 * Hook com.android.server.pm.PackageManagerService
 */
public class PackageManagerServiceHook extends BaseSubHook {

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    public PackageManagerServiceHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void startHook() {
        try {
            hookGrantPermissionsLPw();
        } catch (Throwable e) {
            XLog.e("Failed to hook PackageManagerService", e);
        }
    }

    private void hookGrantPermissionsLPw() {
        Class pmsClass = XposedWrapper.findClass(CLASS_PACKAGE_MANAGER_SERVICE, mClassLoader);
        Method method;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0 +
            XLog.d("Hooking grantPermissionsLPw() for Android 21+");
            method = Reflector.findMethodExact(pmsClass, "grantPermissionsLPw",
                    /* PackageParser.Package pkg */ CLASS_PACKAGE_PARSER_PACKAGE,
                    /* boolean replace           */ boolean.class,
                    /* String packageOfInterest  */ String.class);
        } else {
            // Android 4.4 +
            XLog.d("Hooking grantPermissionsLPw() for Android 19+");
            method = Reflector.findMethodExact(pmsClass, "grantPermissionsLPw",
                    /* PackageParser.Package pkg */ CLASS_PACKAGE_PARSER_PACKAGE,
                    /* boolean replace           */ boolean.class);
        }

        HookRuntime.hookMethod(method, new MethodHookWrapper() {
            @Override
            protected void after(MethodHookParam param) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    grantPermissionsLPwSinceM(param);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    grantPermissionsLPwSinceK(param);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void grantPermissionsLPwSinceM(HookCallback.MethodHookParam param) {
        // API 23 (Android 6.0)

        // android.content.pm.PackageParser.Package 对象
        Object pkg = param.args[0];

        final String _packageName = (String) Reflector.getObjectField(pkg, "packageName");

        Set<String> packageSet = PACKAGE_PERMISSIONS.keySet();
        for (String packageName : packageSet) {
            if (packageName.equals(_packageName)) {
                XLog.d("PackageName: %s", packageName);
                // PackageParser$Package.mExtras 实际上是 com.android.server.pm.PackageSetting mExtras 对象
                final Object extras = Reflector.getObjectField(pkg, "mExtras");
                // com.android.server.pm.PermissionsState 对象
                final Object permissionsState = Reflector.callMethod(extras, "getPermissionsState");

                // Manifest.xml 中声明的permission列表
                final List<String> requestedPermissions = (List<String>)
                        Reflector.getObjectField(pkg, "requestedPermissions");

                // com.android.server.pm.Settings mSettings 对象
                final Object settings = Reflector.getObjectField(param.thisObject, "mSettings");
                // ArrayMap<String, com.android.server.pm.BasePermission> mPermissions 对象
                final Object permissions = Reflector.getObjectField(settings, "mPermissions");

                List<String> permissionsToGrant = PACKAGE_PERMISSIONS.get(packageName);
                for (String permissionToGrant : permissionsToGrant) {
                    if (!requestedPermissions.contains(permissionToGrant)) {
                        boolean granted = (boolean) Reflector.callMethod(
                                permissionsState, "hasInstallPermission", permissionToGrant);
                        if (!granted) {
                            // com.android.server.pm.BasePermission bpToGrant
                            final Object bpToGrant = Reflector.callMethod(permissions, "get", permissionToGrant);
                            int result = (int) Reflector.callMethod(permissionsState, "grantInstallPermission", bpToGrant);
                            XLog.d("Add " + bpToGrant + "; result = " + result);
                        } else {
                            XLog.d("Already have " + permissionToGrant + " permission");
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void grantPermissionsLPwSinceK(HookCallback.MethodHookParam param) {
        // API 19 (Android 4.4)

        // android.content.pm.PackageParser.Package object
        Object pkg = param.args[0];

        final String _packageName = (String) Reflector.getObjectField(pkg, "packageName");

        Set<String> packageSet = PACKAGE_PERMISSIONS.keySet();
        for (String packageName : packageSet) {
            if (packageName.equals(_packageName)) {
                XLog.d("PackageName: %s", packageName);
                // com.android.server.pm.PackageSetting mExtra object
                final Object extra = Reflector.getObjectField(pkg, "mExtras");
                // PackageSetting extends PackageSettingBase
                // PackageSettingBase extends GrantedPermissions
                // Android 4.4~4.4.4 api 19 HashSet<String>
                // Android 5.0 api 21 HashSet<String>
                // Android 5.1 api 22 ArraySet<String>
                final Set<String> grantedPermissions = (Set<String>)
                        Reflector.getObjectField(extra, "grantedPermissions");

                // com.android.server.pm.Settings mSettings object
                final Object settings = Reflector.getObjectField(param.thisObject, "mSettings");
                // HashMap<String, com.android.server.pm.BasePermission> mPermissions obj
                final Object permissions = Reflector.getObjectField(settings, "mPermissions");

                List<String> permissionsToGrant = PACKAGE_PERMISSIONS.get(packageName);
                for (String permissionToGrant : permissionsToGrant) {
                    if (!grantedPermissions.contains(permissionToGrant)) {
                        // com.android.server.pm.BasePermission
                        final Object bpToGrant = Reflector.
                                callMethod(permissions, "get", permissionToGrant);
                        grantedPermissions.add(permissionToGrant);

                        // granted permission gids
                        int[] gpGids = (int[]) Reflector.getObjectField(extra, "gids");
                        // base permission to grant gids
                        int[] bpGids = (int[]) Reflector.getObjectField(bpToGrant, "gids");
                        Reflector.callStaticMethod(
                                param.thisObject.getClass(), "appendInts", gpGids, bpGids);

                        XLog.d("Add " + bpToGrant);
                    } else {
                        XLog.d("Already have " + permissionToGrant + " permission");
                    }
                }
            }
        }
    }
}
