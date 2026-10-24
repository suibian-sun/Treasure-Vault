# ---------------------------------------------------------------------------
# R8 规则（release 构建启用 minifyEnabled + shrinkResources）
# ---------------------------------------------------------------------------

# Flutter 宿主与 MethodChannel 入口由引擎反射调用，必须保留。
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-keep class io.flutter.embedding.** { *; }

# 本应用的原生入口
-keep class com.suibian.treasure_vault.** { *; }

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
