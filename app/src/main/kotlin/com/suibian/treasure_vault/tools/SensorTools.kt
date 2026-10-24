package com.suibian.treasure_vault.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.suibian.treasure_vault.core.Cats
import com.suibian.treasure_vault.core.T
import com.suibian.treasure_vault.core.ToolActivity
import com.suibian.treasure_vault.core.ToolSpec
import com.suibian.treasure_vault.core.Ui
import com.suibian.treasure_vault.core.btn
import com.suibian.treasure_vault.core.card
import com.suibian.treasure_vault.core.dp
import com.suibian.treasure_vault.core.note
import com.suibian.treasure_vault.core.sectionTitle
import com.suibian.treasure_vault.core.tv

/** 传感器类工具：尺子、水平仪、指南针、噪音计、金属探测。 */
object SensorTools {

    private fun spec(id: String, name: String, desc: String, emoji: String, vararg kw: String,
                     build: (ToolActivity, LinearLayout) -> Unit) =
        ToolSpec(id, name, desc, Cats.sensor, emoji, kw.toList(), build = build)

    val all: List<ToolSpec> = listOf(
        spec("ruler", "尺子", "屏幕直尺，可校准刻度", "📏", "尺子", "测量") { a, b -> ruler(a, b) },
        spec("level", "水平仪", "气泡水平仪，测倾斜角", "🫧", "水平", "坡度") { a, b -> level(a, b) },
        spec("compass", "指南针", "磁力计方位，带度数", "🧭", "指南针", "方向") { a, b -> compass(a, b) },
        spec("noise", "噪音测量", "麦克风测环境噪音分贝", "🔊", "噪音", "分贝") { a, b -> noise(a, b) },
        spec("metal", "金属探测仪", "靠磁场变化探测金属", "🧲", "金属", "磁场") { a, b -> metal(a, b) },
    )

    private fun sensorManager(act: ToolActivity): SensorManager? =
        act.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // ---------------- 尺子 ----------------

