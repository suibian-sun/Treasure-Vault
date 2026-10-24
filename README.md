# 宝藏盒 · Treasure Vault

> 一个把 57 个常用工具装进兜里的 Android 应用。**Flutter 编写、零第三方运行时依赖、全部离线运行**。

宝藏盒是一个「工具集合」型应用：文本清洗、编码转换、哈希计算、单位换算、日期计算、开发辅助、图片处理、生活小工具，打开即用，不联网也能跑。

界面沿用 [SunBuilder-grpc](https://github.com/suibian-sun/SunBuilder-grpc) 的 **Minimal 极简**设计语言：纯白卡片、浅灰画布、1px 发丝描边、亮蓝 `#1890FF` 唯一强调色。

---

## 1. 三个页面

| 页面 | 内容 |
| --- | --- |
| **首页** | 欢迎卡片、**我的收藏**（四宫格，点右上角星标固定）、**最近使用**（自动记录最近 10 个工具，可一键清空） |
| **全部** | 全量工具按 **8 大分类**分组展示；顶部搜索框支持按名称 / 用途 / 关键词检索；分类 chip 可快速筛选 |
| **我的** | 检查更新、自动检查开关、下载加速源、更新记录、主题（浅色 / 深色 / 跟随系统）、低内存模式、清除使用记录、关于 |

底部导航：**首页 · 全部 · 我的**（图标 + 文字，选中为品牌蓝）。

---

## 2. 内置工具（57 个）

| 分类 | 工具 |
| --- | --- |
| **文本处理**（12） | 字数统计、大小写转换（9 种命名风格）、文本去重、文本排序、查找替换（支持正则）、去除空行、去除空白、添加行号、文本反转、分隔符转换、文本对比（逐行 diff）、内容提取（邮箱/手机号/网址/数字/IP/中文） |
| **编码转换**（5） | Base64 编解码（含 URL 安全变体）、URL 编解码、Unicode 转义/还原、HTML 转义、JSON 格式化/压缩/校验 |
| **加密安全**（5） | MD5、SHA-1、SHA-256、密码生成器、UUID v4 批量生成 |
| **计算换算**（16） | 科学计算器（括号/乘方/阶乘/函数）、进制转换（2/8/10/16/36）、长度 / 重量 / 温度 / 面积 / 体积 / 存储容量换算、百分比计算、折扣计算、BMI、贷款计算（等额本息/本金）、AA 分摊、罗马数字、人民币大写、随机数生成 |
| **日期时间**（5） | 时间戳转换（秒/毫秒自动识别）、日期相差（含工作日）、年龄计算（生肖/星座）、倒数日、日期查询（星期/周数/闰年） |
| **开发工具**（7） | 正则测试（分组捕获）、颜色转换（HEX/RGB/HSL）、JWT 解析、HTTP 状态码、JSON 转 Dart 模型、URL 解析、设备信息 |
| **图片工具**（3） | 图片信息、图片压缩（原生采样 + JPEG）、图片转 Base64 |
| **生活娱乐**（4） | 掷骰子（D4~D100）、随机抽签、抛硬币、秒表 |

> 每个工具都真实可用，没有占位页。所有计算在设备本地完成，不上传任何数据。

---

## 3. 「极低内存」是怎么做到的

这不是一句口号，而是若干具体取舍：

| 手段 | 说明 |
| --- | --- |
| **零第三方运行时依赖** | `pubspec.yaml` 只有 `flutter` 一个依赖。HTTP 用 `dart:io`，哈希自己实现（MD5/SHA-1/SHA-256 共约 200 行，已用标准向量验证），设置持久化用原生目录 + JSON 文件，文件选择与 APK 安装用 200 行 Kotlin 通道。没有插件注册表，没有多余 `.so`。 |
| **不打包字体与图片** | 中文直接用系统字体；参考项目里 8MB 的 Noto Sans SC 与图标字体全部省掉。图标使用 Material Icons 的 tree-shaking 子集。 |
| **页面懒构建 + 不常驻** | 57 个工具只注册「元数据 + 构建闭包」，点开才实例化；底部三个 Tab **只构建当前页**（不用 IndexedStack 常驻三棵子树），切页即释放。 |
| **图片内存受控** | 全局 `imageCache` 压到 16 张 / 6MB；图片解码、压缩全在 Kotlin 侧用 `inSampleSize` 采样，Dart 堆不加载整图。 |
| **低内存模式** | 设置中默认开启：关闭页面过渡动画与 Tab 切换动画，减少一帧离屏缓冲与动画层。 |
| **R8 + 资源压缩 + 分包** | `minifyEnabled` + `shrinkResources`，并 `--split-per-abi` 按 ABI 分包；单设备只安装自己那一份。 |
| **无状态管理框架** | 一份 `ChangeNotifier` + `InheritedNotifier` 即可，省掉 Provider/Riverpod 的容器与监听对象。 |

结果：安装包按 ABI 约 15–19 MB（Flutter 引擎占大头），冷启动只加载首页所需的对象树；R8 + 资源压缩后 dex 与资源体积已压到最低。

---

## 4. 应用内更新（可关闭弹窗 + 进度 + 自动安装）

```
启动 / 点击「检查更新」
        │
        ├── GET api.github.com/repos/suibian-sun/Treasure-Vault-release/releases/latest
        │      （可走加速源，失败自动回退直连）
        │
        ├── 有新版本 → 弹出更新弹窗
        │       ├─ 更新内容（可滚动）
        │       ├─ [关闭]  —— 随时可退出，不打断使用
        │       └─ [更新]  —— 弹窗内显示进度条 / 已下载 / 速度
        │                 └─ 下载完成 → 自动调用系统安装器（FileProvider + ACTION_VIEW）
        │
        └── 已是最新 → 轻提示
```

细节：

* 下载使用 `dart:io` 流式写入磁盘，**不把 APK 读进内存**；支持 HTTP Range 断点续传。
* 只接受 https 直链；下载后校验 ZIP 魔数（`PK\x03\x04`）与文件长度，防止半包或被替换。
* 关闭弹窗会中断下载并清理状态，不留下「假完成」的半包。
* 安装失败（未授予「安装未知应用」）时给出提示与「重试安装」。

**更新记录页**（我的 → 更新记录）独立成页：优先读取 GitHub Release 的版本说明，离线时回退到内置的 `lib/models/release_notes.dart`。

---

## 5. 构建与发布（GitHub Actions）

工作流：`.github/workflows/build.yml`，push 到任意分支即构建，`main` 分支额外发布。

1. 校验 **三处版本号一致**：`pubspec.yaml` / `lib/app_version.dart` / `release_notes.dart` 当前版本条目；
2. 校验 **更新日志非空**（每个版本必须能对用户解释清楚）；
3. 校验 **签名证书指纹**（构建前，比对 `android/signing/fingerprint.txt`）；
4. `flutter analyze` + `flutter build apk --release --split-per-abi`（含混淆与符号分离）；
5. 用 `apksigner` 校验每个 APK 的 **实际签名指纹**（构建后二次校验）；
6. 产物命名 `TreasureVault-<abi>.apk`，上传 artifact；
7. 发布到本仓库的 Releases（tag `v1.0.0`，说明取自 `release_notes.dart`），应用内「检查更新」读取该 Release。

### 签名一致性

签名密钥不进入仓库，由 4 个 Repository Secrets（`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）在 CI 中还原；仓库只保留证书指纹。

* 每次构建**前**校验 keystore 指纹、**后**校验 APK 实际签名，二者必须等于 `android/signing/fingerprint.txt`；
* 缺少密钥时构建直接失败，绝不回退 debug 签名；
* 因此每次产出的 APK 签名完全相同，用户可直接覆盖安装。

详见 [SIGNING.md](SIGNING.md)。

---

## 6. 目录结构

```
lib/
├── main.dart                 入口（图片缓存上限、状态注入）
├── app_version.dart          版本号（与 pubspec 强绑定）
├── theme/app_theme.dart      Minimal 设计令牌与 ThemeData
├── core/
│   ├── tool.dart             ToolDef / 分类定义
│   ├── app_state.dart        全局状态 + 收藏/最近 + JSON 持久化
│   ├── hash.dart             MD5 / SHA-1 / SHA-256
│   ├── native_bridge.dart    原生通道封装
│   └── utils.dart            格式化、复制、解析
├── services/
│   ├── update_service.dart   版本检查（dart:io，含加速源）
│   └── update_downloader.dart 断点续传下载 + 校验 + 安装
├── pages/                    首页 / 全部 / 我的 / 工具宿主 / 更新记录
├── widgets/                  通用组件、顶栏、更新弹窗
└── tools/                    57 个工具（按分类拆分）
```

---

## 7. 本地运行

```bash
flutter pub get
flutter run                     # 调试
flutter build apk --release --split-per-abi   # 发布包
```

要求：Flutter 3.47+（Dart 3.13+）、JDK 17、Android SDK 36。

> **构建注意**：`android/app/build.gradle.kts` 显式应用了 `org.jetbrains.kotlin.android`。
> 模板默认的 `android.builtInKotlin=false` 会关闭 AGP 内置 Kotlin 支持，
> 此时若不显式应用 Kotlin 插件，`src/main/kotlin` 下的 `MainActivity` 不会参与编译，
> 产物会缺少入口 Activity 并在启动时闪退。CI 已加入产物自检（dex 中必须存在
> `com.suibian.treasure_vault.MainActivity`），此类问题不会再流出。

---

## 8. 设计参考

* 设计语言与页面骨架参考 [SunBuilder-grpc](https://github.com/suibian-sun/SunBuilder-grpc)（Minimal：纯白卡片 + 浅灰画布 + 发丝描边 + 亮蓝强调）。
* 更新机制（检查 → 弹窗 → 进度 → 系统安装器）同样沿用该项目已验证的链路，并按「零依赖」重写。
