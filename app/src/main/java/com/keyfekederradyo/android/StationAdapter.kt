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
    private var playingUrl: String? = null

    fun submitList(stations: List<Station>) {
        items.clear()
        items.addAll(stations)
        notifyDataSetChanged()
    }

    fun setPlayingStation(url: String?) {
        if (playingUrl == url) return
        playingUrl = url
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(10.dp(context), 8.dp(context), 10.dp(context), 8.dp(context))
            layoutParams = RecyclerView.LayoutParams(-1, 202.dp(context)).apply {
                leftMargin = 5.dp(context)
                rightMargin = 5.dp(context)
                bottomMargin = 10.dp(context)
            }
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 27, 29))
                cornerRadius = 22.dp(context).toFloat()
                setStroke(1.dp(context), Color.rgb(48, 48, 51))
            }
            isClickable = true
            isFocusable = true
            elevation = 2.dp(context).toFloat()
        }

        val top = LinearLayout(context).apply {
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(-1, 30.dp(context))
        }
        val badgeView = TextView(context).apply {
            textSize = 8.5f
            setTextColor(Color.rgb(255, 122, 0))
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }
        val favoriteView = ImageButton(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(5.dp(context), 3.dp(context), 3.dp(context), 3.dp(context))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(38.dp(context), 38.dp(context))
            contentDescription = "Favorilere ekle"
        }
        top.addView(badgeView)
        top.addView(favoriteView)

        val logoView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(48, 48, 52))
                setStroke(1.dp(context), Color.rgb(75, 75, 80))
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(84.dp(context), 84.dp(context)).apply {
                bottomMargin = 6.dp(context)
            }
        }

        val titleView = TextView(context).apply {
            textSize = 14.5f
            setTextColor(Color.rgb(245, 245, 247))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, 24.dp(context))
        }
        val metaView = TextView(context).apply {
            textSize = 9.5f
            setTextColor(Color.rgb(145, 145, 150))
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(-1, 19.dp(context))
        }
        val liveView = TextView(context).apply {
            textSize = 8.5f
            setTextColor(Color.rgb(255, 122, 0))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(-1, 18.dp(context))
        }

        card.addView(top)
        card.addView(logoView)
        card.addView(titleView)
        card.addView(metaView)
        card.addView(liveView)
        return Holder(card, logoView, titleView, metaView, badgeView, favoriteView, liveView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val station = items[position]
        val active = station.resolvedUrl == playingUrl

        holder.logoView.contentDescription = station.name
        StationImageLoader.load(holder.logoView, station.logoUrl, R.drawable.ic_keyfe_keder_logo)
        holder.titleView.text = station.name
        holder.metaView.text = listOf(station.genre, station.country, station.language)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" • ")
            .ifBlank { "Canlı radyo" }

        val favorite = isFavorite(station)
        holder.badgeView.visibility = if (favorite && !active) View.VISIBLE else View.GONE
        holder.badgeView.text = if (favorite) "● FAVORİ" else ""
        holder.liveView.visibility = if (active) View.VISIBLE else View.GONE
        holder.liveView.text = if (active) "●  CANLI YAYIN" else ""

        holder.favoriteView.setImageResource(R.drawable.ic_heart)
        holder.favoriteView.setColorFilter(
            if (favorite) Color.rgb(255, 122, 0) else Color.rgb(185, 185, 190)
        )
        holder.favoriteView.alpha = if (favorite) 1f else .78f
        holder.favoriteView.contentDescription = if (favorite) "Favorilerden çıkar" else "Favorilere ekle"

        val cardBackground = holder.itemView.background as GradientDrawable
        cardBackground.setColor(if (active) Color.rgb(31, 27, 24) else Color.rgb(27, 27, 29))
        cardBackground.setStroke(
            1.dp(holder.itemView.context),
            if (active) Color.rgb(255, 122, 0) else Color.rgb(48, 48, 51)
        )
        holder.itemView.elevation = if (active) 8.dp(holder.itemView.context).toFloat() else 2.dp(holder.itemView.context).toFloat()

        val logoBackground = holder.logoView.background as GradientDrawable
        logoBackground.setStroke(
            if (active) 2.dp(holder.itemView.context) else 1.dp(holder.itemView.context),
            if (active) Color.rgb(255, 122, 0) else Color.rgb(75, 75, 80)
        )

        holder.itemView.setOnClickListener {
            holder.itemView.animate()
                .scaleX(.94f).scaleY(.94f).setDuration(75)
                .withEndAction {
                    holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
                    onClick(station)
                }.start()
        }

        holder.favoriteView.setOnClickListener {
            holder.favoriteView.animate().scaleX(.72f).scaleY(.72f).setDuration(80)
                .withEndAction {
                    holder.favoriteView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
            onFavorite(station)
        }

        holder.itemView.animate().cancel()
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 12.dp(holder.itemView.context).toFloat()
        holder.itemView.scaleX = .97f
        holder.itemView.scaleY = .97f
        holder.itemView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setStartDelay((position.coerceAtMost(7) * 35L))
            .setDuration(280)
            .start()
    }

    override fun onViewRecycled(holder: Holder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = items.size

    class Holder(
        view: View,
        val logoView: ImageView,
        val titleView: TextView,
        val metaView: TextView,
        val badgeView: TextView,
        val favoriteView: ImageButton,
        val liveView: TextView
    ) : RecyclerView.ViewHolder(view)
}

private fun Int.dp(context: android.content.Context) =
    (this * context.resources.displayMetrics.density).toInt()
