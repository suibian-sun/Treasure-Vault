package com.suibian.treasure_vault.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.chips
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.et
import com.suibian.treasure_vault.core.label
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** 影音与创意工具。 */
object MediaTools {

    private fun spec(id: String, name: String, desc: String, cat: com.suibian.treasure_vault.core.Category,
                     emoji: String, vararg kw: String, build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, cat, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("white_noise", "白噪音", "白 / 粉 / 棕噪音与雨声助眠", Cats.media, "🎧", "白噪音", "助眠") { a, b -> whiteNoise(a, b) },
        spec("tts", "文字转语音", "用系统语音朗读文本", Cats.media, "🗣️", "语音", "朗读") { a, b -> tts(a, b) },
        spec("video_audio", "视频提取音频", "把视频里的声音单独导出", Cats.media, "🎵", "音频", "提取") { a, b -> videoAudio(a, b) },
        spec("hydrogen_wallpaper", "氢壁纸", "生成氢 OS 风格渐变壁纸", Cats.media, "🎨", "壁纸", "渐变") { a, b -> hydrogenWallpaper(a, b) },
        spec("native_wallpaper", "原生氢壁纸", "导出当前系统壁纸", Cats.media, "🖼️", "壁纸", "导出") { a, b -> nativeWallpaper(a, b) },
        spec("research", "大水研究所", "一本正经地生成研究报告", Cats.life, "🔬", "研究所", "娱乐") { a, b -> research(a, b) },
        spec("function_plot", "函数可视化", "输入表达式画出函数曲线", Cats.calc, "📈", "函数", "图像") { a, b -> functionPlot(a, b) },
    )

    // ---------------- 白噪音 ----------------

    private fun whiteNoise(act: ToolActivity, body: LinearLayout) {
        var track: AudioTrack? = null
        var playing = false
        var type = 0
        var volume = 0.6f
        val names = listOf("白噪音", "粉噪音", "棕噪音", "雨声")

        val status = act.tv("未播放", 12f, T.ink2)
        val toggle = act.tv("开始播放", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = T.fillBg(T.primary, act.dp(T.R_MEDIUM).toFloat())
            setPadding(0, act.dp(13), 0, act.dp(13))
            isClickable = true
        }

        body.addView(act.card {
            sectionTitle("音色")
            chips(names, 0) { i ->
                type = i
                if (playing) { stopPlayer(track); track = buildTrack(type, volume); track?.play(); status.text = "正在播放：${names[type]}" }
            }
            label("音量")
            val bar = android.widget.SeekBar(act).apply {
                max = 100; progress = 60
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        volume = p / 100f
                        track?.setVolume(volume)
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            addView(bar)
            addView(status, LinearLayout.LayoutParams(-2, -2).also { it.topMargin = act.dp(6) })
            addView(toggle, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(10) })
            addView(act.tv("播放时建议戴耳机或调低音量；本工具不申请网络权限。", 11.5f, T.ink2).apply {
                setPadding(0, act.dp(8), 0, 0)
            })
        })

        toggle.setOnClickListener {
            if (playing) {
                stopPlayer(track)
                track = null
                playing = false
                toggle.text = "开始播放"
                status.text = "已停止"
            } else {
                val t = buildTrack(type, volume)
                if (t == null) {
                    Ui.toast(act, "无法初始化音频输出")
                    return@setOnClickListener
                }
                track = t
                t.play()
                playing = true
                toggle.text = "停止"
                status.text = "正在播放：${names[type]}"
            }
        }

        act.onCleanup {
            playing = false
            stopPlayer(track)
            track = null
        }
    }

    private fun stopPlayer(track: AudioTrack?) {
        runCatching {
            track?.stop()
            track?.release()
        }
    }