    private fun ruler(act: ToolActivity, body: LinearLayout) {
        var calibrate = 1.0f
        val rulerView = RulerView(act).apply { scale = calibrate }
        body.addView(act.card(pad = 10) {
            sectionTitle("屏幕直尺")
            addView(rulerView, LinearLayout.LayoutParams(-1, act.dp(300)))
            addView(act.tv("把银行卡（长边 85.6mm）贴在屏幕上，拖动下方滑块让刻度对齐。", 11.5f, T.ink2).apply {
                setPadding(0, act.dp(8), 0, act.dp(6))
            })
            val bar = android.widget.SeekBar(act).apply {
                max = 40
                progress = 20
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        calibrate = 0.8f + p * 0.01f
                        rulerView.scale = calibrate
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            addView(bar)
        })
        body.note("刻度按屏幕逻辑密度换算，未校准时有 1–3% 误差；校准后可用于日常估测。")
    }

    /** 横向 + 纵向刻度尺。 */
    private class RulerView(context: Context) : View(context) {
        var scale = 1f
            set(value) { field = value; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = resources.displayMetrics.xdpi / 25.4f   // 每毫米像素

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(T.surface)
            paint.color = T.ink
            paint.strokeWidth = 2f
            val mm = density * scale
            // 横向
            val baseY = height * 0.10f
            canvas.drawLine(0f, baseY, width.toFloat(), baseY, paint)
            var i = 0
            while (i * mm < width) {
                val x = i * mm + mm * 0.5f
                val len = when {
                    i % 10 == 0 -> height * 0.10f
                    i % 5 == 0 -> height * 0.06f
                    else -> height * 0.035f
                }
                canvas.drawLine(x, baseY, x, baseY + len, paint)
                if (i % 10 == 0 && i > 0) {
                    paint.textSize = 26f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("${i / 10}", x, baseY + len + 30f, paint)
                }
                i++
            }
            // 纵向
            val baseX = width * 0.10f
            canvas.drawLine(baseX, 0f, baseX, height.toFloat(), paint)
            var j = 0
            while (j * mm < height) {
                val y = j * mm + mm * 0.5f
                val len = when {
                    j % 10 == 0 -> width * 0.08f
                    j % 5 == 0 -> width * 0.05f
                    else -> width * 0.03f
                }
                canvas.drawLine(baseX, y, baseX + len, y, paint)
                j++
            }
            paint.color = T.ink2
            paint.textSize = 24f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("cm", width - 10f, height - 12f, paint)
        }
    }

    // ---------------- 水平仪 ----------------

    private fun level(act: ToolActivity, body: LinearLayout) {
        val view = LevelView(act)
        body.addView(act.card(pad = 12) {
            sectionTitle("气泡水平仪")
            addView(view, LinearLayout.LayoutParams(-1, act.dp(280)))
            val info = act.tv("把手机平放在被测面上", 13f, T.ink2)
            addView(info)
            view.onAngle = { pitch, roll ->
                val flat = kotlin.math.abs(pitch) < 1.0 && kotlin.math.abs(roll) < 1.0
                info.text = if (flat) "已水平 ✓  俯仰 0.0°  横滚 0.0°"
                else "俯仰 %+.1f°  横滚 %+.1f°".format(pitch, roll)
                info.setTextColor(if (flat) T.ok else T.ink2)
            }
        })
        body.note("基于重力加速度计算倾角，适用于桌面、画框、置物架的粗略找平。")
        startSensor(act, Sensor.TYPE_ACCELEROMETER) { e ->
            val gx = e.values[0]
            val gy = e.values[1]
            val gz = e.values[2]
            val pitch = Math.toDegrees(kotlin.math.atan2(gy.toDouble(), gz.toDouble())).toFloat()
            val roll = Math.toDegrees(kotlin.math.atan2(-gx.toDouble(), gz.toDouble())).toFloat()
            view.update(pitch, roll)
        }
    }

    private class LevelView(context: Context) : View(context) {
        private var pitch = 0f
        private var roll = 0f
        var onAngle: ((Float, Float) -> Unit)? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun update(p: Float, r: Float) {
            pitch = p
            roll = r
            onAngle?.invoke(p, r)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val rad = minOf(cx, cy) * 0.82f
            paint.style = Paint.Style.FILL
            paint.color = T.field
            canvas.drawCircle(cx, cy, rad, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = if (kotlin.math.abs(pitch) < 1f && kotlin.math.abs(roll) < 1f) T.ok else T.line
            canvas.drawCircle(cx, cy, rad, paint)
            paint.strokeWidth = 1.5f
            paint.color = T.line
            canvas.drawCircle(cx, cy, rad * 0.5f, paint)
            canvas.drawLine(cx - rad, cy, cx + rad, cy, paint)
            canvas.drawLine(cx, cy - rad, cx, cy + rad, paint)
            // 气泡位置：角度映射到半径，最大 30°
            val maxAngle = 20f
            val bx = cx + (roll / maxAngle).coerceIn(-1f, 1f) * rad * 0.85f
            val by = cy + (pitch / maxAngle).coerceIn(-1f, 1f) * rad * 0.85f
            paint.style = Paint.Style.FILL
            paint.color = T.withAlpha(T.primary, 0x33)
            canvas.drawCircle(cx, cy, rad * 0.16f, paint)
            paint.color = if (kotlin.math.abs(pitch) < 1f && kotlin.math.abs(roll) < 1f) T.ok else T.primary
            canvas.drawCircle(bx, by, rad * 0.13f, paint)
        }
    }

    // ---------------- 指南针 ----------------

    private fun compass(act: ToolActivity, body: LinearLayout) {
        val view = CompassView(act)
        body.addView(act.card(pad = 12) {
            sectionTitle("指南针")
            addView(view, LinearLayout.LayoutParams(-1, act.dp(300)))
            val info = act.tv("正在读取方向…", 13f, T.ink2)
            addView(info)
            view.onDegree = { deg ->
                val dir = directionName(deg)
                info.text = "$dir  %.0f°".format(deg)
                info.setTextColor(T.ink)
            }
        })
        body.note("手机内磁力计读数，靠近磁铁、音箱或金属桌面时会受干扰。")
        startSensor(act, Sensor.TYPE_ROTATION_VECTOR) { e ->
            val rm = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rm, e.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rm, orientation)
            var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (deg < 0) deg += 360f
            view.update(deg)
        }
    }

    private fun directionName(deg: Float): String {
        val names = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
        val idx = (((deg + 22.5f) % 360) / 45f).toInt()
        return names[idx.coerceIn(0, 7)]
    }

    private class CompassView(context: Context) : View(context) {
        private var deg = 0f
        var onDegree: ((Float) -> Unit)? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun update(d: Float) {
            deg = d
            onDegree?.invoke(d)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val rad = minOf(cx, cy) * 0.9f
            paint.style = Paint.Style.FILL
            paint.color = T.field
            canvas.drawCircle(cx, cy, rad, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = T.line
            canvas.drawCircle(cx, cy, rad, paint)
            // 刻度随手机旋转：整个盘逆向转 -deg
            canvas.save()
            canvas.rotate(-deg, cx, cy)
            paint.color = T.ink3
            paint.textSize = rad * 0.16f
            paint.textAlign = Paint.Align.CENTER
            for (i in 0 until 360 step 15) {
                val long = i % 90 == 0
                paint.strokeWidth = if (long) 3f else 1.5f
                paint.color = if (long) T.ink2 else T.ink3
                val r1 = rad * if (long) 0.78f else 0.86f
                val a = Math.toRadians(i.toDouble())
                val sx = cx + (kotlin.math.sin(a) * r1).toFloat()
                val sy = cy - (kotlin.math.cos(a) * r1).toFloat()
                val ex = cx + (kotlin.math.sin(a) * rad * 0.95f).toFloat()
                val ey = cy - (kotlin.math.cos(a) * rad * 0.95f).toFloat()
                canvas.drawLine(sx, sy, ex, ey, paint)
            }
            val labels = listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
            paint.color = T.ink
            paint.textSize = rad * 0.22f
            for ((a, s) in labels) {
                val rad2 = Math.toRadians(a.toDouble())
                val tx = cx + (kotlin.math.sin(rad2) * rad * 0.6f).toFloat()
                val ty = cy - (kotlin.math.cos(rad2) * rad * 0.6f).toFloat() + paint.textSize / 3
                paint.color = if (s == "N") T.danger else T.ink
                canvas.drawText(s, tx, ty, paint)
            }
            canvas.restore()
            // 固定的顶部指针
            paint.style = Paint.Style.FILL
            paint.color = T.primary
            val path = android.graphics.Path().apply {
                moveTo(cx, cy - rad * 0.92f)
                lineTo(cx - rad * 0.06f, cy - rad * 0.78f)
                lineTo(cx + rad * 0.06f, cy - rad * 0.78f)
                close()
            }
            canvas.drawPath(path, paint)
            paint.color = T.ink
            paint.textSize = rad * 0.16f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("%.0f°".format(deg), cx, cy + rad * 0.52f, paint)
        }
    }

    // ---------------- 噪音 ----------------

    private fun noise(act: ToolActivity, body: LinearLayout) {
        var recording = false
        var record: AudioRecord? = null
        val bar = android.widget.ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        val value = act.tv("— dB", 30f, T.ink, true)
        val hint = act.tv("点开始后把手机对着噪声源", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("环境噪音")
            addView(value)
            addView(bar, LinearLayout.LayoutParams(-1, act.dp(14)).also { it.topMargin = act.dp(10) })
            addView(hint)
            val start = act.tv("开始测量", 13.5f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = T.fillBg(T.primary, act.dp(T.R_SMALL).toFloat())
                setPadding(0, act.dp(12), 0, act.dp(12))
                isClickable = true
            }
            addView(start, LinearLayout.LayoutParams(-1, -2).also { lp -> lp.topMargin = act.dp(12) })
            start.setOnClickListener {
                if (recording) {
                    recording = false
                    start.text = "开始测量"
                    runCatching { record?.stop(); record?.release() }
                    record = null
                    return@setOnClickListener
                }
                act.askPermission(android.Manifest.permission.RECORD_AUDIO) { granted ->
                    if (!granted) {
                        Ui.toast(act, "需要麦克风权限才能测量")
                        return@askPermission
                    }
                    val rate = 16000
                    val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    val rec = runCatching {
                        AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 4096))
                    }.getOrNull()
                    if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                        Ui.toast(act, "无法启动录音")
                        return@askPermission
                    }
                    record = rec
                    recording = true
                    start.text = "停止测量"
                    rec.startRecording()
                    Thread {
                        val buf = ShortArray(2048)
                        var smooth = 0.0
                        while (recording) {
                            val n = rec.read(buf, 0, buf.size)
                            if (n <= 0) continue
                            var sum = 0.0
                            for (i in 0 until n) {
                                val v = buf[i] / 32768.0
                                sum += v * v
                            }
                            val rms = kotlin.math.sqrt(sum / n)
                            val db = if (rms <= 1e-7) 0.0 else 20 * kotlin.math.log10(rms) + 94.0
                            smooth = if (smooth == 0.0) db else smooth * 0.7 + db * 0.3
                            act.runOnUiThread {
                                val v = smooth.coerceIn(0.0, 120.0)
                                value.text = "%.0f dB".format(v)
                                bar.progress = v.toInt()
                                hint.text = when {
                                    v < 30 -> "很安静（图书馆级）"
                                    v < 50 -> "安静（普通室内）"
                                    v < 70 -> "一般（正常交谈）"
                                    v < 85 -> "较吵（马路 / 地铁）"
                                    else -> "很吵，注意保护听力"
                                }
                                value.setTextColor(
                                    when {
                                        v < 50 -> T.ok
                                        v < 70 -> T.ink
                                        v < 85 -> T.warn
                                        else -> T.danger
                                    },
                                )
                            }
                        }
                    }.start()
                }
            }
        })
        body.note("读数为未校准的相对声压级（以 94dB 基准估算），绝对值可能有 ±10dB 偏差，更适合看相对变化。")
        act.onCleanup {
            recording = false
            runCatching { record?.stop(); record?.release() }
            record = null
        }
    }

    // ---------------- 金属探测 ----------------

    private fun metal(act: ToolActivity, body: LinearLayout) {
        var base = 0f
        val value = act.tv("— μT", 28f, T.ink, true)
        val bar = android.widget.ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        val hint = act.tv("将手机靠近可疑位置，数值明显上升即为金属", 12f, T.ink2)
        body.addView(act.card {
            sectionTitle("磁场强度")
            addView(value)
            addView(bar, LinearLayout.LayoutParams(-1, act.dp(14)).also { it.topMargin = act.dp(10) })
            addView(hint)
            btn("重新校准基准") { base = 0f; Ui.toast(act, "基准已重置，请远离金属静置片刻") }
        })
        body.note("地球磁场约 25–65 μT。读数突然升高或剧烈跳动时，附近可能有金属或磁体。")
        startSensor(act, Sensor.TYPE_MAGNETIC_FIELD) { e ->
            val x = e.values[0]
            val y = e.values[1]
            val z = e.values[2]
            val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            if (base == 0f) base = magnitude
            val delta = magnitude - base
            val percent = ((delta / 60f) * 100).toInt().coerceIn(0, 100)
            value.text = "%.1f μT".format(magnitude)
            value.setTextColor(when {
                percent > 70 -> T.danger
                percent > 35 -> T.warn
                else -> T.ink
            })
            bar.progress = percent
            hint.text = when {
                percent > 70 -> "检测到明显磁场变化，附近可能有金属"
                percent > 35 -> "磁场有变化，可再靠近一点"
                else -> "磁场平稳"
            }
        }
    }

    // ---------------- 传感器通用 ----------------

    private fun startSensor(act: ToolActivity, type: Int, onEvent: (SensorEvent) -> Unit) {
        val sm = sensorManager(act) ?: return
        val sensor = sm.getDefaultSensor(type)
        if (sensor == null) {
            Ui.toast(act, "这台设备没有对应的传感器")
            return
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onEvent(event)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        act.onCleanup { sm.unregisterListener(listener) }
    }
}
