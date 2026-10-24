import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';

/// 纯 Dart 实现的 MD5 / SHA-1 / SHA-256。
///
/// 之所以不用 crypto 包：本项目坚持零第三方运行时依赖，
/// 而这三个算法实现加起来不到 200 行，且能被单元测试完全覆盖。

const int _mask = 0xFFFFFFFF;

/// MD5（RFC 1321）。
String md5Hex(String input) => _md5(utf8.encode(input));

/// SHA-1（RFC 3174）。
String sha1Hex(String input) => _sha1(utf8.encode(input));

/// SHA-256（FIPS 180-4）。
String sha256Hex(String input) => _sha256(utf8.encode(input));

// ---------------------------------------------------------------------------
// MD5
// ---------------------------------------------------------------------------

const List<int> _md5Shift = [
  7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
  5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
  4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
  6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
];

List<int>? _md5Table;

List<int> _md5K() {
  final cached = _md5Table;
  if (cached != null) return cached;
  final k = List<int>.generate(64, (i) => ((math.sin(i + 1).abs()) * 4294967296).floor() & _mask);
  _md5Table = k;
  return k;
}

int _rotl(int x, int n) => ((x << n) | (x >> (32 - n))) & _mask;

String _md5(List<int> data) {
  final k = _md5K();
  final bitLen = data.length * 8;
  final padded = <int>[...data, 0x80];
  while (padded.length % 64 != 56) {
    padded.add(0);
  }
  // 64 位小端长度
  for (var i = 0; i < 8; i++) {
    padded.add((bitLen >> (8 * i)) & 0xFF);
  }

  var a0 = 0x67452301, b0 = 0xEFCDAB89, c0 = 0x98BADCFE, d0 = 0x10325476;

  final m = List<int>.filled(16, 0);
  for (var off = 0; off < padded.length; off += 64) {
    for (var i = 0; i < 16; i++) {
      final j = off + i * 4;
      m[i] = padded[j] | (padded[j + 1] << 8) | (padded[j + 2] << 16) | (padded[j + 3] << 24);
    }
    var a = a0, b = b0, c = c0, d = d0;
    for (var i = 0; i < 64; i++) {
      int f, g;
      if (i < 16) {
        f = (b & c) | (~b & d);
        g = i;
      } else if (i < 32) {
        f = (d & b) | (~d & c);
        g = (5 * i + 1) % 16;
      } else if (i < 48) {
        f = b ^ c ^ d;
        g = (3 * i + 5) % 16;
      } else {
        f = c ^ (b | ~d);
        g = (7 * i) % 16;
      }
      f = (f & _mask) + (a & _mask) + k[i] + m[g];
      final tmp = d;
      d = c;
      c = b;
      b = (b + _rotl(f & _mask, _md5Shift[i])) & _mask;
      a = tmp;
    }
    a0 = (a0 + a) & _mask;
    b0 = (b0 + b) & _mask;
    c0 = (c0 + c) & _mask;
    d0 = (d0 + d) & _mask;
  }

  final out = <int>[];
  for (final v in [a0, b0, c0, d0]) {
    out.addAll([v & 0xFF, (v >> 8) & 0xFF, (v >> 16) & 0xFF, (v >> 24) & 0xFF]);
  }
  return out.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
}

// ---------------------------------------------------------------------------
// SHA-1
// ---------------------------------------------------------------------------

