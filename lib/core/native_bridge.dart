import 'dart:io';

import 'package:flutter/services.dart';

/// 原生能力桥接（不依赖任何第三方插件）。
///
/// 仅承载四类必要能力：应用目录、设备信息、APK 安装、文件选择 / 图片压缩。
/// 这些能力用 Kotlin 直接实现，比引入插件更轻：无额外 .so、无插件注册开销。
class NativeBridge {
  NativeBridge._();

  static const MethodChannel _ch = MethodChannel('treasure_vault/native');

  /// 应用私有目录（files / cache）。
  static Future<Map<String, String>> appDirs() async {
    try {
      final r = await _ch.invokeMapMethod<String, String>('appDirs');
      if (r != null) return r;
    } catch (_) {}
    final tmp = Directory.systemTemp.path;
    return {'files': tmp, 'cache': tmp};
  }

  /// 设备信息（品牌 / 型号 / Android 版本 / ABI）。
  static Future<Map<String, String>> deviceInfo() async {
    try {
      final r = await _ch.invokeMapMethod<String, String>('deviceInfo');
      if (r != null) return r;
    } catch (_) {}
    return {};
  }

  /// 调用系统安装器安装 APK。返回是否成功拉起安装界面。
  static Future<bool> installApk(String path) async {
    try {
      final ok = await _ch.invokeMethod<bool>('installApk', {'path': path});
      return ok ?? false;
    } catch (_) {
      return false;
    }
  }

  /// 打开系统文件选择器，返回本地缓存副本路径（用户取消返回 null）。
  static Future<String?> pickFile({String mime = '*/*'}) async {
    try {
      return await _ch.invokeMethod<String>('pickFile', {'mime': mime});
    } catch (_) {
      return null;
    }
  }

  /// 读取图片元信息（不解码像素，内存占用可忽略）。
  static Future<Map<String, dynamic>?> imageInfo(String path) async {
    try {
      return await _ch.invokeMapMethod<String, dynamic>('imageInfo', {'path': path});
    } catch (_) {
      return null;
    }
  }

  /// 压缩图片（原生 BitmapFactory 按需采样，内存占用远低于 Dart 侧解码）。
  static Future<Map<String, dynamic>?> compressImage(
    String path, {
    required int maxDim,
    required int quality,
  }) async {
    try {
      return await _ch.invokeMapMethod<String, dynamic>('compressImage', {
        'path': path,
        'maxDim': maxDim,
        'quality': quality,
      });
    } catch (_) {
      return null;
    }
  }
}
