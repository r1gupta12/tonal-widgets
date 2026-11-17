package com.rgupta.tonalwidgets

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal shape wallpapers drawn in code. Setting one also re-tints every
 * widget, because Material You takes its colours from the wallpaper.
 */
object Wallpapers {
    data class Style(
        val name: String,
        val ground: Int,
        val scallop: Int,
        val circle: Int,
        val pill: Int,
        val peaks: Boolean = false,
        val skyTop: Int = 0,
    )

    val ALL = listOf(
        Style("Lilac", Color.parseColor("#EEE9F7"), Color.parseColor("#5B4BDB"), Color.parseColor("#D4F26A"), Color.parseColor("#16131F")),
        Style("Midnight", Color.parseColor("#121019"), Color.parseColor("#7B6CF0"), Color.parseColor("#D4F26A"), Color.parseColor("#2A2540")),
        Style("Lime", Color.parseColor("#D4F26A"), Color.parseColor("#16131F"), Color.parseColor("#B9AEFF"), Color.parseColor("#FFFFFF")),
        Style("Mono", Color.parseColor("#9C9C9C"), Color.parseColor("#1C1C1C"), Color.parseColor("#6E6E6E"), Color.parseColor("#E6E6E6"), true, Color.parseColor("#DADADA")),
        Style("Mono Dark", Color.parseColor("#141414"), Color.parseColor("#050505"), Color.parseColor("#3A3A3A"), Color.parseColor("#8A8A8A"), true, Color.parseColor("#4A4A4A")),
        Style("Clay", Color.parseColor("#F3E9DF"), Color.parseColor("#D9734E"), Color.parseColor("#1F3A33"), Color.parseColor("#E9C9A8")),
    )

    fun render(style: Style, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (style.peaks) {
            drawPeaks(c, p, style, w.toFloat(), h.toFloat())
            return bmp
        }
        c.drawColor(style.ground)

        // Big scallop, partly off the top-left edge
        p.color = style.scallop
        c.drawPath(scallop(w * 0.18f, h * 0.30f, w * 0.62f, 12, 0.09f), p)

        // Circle, partly off the bottom-right edge
        p.color = style.circle
        c.drawCircle(w * 0.88f, h * 0.80f, w * 0.38f, p)

        // Small pill accent
        p.color = style.pill
        val pw = w * 0.34f
        val ph = w * 0.12f
        c.drawRoundRect(RectF(w * 0.10f, h * 0.66f, w * 0.10f + pw, h * 0.66f + ph), ph / 2, ph / 2, p)
        return bmp
    }

    /** Grey sky with layered mountain ridges and one sharp lit peak. */
    private fun drawPeaks(c: Canvas, p: Paint, s: Style, w: Float, h: Float) {
        p.shader = LinearGradient(0f, 0f, 0f, h, s.skyTop, s.ground, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // Far ridge
        p.color = s.circle
        c.drawPath(Path().apply {
            moveTo(0f, h * 0.62f)
            lineTo(w * 0.22f, h * 0.55f); lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.70f, h * 0.50f); lineTo(w, h * 0.57f)
            lineTo(w, h); lineTo(0f, h); close()
        }, p)

        // Main peak: lit left face + shadowed right face
        val apexX = w * 0.50f
        val apexY = h * 0.36f
        p.color = s.pill
        c.drawPath(Path().apply {
            moveTo(apexX, apexY)
            lineTo(w * 0.05f, h * 0.78f); lineTo(w * 0.40f, h * 0.80f); close()
        }, p)
        p.color = s.scallop
        c.drawPath(Path().apply {
            moveTo(apexX, apexY)
            lineTo(w * 0.40f, h * 0.80f); lineTo(w * 1.05f, h * 0.70f); close()
        }, p)

        // Foreground dune
        p.color = s.scallop
        c.drawPath(Path().apply {
            moveTo(0f, h * 0.84f)
            quadTo(w * 0.45f, h * 0.74f, w, h * 0.82f)
            lineTo(w, h); lineTo(0f, h); close()
        }, p)
    }

    private fun scallop(cx: Float, cy: Float, r: Float, lobes: Int, depth: Float): Path {
        val path = Path()
        val steps = 360
        for (i in 0..steps) {
            val t = (2 * Math.PI * i / steps)
            val rr = r * (1 - depth + depth * cos(lobes * t))
            val x = (cx + rr * cos(t)).toFloat()
            val y = (cy + rr * sin(t)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** Sets home and lock screen. Call off the main thread. */
    fun apply(context: Context, style: Style, w: Int, h: Int) {
        val bmp = render(style, w, h)
        WallpaperManager.getInstance(context).setBitmap(
            bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        )
    }
}
