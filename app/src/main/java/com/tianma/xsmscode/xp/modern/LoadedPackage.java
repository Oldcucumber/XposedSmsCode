package com.tianma.xsmscode.xp.modern;
public final class LoadedPackage {
    public final String packageName, processName;
    public final ClassLoader classLoader;
    public LoadedPackage(String name, String process, ClassLoader loader) {
        packageName = name; processName = process; classLoader = loader;
    }
}