    private fun buildTrack(type: Int, volume: Float): AudioTrack? = runCatching {
        val rate = 44100
        val seconds = 3
        val samples = rate * seconds
        val buf = ShortArray(samples)
        val rnd = Random(System.nanoTime())
        when (type) {
            0 -> {  // 白噪音
                for (i in 0 until samples) buf[i] = ((rnd.nextFloat() * 2 - 1) * 7000).toInt().toShort()
            }
            1 -> {  // 粉噪音（简单一阶滤波）
                var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
                for (i in 0 until samples) {
                    val white = rnd.nextFloat() * 2 - 1
                    b0 = 0.99765 * b0 + white * 0.0990460
                    b1 = 0.96300 * b1 + white * 0.2965164
                    b2 = 0.57000 * b2 + white * 1.0526913
                    val v = (b0 + b1 + b2 + white * 0.1848) * 1400
                    buf[i] = v.coerceIn(-32000.0, 32000.0).toInt().toShort()
                }
            }
            2 -> {  // 棕噪音（积分）
                var last = 0.0
                for (i in 0 until samples) {
                    val white = rnd.nextFloat() * 2 - 1
                    last = (last + 0.02 * white) / 1.02
                    buf[i] = (last * 60000).coerceIn(-32000.0, 32000.0).toInt().toShort()
                }
            }
            else -> {  // 雨声：棕噪音 + 随机雨滴
                var last = 0.0
                for (i in 0 until samples) {
                    val white = rnd.nextFloat() * 2 - 1
                    last = (last + 0.03 * white) / 1.03
                    var v = last * 45000
                    if (rnd.nextInt(2200) == 0) v += (rnd.nextFloat() * 0.8 + 0.2) * 18000
                    buf[i] = v.coerceIn(-32000.0, 32000.0).toInt().toShort()
                }
            }
        }
        // 首尾淡入淡出，避免循环处爆音
        val fade = 2000
        for (i in 0 until fade) {
            val g = i / fade.toFloat()
            buf[i] = (buf[i] * g).toInt().toShort()
            buf[samples - 1 - i] = (buf[samples - 1 - i] * g).toInt().toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(buf, 0, buf.size)
        t.setLoopPoints(0, buf.size, -1)
        t.setVolume(volume)
        t
    }.getOrNull()

    // ---------------- 文字转语音 ----------------

    private fun tts(act: ToolActivity, body: LinearLayout) {
        var rate = 1.0f
        var ready = false
        val holder = arrayOfNulls<TextToSpeech>(1)
        var statusView: android.widget.TextView? = null

        body.addView(act.card {
            sectionTitle("朗读")
            val e = et("输入要朗读的文字", 6)
            label("语速")
            chips(listOf("0.7×", "1.0×", "1.3×", "1.6×"), 1) { i ->
                rate = listOf(0.7f, 1.0f, 1.3f, 1.6f)[i]
                holder[0]?.setSpeechRate(rate)
            }
            val status = act.tv("正在初始化语音引擎…", 11.5f, T.ink2)
            statusView = status
            addView(status)
            btn("朗读") {
                if (!ready) { Ui.toast(act, "语音引擎还没准备好"); return@btn }
                val text = e.text?.toString().orEmpty()
                if (text.isBlank()) { Ui.toast(act, "先输入文字"); return@btn }
                holder[0]?.setSpeechRate(rate)
                holder[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tv")
                Ui.toast(act, "开始朗读")
            }
            btn("停止", primary = false) { holder[0]?.stop() }
        })
        body.note("使用系统自带的 TTS 引擎，中文需要系统里已安装中文语音包（多数手机自带）。")

        val engine = TextToSpeech(act) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = holder[0]?.setLanguage(Locale.CHINA)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                holder[0]?.setSpeechRate(rate)
                act.runOnUiThread {
                    statusView?.text = if (ready) "语音引擎就绪" else "系统缺少中文语音包"
                }
            } else {
                act.runOnUiThread { statusView?.text = "语音引擎初始化失败" }
            }
        }
        holder[0] = engine
        act.onCleanup {
            holder[0]?.stop()
            holder[0]?.shutdown()
            holder[0] = null
        }
    }

    // ---------------- 视频提取音频 ----------------

