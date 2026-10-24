# 本应用入口与工具页（清单声明的组件必须保留原名）
-keep class com.suibian.treasure_vault.** { *; }
-keep class com.suibian.treasure_vault.MainActivity { *; }

# ML Kit 端侧 OCR / 条码：内部大量反射与可选依赖
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# 保留注解与泛型签名，避免 Gson/JSON 反射类库出错
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
