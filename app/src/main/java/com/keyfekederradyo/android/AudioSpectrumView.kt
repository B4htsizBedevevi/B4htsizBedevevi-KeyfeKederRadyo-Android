package com.keyfekederradyo.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class AudioSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var active = false
    private var stationSeed = 0
    private var attached = false

    init {
        paint.strokeCap = Paint.Cap.ROUND
        glowPaint.strokeCap = Paint.Cap.ROUND
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setPlaying(playing: Boolean) {
        active = playing
        if (playing) postInvalidateOnAnimation() else invalidate()
    }

    fun setStationSeed(seed: Int) {
        stationSeed = seed
        phase = ((seed and 0xFF) / 255f) * 6.28f
        invalidate()
    }

    fun restart() {
        phase = ((stationSeed and 0xFF) / 255f) * 6.28f
        invalidate()
        if (active) postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        if (active) postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        attached = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = 28
        val gap = width.toFloat() / (count + 1).coerceAtLeast(2)
        val center = height / 2f
        val maxHeight = (height * 0.82f).coerceAtLeast(3f)
        paint.color = 0xFFFF7A00.toInt()
        paint.strokeWidth = maxOf(2f, gap * .16f)
        glowPaint.color = 0xFFFF7A00.toInt()
        glowPaint.strokeWidth = maxOf(4f, gap * .34f)
        glowPaint.alpha = if (active) 58 else 0
        glowPaint.setShadowLayer(if (active) 7f else 0f, 0f, 0f, 0xFFFF7A00.toInt())

        for (i in 0 until count) {
            val envelope = (0.25f + 0.75f * (1f - abs(i - count / 2f) / (count / 2f)))
            val wave = if (active) abs(sin((phase + i * (.51f + (stationSeed and 15) * .018f) + sin(i * .31f) * .45f).toDouble())).toFloat() else .10f
            val h = maxHeight * envelope * (.16f + .84f * wave)
            val x = gap * (i + 1)
            if (active) canvas.drawLine(x, center - h / 2f, x, center + h / 2f, glowPaint)
            canvas.drawLine(x, center - h / 2f, x, center + h / 2f, paint)
        }

        if (active && attached && width > 0 && height > 0) {
            phase += .11f
            postInvalidateOnAnimation()
        }
    }
}
