package `in`.ankitsaroj.diable.wallpaper

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

/** Original moving wallpapers. The style is stored in app prefs and read each frame. */
class DiableLiveWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = MovingEngine()

    private inner class MovingEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var running = false
        private var phase = 0f
        private val tick = object : Runnable {
            override fun run() {
                drawFrame()
                if (running) handler.postDelayed(this, 32L)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            handler.removeCallbacks(tick)
            if (visible) handler.post(tick)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            running = false
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            running = false
            handler.removeCallbacks(tick)
            super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (_: Exception) {
                null
            } ?: return
            try {
                phase += 0.01f
                when (currentStyle(this@DiableLiveWallpaper)) {
                    "stars" -> drawStars(canvas)
                    "waves" -> drawWaves(canvas)
                    "aurora" -> drawAurora(canvas)
                    else -> drawDrift(canvas)
                }
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }

        private fun drawDrift(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val shift = (sin(phase.toDouble()) * 0.15 + 0.5).toFloat()
            paint.shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(0xFF0B1D36.toInt(), blend(0xFF1B4F72.toInt(), 0xFF6C3483.toInt(), shift), 0xFF0E2F3A.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }

        private fun drawStars(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawColor(0xFF070B16.toInt())
            paint.shader = null
            paint.color = 0xFFFFFFFF.toInt()
            repeat(80) { i ->
                val x = ((i * 97) % 1000) / 1000f * w
                val y = ((i * 57 + (phase * 40).toInt()) % 1000) / 1000f * h
                val twinkle = (sin(phase * 3 + i) * 0.5 + 0.5).toFloat()
                paint.alpha = (80 + twinkle * 175).toInt()
                canvas.drawCircle(x, y, if (i % 11 == 0) 3.2f else 1.4f, paint)
            }
            paint.alpha = 255
        }

        private fun drawWaves(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawColor(0xFF06283D.toInt())
            paint.shader = null
            repeat(6) { band ->
                paint.color = if (band % 2 == 0) 0xFF1B6CA8.toInt() else 0xFF47A3C6.toInt()
                paint.alpha = 90
                val base = h * (0.35f + band * 0.1f)
                val path = android.graphics.Path()
                path.moveTo(0f, h)
                var x = 0f
                while (x <= w) {
                    val y = base + (sin(phase * 1.4 + x / 140f + band) * 28f).toFloat()
                    path.lineTo(x, y)
                    x += 12f
                }
                path.lineTo(w, h)
                path.close()
                canvas.drawPath(path, paint)
            }
            paint.alpha = 255
        }

        private fun drawAurora(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawColor(0xFF041018.toInt())
            repeat(3) { i ->
                val cx = w * (0.3f + i * 0.2f)
                val cy = h * (0.25f + (sin(phase + i) * 0.08f).toFloat())
                paint.shader = RadialGradient(
                    cx, cy, w * 0.55f,
                    intArrayOf(
                        if (i == 1) 0x8869F0AE.toInt() else 0x8848C9B0.toInt(),
                        0x00000000,
                    ),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            paint.shader = null
        }
    }

    companion object {
        const val PREFS = "diable_live"
        const val STYLE = "style"

        val styles = listOf(
            "drift" to "Drift",
            "stars" to "Stars",
            "waves" to "Waves",
            "aurora" to "Aurora",
        )

        fun currentStyle(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(STYLE, "drift") ?: "drift"

        fun request(context: Context, style: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(STYLE, style).apply()
            val component = android.content.ComponentName(context, DiableLiveWallpaper::class.java)
            val change = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(change) }.isFailure) {
                runCatching {
                    context.startActivity(
                        Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
}

private fun blend(a: Int, b: Int, t: Float): Int {
    fun ch(shift: Int): Int {
        val av = (a shr shift) and 0xFF
        val bv = (b shr shift) and 0xFF
        return (av + (bv - av) * t).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
