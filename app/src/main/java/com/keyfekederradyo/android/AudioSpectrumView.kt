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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var active = false
    private var attached = false

    init { paint.strokeCap = Paint.Cap.ROUND; setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    fun setPlaying(playing: Boolean) {
        active = playing
        if (playing) postInvalidateOnAnimation() else invalidate()
    }

    fun restart() {
        phase = 0f
        invalidate()
        if (active) postInvalidateOnAnimation()
    }

    fun restart() {
        phase = 0f
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

        for (i in 0 until count) {
            val envelope = (0.25f + 0.75f * (1f - abs(i - count / 2f) / (count / 2f)))
            val wave = if (active) abs(sin((phase + i * .73f).toDouble())).toFloat() else .10f
            val h = maxHeight * envelope * (.16f + .84f * wave)
            val x = gap * (i + 1)
            canvas.drawLine(x, center - h / 2f, x, center + h / 2f, paint)
        }

        if (active && attached && width > 0 && height > 0) {
            phase += .11f
            postInvalidateOnAnimation()
        }
    }
}
