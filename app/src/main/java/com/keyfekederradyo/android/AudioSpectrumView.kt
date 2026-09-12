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
    private var phase = 0.0
    private var active = false

    init { paint.strokeCap = Paint.Cap.ROUND }

    fun setPlaying(playing: Boolean) {
        active = playing
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = 28
        val gap = width.toFloat() / (count + 1)
        val center = height / 2f
        val maxHeight = height * 0.82f
        paint.color = 0xFFFF7A00.toInt()
        paint.strokeWidth = maxOf(2f, gap * .16f)
        for (i in 0 until count) {
            val envelope = 0.25 + 0.75 * (1.0 - abs(i - count / 2f) / (count / 2f))
            val wave = if (active) abs(sin(phase + i * .73)) else .12
            val h = maxHeight * envelope * (.16 + .84 * wave)
            val x = gap * (i + 1)
            canvas.drawLine(x, center - h / 2, x, center + h / 2, paint)
        }
        if (active) {
            phase += .11
            postInvalidateDelayed(32)
        }
    }
}
