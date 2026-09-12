package com.keyfekederradyo.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.ImageView
import android.graphics.Path
import kotlin.math.min
import kotlin.math.sin

class StationArtworkView(context: Context) : ImageView(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var label = "RADYO"
    private var genre = ""
    private var logoUrl = ""
    private var seed = 0
    private var active = false
    private var phase = 0f

    fun bind(name: String, genreValue: String, artworkUrl: String = "") {
        label = name.take(12).uppercase()
        genre = genreValue.lowercase()
        logoUrl = "brand"
        seed = name.hashCode().ushr(1)
        setImageResource(R.drawable.keyfe_keder_brand)
        scaleType = ImageView.ScaleType.CENTER_CROP
        invalidate()
    }

    fun setPlaying(playing: Boolean) {
        active = playing
        if (playing) postInvalidateOnAnimation() else invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val r = min(w, h) * .5f
        val pulse = if (active) (0.78f + 0.22f * (0.5f + 0.5f * sin(phase.toDouble()))).toFloat() else 0.78f
        val accent = when {
            "rock" in genre -> 0xFFFF8A3D.toInt()
            "arabesk" in genre || "slow" in genre -> 0xFFE85B86.toInt()
            "jazz" in genre || "lounge" in genre -> 0xFF8A7BFF.toInt()
            "classical" in genre -> 0xFFFFC857.toInt()
            "elect" in genre || "disco" in genre -> 0xFF26D9C5.toInt()
            "pop" in genre -> 0xFFFF6B35.toInt()
            else -> 0xFFFF7A00.toInt()
        }

        bgPaint.shader = LinearGradient(0f,0f,w,h,0xFF17171B.toInt(),0xFF08080A.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,bgPaint)

        // Real station artwork (or the branded fallback) is drawn by ImageView.
        super.onDraw(canvas)

        // Keep the artwork readable while preserving the radio-app atmosphere.
        bgPaint.shader = null
        bgPaint.color = 0x26000000
        canvas.drawRect(0f,0f,w,h,bgPaint)

        // Clean fallback poster when the station has no artwork URL.
        if (logoUrl.isBlank()) {
            val poster = Paint(Paint.ANTI_ALIAS_FLAG)
            poster.color = 0xFF0B0B0E.toInt()
            canvas.drawRoundRect(w*.04f,h*.04f,w*.96f,h*.96f,r*.16f,r*.16f,poster)
            poster.style = Paint.Style.STROKE
            poster.strokeWidth = maxOf(2f,w*.012f)
            poster.color = accent
            val p = Path()
            p.moveTo(w*.15f,h*.63f)
            p.cubicTo(w*.30f,h*.40f,w*.38f,h*.78f,w*.54f,h*.55f)
            p.cubicTo(w*.66f,h*.38f,w*.76f,h*.67f,w*.88f,h*.44f)
            canvas.drawPath(p,poster)
            poster.style = Paint.Style.FILL
            poster.color = accent
            canvas.drawCircle(w*.72f,h*.27f,r*.10f,poster)
        }

        // Artwork is intentionally clean. Glow is rendered by the parent card/player.
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = maxOf(1.5f,w*.010f)
        linePaint.color = accent
        linePaint.alpha = if(active) (55 + pulse*45f).toInt() else 48
        canvas.drawRoundRect(w*.08f,h*.08f,w*.92f,h*.92f,r*.16f,r*.16f,linePaint)
        if (active && width > 0 && height > 0) {
            phase += .085f
            postInvalidateOnAnimation()
        }
    }
}
