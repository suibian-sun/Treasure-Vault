plugins {
    id("com.android.application")
}

android {
    namespace = "com.suibian.treasure_vault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.suibian.treasure_vault"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "2.1.2"
        resourceConfigurations += listOf("zh", "en")
    }

    signingConfigs {
        // keystore 由 CI 从 Secrets 还原；本地构建请放到 signing/treasure-vault.jks
        val ks = file("../signing/treasure-vault.jks")
        if (!ks.exists() && System.getenv("CI") == "true") {
            throw GradleException("缺少签名密钥 signing/treasure-vault.jks，CI 需注入 KEYSTORE_BASE64")
        }
        if (ks.exists()) {
            create("release") {
                storeFile = ks
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
            signingConfig = if (signingConfigs.findByName("release") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // 按 ABI 分包：OCR 依赖携带较大的原生库，拆包后单个安装包体积减半。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // 刻意只用 appcompat + core：不引入 Compose / Material Components，
    // 单 Activity + 原生 View，安装包与常驻内存都更小。
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // 端侧 OCR / 条码（无 Key，模型按需下载，不进 APK）
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // 二维码生成：只用 zxing core 的纯 Java 编码器（约 500KB，无 Activity 依赖）
    implementation("com.google.zxing:core:3.5.3")
}
