-keep public class com.tianma.xsmscode.xp.HookEntry { public <init>(); }
-dontwarn io.github.libxposed.annotation.**
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.jaredrummler.cyanea.Cyanea { private void loadDefaults(); }
# ==========================
# Umeng analyze proguard start

#-keepclassmembers class * {
#   public <init> (org.json.JSONObject);
#}
#
#-keep public class com.github.tianma8023.xposed.smscode.R$*{
#public static final int *;
#}
#
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-keep class com.umeng.** {*;}

# Umeng analyze proguard end
# ==========================


# ==========================
# event bus proguard start

-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# event bus proguard end
# ==========================


# ==========================
# greenDAO 3 proguard start
### greenDAO 3
### GreenDaoUpgradeHelper
-keep class * extends org.greenrobot.greendao.AbstractDao { *; }
-keep class com.tianma.xsmscode.data.db.entity.AppInfoDao { *; }
-keep class com.tianma.xsmscode.data.db.entity.SmsCodeRuleDao { *; }
-keep class com.tianma.xsmscode.data.db.entity.SmsMsgDao { *; }
-keep class com.tianma.xsmscode.data.db.entity.SmsCodeRule { *; }
-keep class com.tianma.xsmscode.data.db.entity.AppInfo { *; }
-keep class com.tianma.xsmscode.data.db.entity.SmsMsg { *; }
-keep class com.tianma.xsmscode.data.http.entity.GithubRelease { *; }
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
    public static java.lang.String TABLENAME;
    public static void dropTable(org.greenrobot.greendao.database.Database, boolean);
    public static void createTable(org.greenrobot.greendao.database.Database, boolean);
}
-keep class **$Properties {*;}

# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use RxJava:
-dontwarn rx.**

# greenDAO 3 proguard end
# ==========================


# ==========================
# bugly proguard start

#-dontwarn com.tencent.bugly.**
#-keep public class com.tencent.bugly.** {
#    *;
#}

# bugly proguard end
# ==========================


# ==========================
# jsoup proguard start
-keeppackagenames org.jsoup.nodes
# jsoup proguard end
# ==========================

# ==========================
# Retrofit service signatures
# R8 full mode also needs the generic return type retained, otherwise it can
# erase Observable<T> to raw Observable even when the service is kept.
-keep,allowoptimization,allowobfuscation class io.reactivex.Observable
-keep interface com.tianma.xsmscode.data.http.service.GithubService { *; }
-keep interface com.tianma.xsmscode.data.http.service.CoolApkService { *; }
# ==========================

# ==========================
# Gson annotated models
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
# ==========================


# ==========================
# okhttp3 start
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
# okhttp3 end
# ==========================


# ==========================
# okio start
# okio end
# ==========================
