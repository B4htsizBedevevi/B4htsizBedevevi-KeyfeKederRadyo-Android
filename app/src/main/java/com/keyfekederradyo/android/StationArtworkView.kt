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
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var label = "RADYO"
    private var genre = ""
    private var seed = 0

    fun bind(name: String, genreValue: String) {
        label = name.take(12).uppercase()
        genre = genreValue.lowercase()
        seed = name.hashCode().ushr(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
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
        bgPaint.shader = LinearGradient(0f,0f,w,h,0xFF17171B.toInt(),0xFF08080A.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,bgPaint)

        val gx = .22f + ((seed % 55) / 100f)
        val gy = .16f + (((seed / 17) % 45) / 100f)
        glowPaint.shader = RadialGradient(w*gx,h*gy,r*.85f,accent,0x00111111,Shader.TileMode.CLAMP)
        canvas.drawCircle(w*gx,h*gy,r*.85f,glowPaint)
        glowPaint.shader = RadialGradient(w*(1f-gx*.55f),h*.88f,r*.58f,accent,0x0009090A,Shader.TileMode.CLAMP)
        canvas.drawCircle(w*(1f-gx*.55f),h*.88f,r*.58f,glowPaint)

        linePaint.style = Paint.Style.STROKE; linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.color = accent; linePaint.strokeWidth = maxOf(1f,w*.012f)
        val cx = w*.5f; val cy = h*.40f
        val rings = 3 + seed % 3
        for (i in 1..rings) canvas.drawCircle(cx,cy,r*(.15f+i*.105f),linePaint.apply{alpha=80+i*28})
        linePaint.alpha=150
        val bars=16
        val gap=w/(bars+3)
        for(i in 0 until bars){
            val wave=((i*37+label.length*11+seed)%11)/11f
            val bh=h*(.10f+wave*.30f)
            canvas.drawLine(gap*(i+1),h*.82f,gap*(i+1),h*.82f-bh,linePaint)
        }

        textPaint.textAlign=Paint.Align.CENTER
        textPaint.color=0xFFF5F5F7.toInt()
        textPaint.textSize=w*.095f
        textPaint.typeface=android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
        canvas.drawText(label,cx,h*.50f,textPaint)
        textPaint.color=0xB8FFFFFF.toInt(); textPaint.textSize=w*.048f
        canvas.drawText(if(genre.isBlank()) "CANLI RADYO" else genre.uppercase().take(18),cx,h*.59f,textPaint)

        linePaint.style=Paint.Style.FILL; linePaint.color=accent; linePaint.alpha=190
        canvas.drawCircle(w*.12f,h*.12f,maxOf(1.5f,w*.012f),linePaint)
        canvas.drawCircle(w*.88f,h*.74f,maxOf(1.5f,w*.012f),linePaint)
    }
}