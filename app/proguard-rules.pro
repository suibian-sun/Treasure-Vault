# 本应用入口与工具页（清单声明的组件必须保留原名）
-keep class com.suibian.treasure_vault.** { *; }
-keep class com.suibian.treasure_vault.MainActivity { *; }
-keep class com.suibian.treasure_vault.VaultApp { *; }

# ML Kit 端侧 OCR / 条码：内部大量反射与按 manifest 实例化的注册器
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_**$* { *; }
-dontwarn com.google.mlkit.**

# Firebase Components：ML Kit 通过 meta-data 里的类名反射实例化注册器，
# 类名一旦被混淆，启动或首次识别时就会 ClassNotFoundException。
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.components.** { *; }
-dontwarn com.google.firebase.**

# Google Play services 基础件（GoogleApiActivity、错误处理、可用性检查）
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-dontwarn com.google.android.gms.**

# 日志上报通道（清单里声明为 service）
-keep class com.google.android.datatransport.** { *; }
-dontwarn com.google.android.datatransport.**

# AndroidX Startup（保留类本身，即使已从清单移除，防止被其他地方引用）
-keep class androidx.startup.** { *; }

# 保留注解与泛型签名，避免反射类库出错
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
