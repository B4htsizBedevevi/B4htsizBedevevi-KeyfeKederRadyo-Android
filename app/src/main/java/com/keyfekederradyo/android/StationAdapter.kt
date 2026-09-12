package com.keyfekederradyo.android

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
            layoutParams = RecyclerView.LayoutParams(-1, 82.dp(context)).apply {
                leftMargin = 10.dp(context)
                rightMargin = 10.dp(context)
                bottomMargin = 7.dp(context)
            }
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 27, 29))
                cornerRadius = 20.dp(context).toFloat()
                setStroke(1.dp(context), Color.rgb(46, 46, 49))
            }
            isClickable = true
            isFocusable = true
        }

        val logoView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(48, 48, 52))
                setStroke(1.dp(context), Color.rgb(78, 78, 82))
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(56.dp(context), 56.dp(context)).apply {
                rightMargin = 13.dp(context)
            }
        }

        val infoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }
        val titleView = TextView(context).apply {
            textSize = 16f
            setTextColor(Color.rgb(245, 245, 247))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val metaView = TextView(context).apply {
            textSize = 11.5f
            setTextColor(Color.rgb(145, 145, 150))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val badgeView = TextView(context).apply {
            textSize = 9.5f
            setTextColor(Color.rgb(255, 122, 0))
            visibility = View.GONE
        }
        infoLayout.addView(titleView, LinearLayout.LayoutParams(-1, 27.dp(context)))
        infoLayout.addView(metaView, LinearLayout.LayoutParams(-1, 22.dp(context)))
        infoLayout.addView(badgeView, LinearLayout.LayoutParams(-1, 18.dp(context)))

        val favoriteView = ImageButton(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(6.dp(context), 6.dp(context), 6.dp(context), 6.dp(context))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(42.dp(context), 42.dp(context))
            contentDescription = "Favorilere ekle"
        }

        row.addView(logoView)
        row.addView(infoLayout)
        row.addView(favoriteView)
        return Holder(row, logoView, titleView, metaView, badgeView, favoriteView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val station = items[position]

        holder.logoView.contentDescription = station.name
        StationImageLoader.load(holder.logoView, station.logoUrl, R.drawable.ic_keyfe_keder_logo)
        holder.titleView.text = station.name
        holder.metaView.text = listOf(station.genre, station.country, station.quality)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { "Canlı radyo" }

        val favorite = isFavorite(station)
        holder.badgeView.visibility = if (favorite) View.VISIBLE else View.GONE
        holder.badgeView.text = if (favorite) "● FAVORİ" else ""
        holder.favoriteView.setImageResource(
            if (favorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        holder.favoriteView.setColorFilter(
            if (favorite) Color.rgb(255, 122, 0) else Color.rgb(185, 185, 190)
        )
        holder.favoriteView.alpha = if (favorite) 1f else .78f
        holder.favoriteView.contentDescription = if (favorite) "Favorilerden çıkar" else "Favorilere ekle"

        holder.itemView.setOnClickListener {
            holder.itemView.animate().scaleX(.985f).scaleY(.985f).setDuration(70)
                .withEndAction { holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
            onClick(station)
        }

        holder.favoriteView.setOnClickListener {
            holder.favoriteView.animate().rotationBy(18f).setDuration(80)
                .withEndAction { holder.favoriteView.animate().rotation(0f).setDuration(100).start() }.start()
            onFavorite(station)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = items.size

    class Holder(
        view: View,
        val logoView: ImageView,
        val titleView: TextView,
        val metaView: TextView,
        val badgeView: TextView,
        val favoriteView: ImageButton
    ) : RecyclerView.ViewHolder(view)
}

private fun Int.dp(context: android.content.Context) =
    (this * context.resources.displayMetrics.density).toInt()
