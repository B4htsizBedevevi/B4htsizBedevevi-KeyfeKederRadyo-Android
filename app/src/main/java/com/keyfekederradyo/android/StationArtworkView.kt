package com.keyfekederradyo.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.ImageView
import kotlin.math.min

class StationArtworkView(context: Context) : ImageView(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var label = "RADYO"
    private var genre = ""

    fun bind(name: String, genreValue: String) {
        label = name.take(12).uppercase()
        genre = genreValue.lowercase()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val r = min(w, h) * .5f
        val accent = when {
            "rock" in genre -> 0xFFFF8A3D.toInt()
            "arabesk" in genre || "slow" in genre -> 0xFFE85B86.toInt()
            "jazz" in genre || "lounge" in genre -> 0xFF8A7BFF.toInt()
            "classical" in genre -> 0xFFFFC857.toInt()
            "elect" in genre || "disco" in genre -> 0xFF26D9C5.toInt()
            "pop" in genre -> 0xFFFF6B35.toInt()
            else -> 0xFFFF7A00.toInt()
        }
        bgPaint.shader = LinearGradient(0f,0f,w,h,0xFF151518.toInt(),0xFF09090B.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,bgPaint)
        glowPaint.shader = RadialGradient(w*.72f,h*.18f,r*.75f,accent,0x00111111,Shader.TileMode.CLAMP)
        canvas.drawCircle(w*.72f,h*.18f,r*.75f,glowPaint)
        glowPaint.shader = RadialGradient(w*.18f,h*.82f,r*.62f,accent,0x00111111,Shader.TileMode.CLAMP)
        canvas.drawCircle(w*.18f,h*.82f,r*.62f,glowPaint)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeWidth = maxOf(1f,w*.018f)
        linePaint.color = accent
        val bars=18
        val gap=w/(bars+3)
        for(i in 0 until bars){
            val x=gap*(i+1)
            val wave=((i*37+label.length*11)%9)/9f
            val bh=h*(.13f+wave*.28f)
            canvas.drawLine(x,h*.78f,x,h*.78f-bh,linePaint)
        }
        linePaint.strokeWidth=maxOf(1f,w*.01f)
        linePaint.color=0x66FFFFFF
        canvas.drawCircle(w*.5f,h*.42f,r*.48f,linePaint)
        canvas.drawCircle(w*.5f,h*.42f,r*.31f,linePaint)
        val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
            color=0xFFF4F4F6.toInt(); textAlign=Paint.Align.CENTER; textSize=w*.11f
            typeface=android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
        }
        canvas.drawText(label,w*.5f,h*.52f,textPaint)
        textPaint.color=0xB3FFFFFF.toInt(); textPaint.textSize=w*.055f
        canvas.drawText(if(genre.isBlank()) "CANLI RADYO" else genre.uppercase().take(18),w*.5f,h*.61f,textPaint)
    }
}