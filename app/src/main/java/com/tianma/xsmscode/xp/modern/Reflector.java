package com.tianma.xsmscode.xp.modern;

import java.lang.reflect.*;
import java.util.*;

/** System-member reflection only; modern Xposed APIs are always invoked directly. */
public final class Reflector {
    public static Class<?> findClass(String name, ClassLoader loader) {
        try { return Class.forName(name, false, loader); }
        catch (ClassNotFoundException e) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) return findClass(name.substring(0, dot) + "$" + name.substring(dot + 1), loader);
            throw new IllegalArgumentException(name, e);
        }
    }
    public static Class<?> findClassIfExists(String name, ClassLoader loader) {
        try { return findClass(name, loader); } catch (IllegalArgumentException e) { return null; }
    }
    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new IllegalArgumentException(type + "." + name);
    }
    public static Object getObjectField(Object target, String name) {
        try { return field(target.getClass(), name).get(target); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static int getStaticIntField(Class<?> type, String name) {
        try { return field(type, name).getInt(null); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }
    public static Method findMethodBestMatch(Class<?> type, String name, Object... args) {
        Method best = null; int bestScore = Integer.MAX_VALUE;
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] pts = m.getParameterTypes();
                if (!m.getName().equals(name) || pts.length != args.length) continue;
                int score = 0; boolean match = true;
                for (int i = 0; i < pts.length; i++) {
                    if (args[i] == null) { if (pts[i].isPrimitive()) match = false; else score += 10; }
                    else if (!boxed(pts[i]).isInstance(args[i])) match = false;
                    else if (boxed(pts[i]) != args[i].getClass()) score++;
                }
                if (match && score < bestScore) { best = m; bestScore = score; }
            }
        }
        if (best == null) throw new IllegalArgumentException("No matching " + type + "." + name);
        best.setAccessible(true); return best;
    }
    public static Method findMethodExact(Class<?> type, String name, Class<?>... parameters) {
        Method m = findMethodExactIfExists(type, name, parameters);
        if (m == null) throw new IllegalArgumentException("No exact " + type + "." + name);
        return m;
    }
    public static Method findMethodExact(Class<?> type, String name, String firstType, Class<?>... remaining) {
        Class<?>[] types = new Class<?>[remaining.length + 1];
        types[0] = findClass(firstType, type.getClassLoader());
        System.arraycopy(remaining, 0, types, 1, remaining.length);
        return findMethodExact(type, name, types);
    }
    public static Method findMethodExactIfExists(Class<?> type, String name, Class<?>... parameters) {
        if (type == null || Arrays.asList(parameters).contains(null)) return null;
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { Method m = c.getDeclaredMethod(name, parameters); m.setAccessible(true); return m; }
            catch (NoSuchMethodException ignored) { }
        }
        return null;
    }
    public static Method[] findMethodsByExactParameters(Class<?> type, Class<?> result, Class<?>... params) {
        return Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getReturnType() == result
                && Arrays.equals(m.getParameterTypes(), params)).toArray(Method[]::new);
    }
    private static Object invoke(Method method, Object target, Object[] args) {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) { throw new IllegalStateException(e.getCause()); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static Object callMethod(Object target, String name, Object... args) {
        return invoke(findMethodBestMatch(target.getClass(), name, args), target, args);
    }
    public static Object callMethod(Object target, String name, Class<?>[] params, Object... args) {
        // The original caller passed KeyEvent for an InputEvent parameter.
        Method m = findMethodExactIfExists(target.getClass(), name, params);
        return invoke(m != null ? m : findMethodBestMatch(target.getClass(), name, args), target, args);
    }
    public static Object callStaticMethod(Class<?> type, String name, Object... args) {
        return invoke(findMethodBestMatch(type, name, args), null, args);
    }
    private static Class<?>[] types(ClassLoader loader, Object[] args) {
        Class<?>[] result = new Class<?>[args.length - 1];
        for (int i = 0; i < result.length; i++) result[i] = args[i] instanceof Class
                ? (Class<?>) args[i] : findClass((String) args[i], loader);
        return result;
    }
    public static HookCallback.Unhook findAndHookMethod(String name, ClassLoader loader, String method, Object... args) {
        return findAndHookMethod(findClass(name, loader), method, args);
    }
    public static HookCallback.Unhook findAndHookMethod(Class<?> type, String name, Object... args) {
        return HookRuntime.hookMethod(findMethodExact(type, name, types(type.getClassLoader(), args)), (HookCallback) args[args.length - 1]);
    }
    public static HookCallback.Unhook findAndHookConstructor(String name, ClassLoader loader, Object... args) {
        try { return HookRuntime.hookMethod(findClass(name, loader).getDeclaredConstructor(types(loader, args)), (HookCallback) args[args.length - 1]); }
        catch (NoSuchMethodException e) { throw new IllegalArgumentException(e); }
    }
}
