# 签名与构建一致性

应用内更新依赖一个硬性前提：**新 APK 与已安装 APK 的签名必须完全相同**，否则 Android 会拒绝覆盖安装（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`）。

## 1. 方案：一把密钥 + Secrets 注入 + 指纹校验

| 项目 | 值 |
| --- | --- |
| 密钥文件 | `treasure-vault.jks`（**不入库**，由 CI 从 Secrets 还原） |
| Alias | `treasure-vault` |
| Store / Key 密码 | `TreasureVault2026!` |
| 算法 | RSA 2048，SHA384withRSA，有效期至 2056-10-15 |
| 证书 SHA-256 | `7F:E4:2D:68:9C:43:53:9E:EA:4C:39:65:18:2D:27:48:1B:15:ED:59:B4:4D:80:14:B0:A3:92:66:7F:1C:CE:5C` |

仓库中**只保存证书指纹**（`signing/fingerprint.txt`）。私钥由以下 Secrets 提供：

| Secret | 说明 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore 的 Base64 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | `treasure-vault` |
| `KEY_PASSWORD` | key 密码 |

`build.yml` 第一步就把 keystore 还原到 `signing/treasure-vault.jks`，Gradle 从同一路径读取。这样：

* CI 与本地构建产出的 APK **物理上使用同一把钥匙**；
* 密钥不进入 git 历史，仓库可以安全公开；
* 缺少 secret 时构建**直接失败**（`build.gradle.kts` 在 `CI=true` 时抛异常），
  绝不会静默退化成 debug 签名——那种情况用户升级时才会发现，且无法补救。

## 2. CI 的三重校验

```bash
# ① 还原 keystore 前：确认 secret 存在
# ② 构建前：keystore 的证书指纹 == 固定指纹
keytool -list -v -keystore signing/treasure-vault.jks \
  -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" | grep SHA256:

# ③ 构建后：APK 的实际签名指纹 == 固定指纹
apksigner verify --print-certs TreasureVault-arm64-v8a.apk | grep 'SHA-256 digest'
```

任一环节不符，流水线立即失败，不会发布 Release。

## 3. 本地构建

```bash
# 从安全位置取回 keystore，放到约定路径
cp /path/to/treasure-vault.jks android/signing/

# 或临时用环境变量指定密码（默认值与 Secrets 相同）
KEYSTORE_PASSWORD='TreasureVault2026!' \
KEY_ALIAS='treasure-vault' \
KEY_PASSWORD='TreasureVault2026!' \
flutter build apk --release --split-per-abi
```

本地缺少 keystore 时会退回 debug 签名（仅用于调试，**不可发布**）。

## 4. 验证已安装版本的签名

```bash
adb shell dumpsys package com.suibian.treasure_vault | grep -A2 signatures
```

## 5. 如果密钥丢失或需要轮换

1. 生成新 keystore，更新 4 个 Secrets 与 `fingerprint.txt`；
2. 提升 `versionCode`；
3. 在更新说明中明确告知用户「本次需要卸载重装」；
4. 旧版本用户无法自动覆盖，这是密钥轮换的必然代价——所以**务必备份密钥**。
