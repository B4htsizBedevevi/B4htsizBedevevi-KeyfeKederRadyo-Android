package com.keyfekederradyo.android

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StationAdapter(
    private val onClick: (Station) -> Unit,
    private val isFavorite: (Station) -> Boolean,
    private val onFavorite: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.Holder>() {
    private val items = mutableListOf<Station>()

    fun submitList(stations: List<Station>) {
        items.clear()
        items.addAll(stations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val c = parent.context
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(c), 8.dp(c), 10.dp(c), 8.dp(c))
            layoutParams = RecyclerView.LayoutParams(-1, 82.dp(c)).apply {
                leftMargin = 10.dp(c); rightMargin = 10.dp(c); bottomMargin = 7.dp(c)
            }
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 27, 29))
                cornerRadius = 20.dp(c).toFloat()
                setStroke(1.dp(c), Color.rgb(46, 46, 49))
            }
            isClickable = true
            isFocusable = true
        }

        val logo = TextView(c).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(48, 48, 52))
                setStroke(1.dp(c), Color.rgb(78, 78, 82))
            }
            layoutParams = LinearLayout.LayoutParams(56.dp(c), 56.dp(c)).apply { rightMargin = 13.dp(c) }
        }

        val text = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }
        val title = TextView(c).apply {
            textSize = 16f
            setTextColor(Color.rgb(245, 245, 247))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val meta = TextView(c).apply {
            textSize = 11.5f
            setTextColor(Color.rgb(145, 145, 150))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val live = TextView(c).apply {
            text = "● CANLI"
            textSize = 9.5f
            setTextColor(Color.rgb(255, 122, 0))
            visibility = View.GONE
        }
        text.addView(title, LinearLayout.LayoutParams(-1, 27.dp(c)))
        text.addView(meta, LinearLayout.LayoutParams(-1, 22.dp(c)))
        text.addView(live, LinearLayout.LayoutParams(-1, 18.dp(c)))

        val fav = ImageButton(c).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.rgb(210, 210, 214))
            layoutParams = LinearLayout.LayoutParams(48.dp(c), 48.dp(c))
            contentDescription = "Favorilere ekle"
        }
        row.addView(logo); row.addView(text); row.addView(fav)
        return Holder(row, logo, title, meta, live, fav)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val s = items[position]
        h.logo.text = s.name.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        h.title.text = s.name
        h.meta.text = listOf(s.genre, s.country, s.quality).filter { it.isNotBlank() }
            .joinToString(" • ").ifBlank { "Canlı radyo" }
        h.live.visibility = if (isFavorite(s)) View.VISIBLE else View.GONE
        h.live.text = if (isFavorite(s)) "● FAVORİ" else ""
        h.favorite.setImageResource(
            if (isFavorite(s)) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
        h.itemView.setOnClickListener {
            h.itemView.animate().scaleX(.985f).scaleY(.985f).setDuration(70).withEndAction {
                h.itemView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
            onClick(s)
        }
        h.favorite.setOnClickListener {
            h.favorite.animate().rotationBy(18f).setDuration(80).withEndAction {
                h.favorite.animate().rotation(0f).setDuration(100).start()
            }.start()
            onFavorite(s)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = items.size

    class Holder(
        v: View,
        val logo: TextView,
        val title: TextView,
        val meta: TextView,
        val live: TextView,
        val favorite: ImageButton
    ) : RecyclerView.ViewHolder(v)
}

private fun Int.dp(c: android.content.Context) = (this * c.resources.displayMetrics.density).toInt()
