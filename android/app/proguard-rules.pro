# ---------------------------------------------------------------------------
# R8 规则（release 构建启用 minifyEnabled + shrinkResources）
# ---------------------------------------------------------------------------

# Flutter 宿主与 MethodChannel 入口由引擎反射调用，必须保留。
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-keep class io.flutter.embedding.** { *; }

# 本应用的原生入口：清单中声明的组件必须原样保留，
# 否则 R8 裁剪后启动会 ClassNotFoundException。
-keep class com.suibian.treasure_vault.** { *; }
-keep class com.suibian.treasure_vault.MainActivity { *; }
-keep public class * extends io.flutter.embedding.android.FlutterActivity { *; }
-keep public class * extends android.app.Application { *; }

# AndroidX 注解（R8 在裁剪时需要的元数据）
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 移除日志调用，减小体积并避免泄露调试信息
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Flutter 引擎里对 Play Core 的引用属于「延迟组件」可选能力，
# 未接入 Play Core 依赖时 R8 会报 Missing class，这里显式忽略。
-dontwarn com.google.android.play.core.**
-dontwarn io.flutter.embedding.engine.deferredcomponents.**
