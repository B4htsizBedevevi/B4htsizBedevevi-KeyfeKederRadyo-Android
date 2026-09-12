package com.keyfekederradyo.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.ImageView
import kotlin.math.min
import kotlin.math.sin

class StationArtworkView(context: Context) : ImageView(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var label = "RADYO"
    private var genre = ""
    private var active = false
    private var phase = 0f

    fun bind(name: String, genreValue: String, artworkUrl: String = "") {
        label = name.take(12).uppercase()
        genre = genreValue.lowercase()
        setImageResource(R.drawable.keyfe_keder_brand)
        scaleType = ScaleType.CENTER_INSIDE
        adjustViewBounds = true
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
        val pulse = if (active) (0.72f + 0.28f * (0.5f + 0.5f * sin(phase.toDouble()))).toFloat() else .72f
        val accent = when {
            "rock" in genre -> 0xFFFF8A3D.toInt()
            "arabesk" in genre || "slow" in genre -> 0xFFE85B86.toInt()
            "jazz" in genre || "lounge" in genre -> 0xFF8A7BFF.toInt()
            "classical" in genre -> 0xFFFFC857.toInt()
            "elect" in genre || "disco" in genre -> 0xFF26D9C5.toInt()
            "pop" in genre -> 0xFFFF6B35.toInt()
            else -> 0xFFFF7A00.toInt()
        }

        // Neutral dark backing lets a transparent PNG breathe instead of creating a black-on-black block.
        bgPaint.shader = LinearGradient(0f,0f,w,h,0xFF161619.toInt(),0xFF09090B.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRoundRect(w*.02f,h*.02f,w*.98f,h*.98f,r*.18f,r*.18f,bgPaint)
        super.onDraw(canvas)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = maxOf(1.5f,w*.010f)
        linePaint.color = accent
        linePaint.alpha = if(active) (48 + pulse*48f).toInt() else 42
        canvas.drawRoundRect(w*.075f,h*.075f,w*.925f,h*.925f,r*.16f,r*.16f,linePaint)

        if (active && width > 0 && height > 0) {
            phase += .075f
            postInvalidateOnAnimation()
        }
    }
}