    private fun videoAudio(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("选择视频")
            btn("选择视频文件") {
                act.pickFile { uri ->
                    if (uri == null) return@pickFile
                    holder.removeAllViews()
                    holder.addView(act.card { addView(act.tv("正在提取音轨…", 13f, T.ink2)) })
                    Thread {
                        val result = runCatching {
                            val src = ImageKit.copyToCache(act, uri, ".mp4") ?: error("无法读取文件")
                            val outDir = File(act.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "extracted").apply { mkdirs() }
                            val outFile = File(outDir, "audio_${System.currentTimeMillis()}.m4a")
                            extractAudio(src, outFile)
                            outFile
                        }
                        act.runOnUiThread {
                            holder.removeAllViews()
                            result.onSuccess { f ->
                                holder.addView(act.card {
                                    sectionTitle("提取完成")
                                    addView(act.tv(f.name, 14f, T.ink, true))
                                    addView(act.tv("大小 ${f.length() / 1024} KB", 12f, T.ink2).apply { setPadding(0, act.dp(4), 0, act.dp(10)) })
                                    addView(act.tv(f.absolutePath, 11f, T.ink3).apply { setTextIsSelectable(true) })
                                    btn("播放试听") {
                                        runCatching {
                                            act.startActivity(
                                                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(android.net.Uri.fromFile(f), "audio/mp4")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                },
                                            )
                                        }.onFailure { Ui.toast(act, "没有可用的播放器") }
                                    }
                                    btn("分享", primary = false) {
                                        ImageKit.share(act, androidx.core.content.FileProvider.getUriForFile(act, "${act.packageName}.fileprovider", f))
                                    }
                                })
                            }.onFailure {
                                holder.addView(act.card {
                                    addView(act.tv("提取失败：${it.message}\n\n请确认所选文件包含音轨。", 12.5f, T.danger))
                                })
                            }
                        }
                    }.start()
                }
            }
        })
        body.addView(holder)
        body.note("使用系统 MediaExtractor 直接拷贝音轨，不重新编码，因此速度很快、音质无损。")
    }

    private fun extractAudio(src: File, out: File) {
        val extractor = MediaExtractor()
        extractor.setDataSource(src.absolutePath)
        var audioTrack = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                audioTrack = i
                format = f
                break
            }
        }
        if (audioTrack < 0 || format == null) {
            extractor.release()
            error("这个文件里没有音轨")
        }
        extractor.selectTrack(audioTrack)
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackIndex = muxer.addTrack(format)
        muxer.start()
        val maxSize = runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(256 * 1024)
        val buffer = java.nio.ByteBuffer.allocateDirect(maxSize.coerceAtLeast(64 * 1024))
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime
            info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            muxer.writeSampleData(trackIndex, buffer, info)
            extractor.advance()
        }
        muxer.stop()
        muxer.release()
        extractor.release()
    }

    // ---------------- 壁纸 ----------------

    private fun hydrogenWallpaper(act: ToolActivity, body: LinearLayout) {
        var style = 0
        var current: Bitmap? = null
        val styles = listOf("氢蓝", "晨光", "暮色", "青碧")
        val pairs = listOf(
            0xFF0B3D91.toInt() to 0xFF1890FF.toInt(),
            0xFFFFB199.toInt() to 0xFFFFE3B3.toInt(),
            0xFF232526.toInt() to 0xFF6A3D9A.toInt(),
            0xFF0F5B5B.toInt() to 0xFF4FD1C5.toInt(),
        )
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        var render: () -> Unit = {}

        body.addView(act.card {
            sectionTitle("风格")
            chips(styles, 0) { i -> style = i; render() }
            btn("保存到相册") {
                val bmp = current
                if (bmp == null) Ui.toast(act, "先生成壁纸") else
                    Ui.toast(act, if (ImageKit.save(act, bmp, "wallpaper") != null) "已保存到相册" else "保存失败")
            }
            btn("设为壁纸", primary = false) {
                val bmp = current
                if (bmp == null) { Ui.toast(act, "先生成壁纸"); return@btn }
                runCatching {
                    val wm = act.getSystemService(android.app.WallpaperManager::class.java)
                    wm.setBitmap(bmp)
                    Ui.toast(act, "壁纸已设置")
                }.onFailure { Ui.toast(act, "设置失败：${it.message}") }
            }
        })
        body.addView(holder)

        render = {
            val w = 1080
            val h = 1920
            val bmp = ImageKit.newBitmap(w, h)
            val c = Canvas(bmp)
            val (c1, c2) = pairs[style]
            val paint = Paint().apply {
                shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), c1, c2, Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            val glow = Paint(Paint.ANTI_ALIAS_FLAG)
            glow.shader = android.graphics.RadialGradient(
                w * 0.72f, h * 0.28f, w * 0.55f,
                intArrayOf(T.withAlpha(Color.WHITE, 90), 0x00FFFFFF),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)
            val glow2 = Paint(Paint.ANTI_ALIAS_FLAG)
            glow2.shader = android.graphics.RadialGradient(
                w * 0.2f, h * 0.78f, w * 0.5f,
                intArrayOf(T.withAlpha(pairs[style].first, 120), 0x00000000),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow2)
            current = bmp
            holder.removeAllViews()
            holder.addView(act.card {
                sectionTitle("预览（1080 × 1920）")
                addView(ImageKit.preview(act, bmp, 320))
            })
        }
        render()
    }

    private fun nativeWallpaper(act: ToolActivity, body: LinearLayout) {
        val holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        body.addView(act.card {
            sectionTitle("系统壁纸")
            btn("读取当前壁纸") {
                holder.removeAllViews()
                val bmp = runCatching {
                    val wm = act.getSystemService(android.app.WallpaperManager::class.java)
                    @Suppress("DEPRECATION")
                    val d = wm.drawable ?: error("系统没有返回壁纸")
                    val b = ImageKit.newBitmap(d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1))
                    val c = Canvas(b)
                    d.setBounds(0, 0, b.width, b.height)
                    d.draw(c)
                    b
                }.getOrNull()
                holder.addView(act.card {
                    if (bmp == null) {
                        addView(act.tv("读取失败，部分系统限制了壁纸读取。", 13f, T.ink2))
                    } else {
                        sectionTitle("当前壁纸")
                        addView(ImageKit.preview(act, bmp, 300))
                        addView(act.tv("${bmp.width} × ${bmp.height}", 11f, T.ink3))
                        btn("保存到相册") { Ui.toast(act, if (ImageKit.save(act, bmp, "system_wallpaper") != null) "已保存" else "保存失败") }
                        btn("分享", primary = false) { ImageKit.shareable(act, bmp)?.let { ImageKit.share(act, it) } }
                    }
                })
            }
        })
        body.addView(holder)
        body.note("「原生氢壁纸」指系统里正在使用的原生壁纸，本工具把它导出保存，便于备份或做配色参考。")
    }

    // ---------------- 大水研究所 ----------------

    private fun research(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card {
            sectionTitle("研究主题")
            val e = et("输入一个词，比如：周一、加班、奶茶", singleLine = true)
            val outView = act.tv("—", 13.5f, T.ink3).apply {
                background = T.fieldBg()
                setPadding(act.dp(14), act.dp(14), act.dp(14), act.dp(14))
                setTextIsSelectable(true)
                setLineSpacing(act.dp(4).toFloat(), 1f)
            }
            btn("生成研究报告") {
                val t = e.text?.toString()?.trim().orEmpty()
                if (t.isEmpty()) { Ui.toast(act, "先输入一个主题"); return@btn }
                outView.text = Nonsense.report(t)
                outView.setTextColor(T.ink)
            }
            btn("换个版本", primary = false) {
                val t = e.text?.toString()?.trim().orEmpty()
                if (t.isNotEmpty()) outView.text = Nonsense.report(t)
            }
            addView(outView)
            note("纯本地模板随机拼装，不联网、不代表任何真实研究结论。")
        })
    }

    // ---------------- 函数可视化 ----------------

    private fun functionPlot(act: ToolActivity, body: LinearLayout) {
        body.addView(act.card(pad = 12) {
            sectionTitle("函数图像")
            val e = et("输入表达式，如：sin(x)/x　或　x^2-3*x+1", singleLine = true)
            e.setText("sin(x)/x")
            val plot = PlotView(act)
            plot.expression = "sin(x)/x"
            e.doAfterTextChanged { s -> plot.expression = s?.toString().orEmpty() }
            addView(plot, LinearLayout.LayoutParams(-1, act.dp(320)))
            val range = act.tv("x ∈ [-10, 10]　拖动下方滑块缩放", 11.5f, T.ink2)
            addView(range)
            val bar = android.widget.SeekBar(act).apply {
                max = 30; progress = 10
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        val r = (p + 2).toFloat()
                        plot.range = r
                        range.text = "x ∈ [-${"%.0f".format(r)}, ${"%.0f".format(r)}]　纵向自动缩放"
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            addView(bar)
            note("支持 + - * / ^ 与 sin cos tan ln log sqrt abs exp 等函数，变量用 x。")
        })
    }

    private class PlotView(context: android.content.Context) : View(context) {
        var expression: String = ""
            set(value) { field = value; cache.clear(); invalidate() }
        var range = 10f
            set(value) { field = value; cache.clear(); invalidate() }
        private val cache = HashMap<Int, Float?>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val curve = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }

        private fun f(x: Double): Double? = runCatching {
            val expr = expression.replace(Regex("(?<![a-zA-Z])x(?![a-zA-Z])"), "($x)")
            Evaluator.eval(expr)
        }.getOrNull()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(T.surface)
            // 采样
            val pts = FloatArray(width + 1)
            var minY = Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            for (px in 0..width) {
                val x = -range + 2 * range * px / width
                val y = f(x.toDouble())
                if (y != null && !y.isNaN() && !y.isInfinite() && abs(y) < 1e6) {
                    pts[px] = y.toFloat()
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                } else {
                    pts[px] = Float.NaN
                }
            }
            if (minY > maxY) {
                paint.color = T.ink3
                paint.textSize = 40f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("表达式无法计算", width / 2f, height / 2f, paint)
                return
            }
            // 让 0 包含在可视范围
            var lo = minOf(minY, 0.0)
            var hi = maxOf(maxY, 0.0)
            if (hi - lo < 1e-6) { hi += 1; lo -= 1 }
            val pad = (hi - lo) * 0.12
            lo -= pad; hi += pad
            fun sy(v: Double) = (height - (v - lo) / (hi - lo) * height).toFloat()
            // 网格
            paint.strokeWidth = 1.5f
            paint.color = T.line
            for (i in 1..3) {
                val y = height * i / 4f
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
            for (i in 1..3) {
                val x = width * i / 4f
                canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            }
            // 坐标轴
            paint.strokeWidth = 2.5f
            paint.color = T.ink3
            val zeroY = sy(0.0)
            if (zeroY in 0f..height.toFloat()) canvas.drawLine(0f, zeroY, width.toFloat(), zeroY, paint)
            canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), paint)
            // 曲线
            curve.color = T.primary
            val path = Path()
            var started = false
            for (px in 0..width) {
                val v = pts[px]
                if (v.isNaN()) { started = false; continue }
                val y = sy(v.toDouble())
                if (!started) { path.moveTo(px.toFloat(), y); started = true } else path.lineTo(px.toFloat(), y)
            }
            canvas.drawPath(path, curve)
            // 标注
            paint.color = T.ink3
            paint.textSize = 26f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("y max %.2f".format(maxY), 10f, 34f, paint)
            canvas.drawText("y min %.2f".format(minY), 10f, 64f, paint)
        }
    }
}

