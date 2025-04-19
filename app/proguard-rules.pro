-keep class org.drinkless.tdlib.** { *; }
-keep class org.drinkless.tdlib.TdApi$* { *; }
-keep class org.drinkless.tdlib.Client$* { *; }
-keep class okhttp3.internal.platform.** { *; }
-keep class org.bouncycastle.jsse.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.openjsse.** { *; }
-keep class com.gohj99.telewatch.model.ReleaseInfo { *; }
-keep class com.gohj99.telewatch.model.Asset { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
# 保留泛型签名，让 Gson 能看到你的 <T>
-keepattributes Signature
-keepattributes EnclosingMethod
# 保留 Gson 自身的 TypeToken 类
-keep class com.google.gson.reflect.TypeToken { *; }
# 保留所有继承了 Gson TypeToken 的匿名子类
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep public class * implements java.lang.reflect.Type { *; }
