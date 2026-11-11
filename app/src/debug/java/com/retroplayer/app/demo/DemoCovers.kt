package com.retroplayer.app.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.random.Random

/** DEBUG ONLY. The three placeholder covers (bright, dark, busy). */
internal object DemoCovers {

    fun bright(size: Int): Bitmap = draw(size) { c, s, p ->
        c.drawColor(Color.parseColor("#FFD23F"))
        p.color = Color.parseColor("#FF8C42"); c.drawCircle(s * 0.35f, s * 0.4f, s * 0.3f, p)
        p.color = Color.parseColor("#F25F8C"); c.drawCircle(s * 0.68f, s * 0.62f, s * 0.26f, p)
        p.color = Color.parseColor("#FFF4D6"); c.drawCircle(s * 0.62f, s * 0.28f, s * 0.1f, p)
    }

    fun dark(size: Int): Bitmap = draw(size) { c, s, p ->
        p.shader = LinearGradient(0f, 0f, 0f, s * 0.7f, Color.parseColor("#0B1638"), Color.parseColor("#27406E"), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, s, s * 0.7f, p)
        p.shader = null
        p.color = Color.parseColor("#0A1022"); c.drawRect(0f, s * 0.7f, s, s, p)
        p.color = Color.parseColor("#F3E6B5"); c.drawCircle(s * 0.68f, s * 0.3f, s * 0.12f, p)
        p.color = Color.parseColor("#1A2C55"); c.drawCircle(s * 0.73f, s * 0.26f, s * 0.11f, p)
        p.color = Color.parseColor("#3A5A8C"); c.drawRect(0f, s * 0.695f, s, s * 0.71f, p)
    }

    fun busy(size: Int): Bitmap = draw(size) { c, s, p ->
        val colors = listOf("#E63946", "#F1C453", "#2A9D8F", "#264653", "#F4A261", "#8E7DBE", "#FFFFFF").map(Color::parseColor)
        val rnd = Random(7)
        val cw = s / 5f
        val ch = s / 4f
        for (row in 0 until 4) for (col in 0 until 5) {
            val l = col * cw
            val t = row * ch
            p.color = colors[rnd.nextInt(colors.size)]
            c.drawRect(l, t, l + cw, t + ch, p)
            p.color = colors[rnd.nextInt(colors.size)]
            when (rnd.nextInt(3)) {
                0 -> c.drawCircle(l + cw / 2, t + ch / 2, cw * 0.32f, p)
                1 -> c.drawPath(Path().apply { moveTo(l + cw / 2, t + ch * 0.15f); lineTo(l + cw * 0.85f, t + ch * 0.85f); lineTo(l + cw * 0.15f, t + ch * 0.85f); close() }, p)
                else -> c.drawRect(l + cw * 0.22f, t + ch * 0.22f, l + cw * 0.78f, t + ch * 0.78f, p)
            }
        }
    }

    private inline fun draw(size: Int, block: (Canvas, Float, Paint) -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        block(Canvas(bmp), size.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
        return bmp
    }
}