/** 无厘头研究报告生成器。 */
object Nonsense {

    private val fields = listOf(
        "行为经济学", "社会心理学", "组织行为学", "认知科学", "传播学", "人类学",
        "睡眠医学", "城市社会学", "数据科学", "文化研究",
    )
    private val methods = listOf(
        "对 1,024 份问卷做了三因素方差分析",
        "跟踪记录了 37 名志愿者连续 21 天的行为日志",
        "对 8.6 万条公开文本做了主题模型聚类",
        "采用双盲对照，控制了时间段与室温两个变量",
        "用眼动仪记录了受试者的注意力热区",
        "在三个城市的 12 个采样点做了为期两个月的观察",
    )
    private val findings = listOf(
        "效应量虽小但稳定（Cohen's d = 0.31，p < 0.05）",
        "呈现出明显的 U 型曲线，拐点出现在第 3 天",
        "与晨间光照时长呈显著负相关（r = -0.42）",
        "在 25–34 岁人群中信号最强，随后迅速衰减",
        "存在显著的个体差异，约 18% 的样本表现出反向趋势",
        "重复测量显示结果可复现，但幅度减半",
    )
    private val conclusions = listOf(
        "建议将其纳入日常决策的参考因素，但不必过度解读。",
        "短期内不会改变任何人的生活，长期看也许有一点。",
        "本研究的主要贡献是提出了一个更好的问题。",
        "结论的边界条件仍需更大样本验证，谨慎外推。",
        "如果一定要给建议：保持规律，剩下的交给概率。",
        "该现象或许只是我们观察方式造成的错觉，这本身值得研究。",
    )

