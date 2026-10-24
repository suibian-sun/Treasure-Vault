plugins {
    id("com.android.application")
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.suibian.treasure_vault"
    compileSdk = flutter.compileSdkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.suibian.treasure_vault"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        // 只保留中文与英文资源，进一步压缩包体
        resourceConfigurations += listOf("zh", "en")
    }

    signingConfigs {
        // ------------------------------------------------------------------
        // 固定发布签名：keystore 由 CI 从 Repository Secrets 还原到
        // android/signing/treasure-vault.jks（仓库中不保存私钥）。
        // 每次构建前 CI 都会校验证书指纹，保证所有 APK 签名完全一致，
        // 从而支持应用内覆盖升级。
        // 本地构建：把 keystore 放到同路径即可；CI 中缺少密钥会直接失败，
        // 绝不会静默退回 debug 签名（否则已安装用户无法覆盖安装）。
        // 详见 SIGNING.md。
        // ------------------------------------------------------------------
        val keystore = file("../signing/treasure-vault.jks")
        if (!keystore.exists() && System.getenv("CI") == "true") {
            throw GradleException(
                "缺少签名密钥 android/signing/treasure-vault.jks。" +
                    "CI 需通过 KEYSTORE_BASE64 secret 注入，详见 SIGNING.md"
            )
        }
        if (keystore.exists()) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "TreasureVault2026!"
                keyAlias = System.getenv("KEY_ALIAS") ?: "treasure-vault"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "TreasureVault2026!"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // 本地无密钥时退回 debug 签名（仅便于调试），CI 已在上面直接拦截
            signingConfig = if (signingConfigs.findByName("release") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // 资源压缩 + R8 代码裁剪：去掉未使用的 Material 组件与字体子集，
            // 是「低内存、小包体」目标的直接手段。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isDebuggable = false
        }
    }

    // 低内存设备上 release lint 容易 OOM，且不影响交付
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