String _sha1(List<int> data) {
  final bitLen = data.length * 8;
  final padded = <int>[...data, 0x80];
  while (padded.length % 64 != 56) {
    padded.add(0);
  }
  final len = ByteData(8)..setUint64(0, bitLen, Endian.big);
  for (var i = 0; i < 8; i++) {
    padded.add(len.getUint8(i));
  }

  var h0 = 0x67452301, h1 = 0xEFCDAB89, h2 = 0x98BADCFE, h3 = 0x10325476, h4 = 0xC3D2E1F0;
  final w = List<int>.filled(80, 0);

  for (var off = 0; off < padded.length; off += 64) {
    for (var i = 0; i < 16; i++) {
      final j = off + i * 4;
      w[i] = (padded[j] << 24) | (padded[j + 1] << 16) | (padded[j + 2] << 8) | padded[j + 3];
    }
    for (var i = 16; i < 80; i++) {
      w[i] = _rotl(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
    }

    var a = h0, b = h1, c = h2, d = h3, e = h4;
    for (var i = 0; i < 80; i++) {
      int f, k;
      if (i < 20) {
        f = (b & c) | (~b & d);
        k = 0x5A827999;
      } else if (i < 40) {
        f = b ^ c ^ d;
        k = 0x6ED9EBA1;
      } else if (i < 60) {
        f = (b & c) | (b & d) | (c & d);
        k = 0x8F1BBCDC;
      } else {
        f = b ^ c ^ d;
        k = 0xCA62C1D6;
      }
      final temp = (_rotl(a, 5) + (f & _mask) + e + k + w[i]) & _mask;
      e = d;
      d = c;
      c = _rotl(b, 30);
      b = a;
      a = temp;
    }
    h0 = (h0 + a) & _mask;
    h1 = (h1 + b) & _mask;
    h2 = (h2 + c) & _mask;
    h3 = (h3 + d) & _mask;
    h4 = (h4 + e) & _mask;
  }

  final buf = StringBuffer();
  for (final h in [h0, h1, h2, h3, h4]) {
    buf.write(h.toRadixString(16).padLeft(8, '0'));
  }
  return buf.toString();
}

// ---------------------------------------------------------------------------
// SHA-256
// ---------------------------------------------------------------------------

const List<int> _k256 = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
];

int _rotr(int x, int n) => ((x >> n) | (x << (32 - n))) & _mask;

String _sha256(List<int> data) {
  final bitLen = data.length * 8;
  final padded = <int>[...data, 0x80];
  while (padded.length % 64 != 56) {
    padded.add(0);
  }
  final len = ByteData(8)..setUint64(0, bitLen, Endian.big);
  for (var i = 0; i < 8; i++) {
    padded.add(len.getUint8(i));
  }

  final h = <int>[
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ];
  final w = List<int>.filled(64, 0);

  for (var off = 0; off < padded.length; off += 64) {
    for (var i = 0; i < 16; i++) {
      final j = off + i * 4;
      w[i] = (padded[j] << 24) | (padded[j + 1] << 16) | (padded[j + 2] << 8) | padded[j + 3];
    }
    for (var i = 16; i < 64; i++) {
      final s0 = _rotr(w[i - 15], 7) ^ _rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
      final s1 = _rotr(w[i - 2], 17) ^ _rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
      w[i] = (w[i - 16] + s0 + w[i - 7] + s1) & _mask;
    }

    var a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
    for (var i = 0; i < 64; i++) {
      final s1 = _rotr(e, 6) ^ _rotr(e, 11) ^ _rotr(e, 25);
      final ch = (e & f) ^ (~e & g);
      final t1 = (hh + s1 + (ch & _mask) + _k256[i] + w[i]) & _mask;
      final s0 = _rotr(a, 2) ^ _rotr(a, 13) ^ _rotr(a, 22);
      final maj = (a & b) ^ (a & c) ^ (b & c);
      final t2 = (s0 + (maj & _mask)) & _mask;
      hh = g;
      g = f;
      f = e;
      e = (d + t1) & _mask;
      d = c;
      c = b;
      b = a;
      a = (t1 + t2) & _mask;
    }
    h[0] = (h[0] + a) & _mask;
    h[1] = (h[1] + b) & _mask;
    h[2] = (h[2] + c) & _mask;
    h[3] = (h[3] + d) & _mask;
    h[4] = (h[4] + e) & _mask;
    h[5] = (h[5] + f) & _mask;
    h[6] = (h[6] + g) & _mask;
    h[7] = (h[7] + hh) & _mask;
  }

  final buf = StringBuffer();
  for (final v in h) {
    buf.write(v.toRadixString(16).padLeft(8, '0'));
  }
  return buf.toString();
}