    fun report(topic: String): String {
        val f = fields.random()
        val m = methods.random()
        val f1 = findings.random()
        val f2 = findings.filter { it != f1 }.random()
        val c = conclusions.random()
        val n = (120..980).random()
        return buildString {
            append("《关于「$topic」的$f 研究》\n\n")
            append("摘要　本研究以「$topic」为对象，从$f 视角出发，$m。")
            append("样本量 N = $n，结果显示：$f1；同时$f2。")
            append("我们认为，这一发现对理解日常生活中的「$topic」具有启发意义。\n\n")
            append("一、研究背景\n")
            append("「$topic」长期存在于人们的日常经验中，却少有系统研究。")
            append("现有文献多停留在描述层面，缺乏可重复的量化证据，这构成了本研究的出发点。\n\n")
            append("二、方法\n")
            append("本研究$m。为排除干扰，所有参与者均在相同环境下完成任务，")
            append("并在结束后接受了 5 分钟的开放式访谈。\n\n")
            append("三、结果\n")
            append("• $f1\n• $f2\n• 访谈文本中，「累」「还行」「再来一次」出现频率最高。\n\n")
            append("四、讨论\n")
            append("结果的解释需要谨慎。一方面，$f 领域的既有理论可以部分解释该现象；")
            append("另一方面，样本的代表性有限，跨文化适用性存疑。\n\n")
            append("五、结论\n")
            append("$c\n\n")
            append("（本报告由本地模板随机生成，仅供娱乐，不构成任何研究结论。）")
        }
    }
}
