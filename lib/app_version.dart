/// 应用版本号 —— 单一事实来源之一。
///
/// 约定（CI 会校验）：本文件的 `appVersion` 必须与 `pubspec.yaml` 的
/// `version:` 前半段完全一致，否则构建失败。
const String appVersion = '1.0.0';
const int appBuildNumber = 1;

/// 应用展示名与包名（关于页 / 更新日志使用）。
const String appName = '宝藏盒';
const String appNameEn = 'Treasure Vault';
const String appPackageId = 'com.suibian.treasure_vault';

/// 更新源仓库（公开只读，供应用内检查更新 / 提交记录使用）。
/// 源码仓库为私有，故发布产物统一投递到公开的 release 仓库。
const String releaseRepo = 'suibian-sun/Treasure-Vault-release';
const String sourceRepo = 'suibian-sun/Treasure-Vault';
