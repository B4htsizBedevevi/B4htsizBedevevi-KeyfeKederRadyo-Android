package com.keyfekederradyo.android

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
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

    fun submitList(stations: List<Station>) { items.clear(); items.addAll(stations); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val c = parent.context
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(18.dp(c), 10.dp(c), 10.dp(c), 10.dp(c))
            layoutParams = RecyclerView.LayoutParams(-1, 78.dp(c))
        }
        val logo = TextView(c).apply {
            gravity = Gravity.CENTER; textSize = 16f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.DKGRAY) }
            layoutParams = LinearLayout.LayoutParams(54.dp(c), 54.dp(c)).apply { rightMargin = 14.dp(c) }
        }
        val text = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        val title = TextView(c).apply { textSize = 18f; setTextColor(Color.WHITE); maxLines = 1 }
        val meta = TextView(c).apply { textSize = 12f; setTextColor(Color.GRAY); maxLines = 1 }
        text.addView(title); text.addView(meta)
        val fav = ImageButton(c).apply { setBackgroundColor(Color.TRANSPARENT); setImageResource(android.R.drawable.btn_star_big_off); layoutParams = LinearLayout.LayoutParams(48.dp(c), 48.dp(c)) }
        row.addView(logo); row.addView(text); row.addView(fav)
        return Holder(row, logo, title, meta, fav)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val s = items[position]
        h.logo.text = s.name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
        h.title.text = s.name
        h.meta.text = listOf(s.genre, s.country, s.quality).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Canlı radyo" }
        h.itemView.setOnClickListener { onClick(s) }
        h.favorite.setImageResource(if (isFavorite(s)) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        h.favorite.setOnClickListener { onFavorite(s) }
    }
    override fun getItemCount() = items.size
    class Holder(v: android.view.View, val logo: TextView, val title: TextView, val meta: TextView, val favorite: ImageButton) : RecyclerView.ViewHolder(v)
}
private fun Int.dp(c: android.content.Context) = (this * c.resources.displayMetrics.density).toInt()
