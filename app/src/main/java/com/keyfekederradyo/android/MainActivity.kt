package com.keyfekederradyo.android

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(13, 13, 14)
    private val surface = Color.rgb(27, 27, 29)
    private val surface2 = Color.rgb(35, 35, 38)
    private val orange = Color.rgb(255, 122, 0)
    private val white = Color.rgb(245, 245, 247)
    private val muted = Color.rgb(150, 150, 155)
    private val prefs by lazy { getSharedPreferences("radio", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private var stations = emptyList<Station>()
    private var currentIndex = -1
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var adapter: StationAdapter
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var search: EditText
    private lateinit var play: ImageButton
    private lateinit var spectrum: AudioSpectrumView
    private lateinit var mini: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        setContentView(buildUi())
        connectPlayer()
        loadStations()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            setBackgroundColor(bg)
        }
        val menu = button(android.R.drawable.ic_menu_sort_by_size)
        val brand = TextView(this).apply {
            text = "KEYFE KEDER\nR A D Y O"
            textSize = 18f
            setTextColor(white)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = .08f
            layoutParams = LinearLayout.LayoutParams(0, 58.dp(), 1f)
        }
        val searchButton = button(android.R.drawable.ic_menu_search)
        top.addView(menu); top.addView(brand); top.addView(searchButton)
        root.addView(top)

        val tabs = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp(), 0, 10.dp(), 0)
            background = rounded(surface, 18)
        }
        tabs.addView(tab("TÜMÜ", true) { adapter.submitList(stations) })
        tabs.addView(tab("FAVORİLER", false) { adapter.submitList(stations.filter { isFavorite(it) }) })
        tabs.addView(tab("KATEGORİLER", false) { showCategories() })
        root.addView(tabs, LinearLayout.LayoutParams(-1, 48.dp()).apply { setMargins(10.dp(), 0, 10.dp(), 10.dp()) })

        search = EditText(this).apply {
            hint = "Radyo ara..."
            setTextColor(white); setHintTextColor(muted); setSingleLine(true)
            setPadding(16.dp(), 0, 16.dp(), 0); background = rounded(surface, 18)
            visibility = View.GONE
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 50.dp()).apply { setMargins(10.dp(), 0, 10.dp(), 8.dp()) })
        searchButton.setOnClickListener {
            search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (search.visibility == View.VISIBLE) search.requestFocus()
        }

        val loader = ProgressBar(this).apply { tag = "loader" }
        root.addView(loader, LinearLayout.LayoutParams(-1, 40.dp()).apply { gravity = Gravity.CENTER })
        val list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, 0, 0, 6.dp())
            clipToPadding = false
        }
        adapter = StationAdapter({ play(it) }, { isFavorite(it) }, { toggleFavorite(it) })
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        mini = buildMiniPlayer()
        root.addView(mini, LinearLayout.LayoutParams(-1, 76.dp()).apply { setMargins(8.dp(), 4.dp(), 8.dp(), 6.dp()) })
        root.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, 68.dp()))
        return root
    }

    private fun buildMiniPlayer(): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp(), 8.dp(), 10.dp(), 8.dp())
            background = rounded(surface2, 24)
            elevation = 8.dp().toFloat()
            setOnClickListener { showFullPlayer() }
        }
        val logo = ImageView(this).apply {
            setImageResource(com.keyfekederradyo.android.R.drawable.ic_keyfe_keder_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(54.dp(), 54.dp()).apply { rightMargin = 10.dp() }
        }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        title = TextView(this).apply { text = "Bir radyo seç"; textSize = 15f; setTextColor(white); maxLines = 1 }
        status = TextView(this).apply { text = "Hazır"; textSize = 12f; setTextColor(muted) }
        spectrum = AudioSpectrumView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 18.dp()) }
        info.addView(title); info.addView(status); info.addView(spectrum)
        play = button(android.R.drawable.ic_media_play).apply { layoutParams = LinearLayout.LayoutParams(50.dp(), 50.dp()) }
        play.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
        row.addView(logo); row.addView(info); row.addView(play)
        return row
    }

    private fun buildBottomNav(): View {
        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(6.dp(), 5.dp(), 6.dp(), 5.dp())
            setBackgroundColor(surface)
        }
        val items = listOf("⌂\nAna Sayfa", "▣\nRadyolar", "✦\nKeşfet", "♡\nFavoriler", "⚙\nAyarlar")
        items.forEachIndexed { index, label ->
            val item = TextView(this).apply {
                text = label; textSize = if (index == 0) 12f else 11f
                gravity = Gravity.CENTER; setTextColor(if (index == 0) orange else muted)
                setPadding(4.dp(), 2.dp(), 4.dp(), 2.dp())
                layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                setOnClickListener { animateTap(this); handleNav(index) }
            }
            nav.addView(item)
        }
        return nav
    }

    private fun handleNav(index: Int) {
        when (index) {
            1 -> Toast.makeText(this, "Radyolar", Toast.LENGTH_SHORT).show()
            2 -> showCategories()
            3 -> adapter.submitList(stations.filter { isFavorite(it) })
            4 -> Toast.makeText(this, "Ayarlar yakında", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFullPlayer() {
        val dialog = android.app.Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp(), 20.dp(), 24.dp(), 22.dp())
            background = rounded(bg, 30)
        }
        val close = TextView(this).apply { text = "⌄"; textSize = 28f; setTextColor(muted); gravity = Gravity.CENTER; setOnClickListener { dialog.dismiss() } }
        root.addView(close, LinearLayout.LayoutParams(-1, 36.dp()))
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_keyfe_keder_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = rounded(Color.rgb(24,24,25), 120)
            elevation = 18.dp().toFloat()
        }
        root.addView(logo, LinearLayout.LayoutParams(220.dp(), 220.dp()).apply { setMargins(0, 18.dp(), 0, 22.dp()) })
        val name = TextView(this).apply { text = title.text; textSize = 25f; setTextColor(white); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD) }
        root.addView(name, LinearLayout.LayoutParams(-1, 40.dp()))
        val live = TextView(this).apply { text = "●  CANLI"; textSize = 12f; setTextColor(orange); gravity = Gravity.CENTER }
        root.addView(live, LinearLayout.LayoutParams(-1, 30.dp()))
        val bigSpectrum = AudioSpectrumView(this).apply { setPlaying(controller?.isPlaying == true) }
        root.addView(bigSpectrum, LinearLayout.LayoutParams(-1, 48.dp()).apply { setMargins(18.dp(), 18.dp(), 18.dp(), 18.dp()) })

        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val previous = button(android.R.drawable.ic_media_previous)
        val main = button(if (controller?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(orange) }
            setPadding(22.dp(), 22.dp(), 22.dp(), 22.dp())
            layoutParams = LinearLayout.LayoutParams(78.dp(), 78.dp()).apply { setMargins(20.dp(), 0, 20.dp(), 0) }
        }
        val next = button(android.R.drawable.ic_media_next)
        previous.setOnClickListener { playRelative(-1); name.text = title.text }
        next.setOnClickListener { playRelative(1); name.text = title.text }
        main.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() }; bigSpectrum.setPlaying(controller?.isPlaying == true) }
        controls.addView(previous); controls.addView(main); controls.addView(next)
        root.addView(controls)

        val favorite = TextView(this).apply {
            text = if (currentIndex >= 0 && isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle"
            textSize = 14f; setTextColor(white); gravity = Gravity.CENTER; setPadding(0, 20.dp(), 0, 0)
            setOnClickListener { if (currentIndex >= 0) { toggleFavorite(stations[currentIndex]); text = if (isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle" } }
        }
        root.addView(favorite, LinearLayout.LayoutParams(-1, 48.dp()))
        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(-1, -2)
        dialog.show()
        dialog.window?.setLayout(-1, -2)
    }

    private fun playRelative(delta: Int) {
        if (stations.isEmpty()) return
        currentIndex = if (currentIndex < 0) 0 else (currentIndex + delta + stations.size) % stations.size
        play(stations[currentIndex])
    }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        status.text = if (isPlaying) "Canlı" else "Durduruldu"
                        play.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                        spectrum.setPlaying(isPlaying)
                    }
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) { title.text = item?.mediaMetadata?.title ?: "Bir radyo seç" }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { status.text = "Yayın açılamadı"; spectrum.setPlaying(false) }
                })
            } catch (_: Exception) { status.text = "Oynatıcı başlatılamadı" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadStations() {
        executor.execute {
            try {
                val loaded = StationRepository().load()
                runOnUiThread {
                    stations = loaded
                    adapter.submitList(loaded)
                    findViewById<View>(android.R.id.content).findViewWithTag<View>("loader")?.visibility = View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Radyolar yüklenemedi"; Toast.makeText(this, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun play(s: Station) {
        currentIndex = stations.indexOfFirst { it.resolvedUrl == s.resolvedUrl }.coerceAtLeast(0)
        val item = MediaItem.Builder().setMediaId(s.resolvedUrl).setUri(s.resolvedUrl)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).build()).build()
        controller?.setMediaItem(item); controller?.prepare(); controller?.play()
        title.text = s.name; status.text = "Bağlanıyor..."
        animateTap(mini)
    }

    private fun filter(q: String) { val x = q.trim().lowercase(); adapter.submitList(if (x.isBlank()) stations else stations.filter { it.name.lowercase().contains(x) || it.genre.lowercase().contains(x) || it.country.lowercase().contains(x) }) }
    private fun showCategories() {
        val genres = stations.flatMap { it.genre.split(",", ";") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(12)
        Toast.makeText(this, if (genres.isEmpty()) "Kategori bulunamadı" else genres.joinToString(" • "), Toast.LENGTH_LONG).show()
    }
    private fun isFavorite(s: Station) = prefs.getBoolean(s.resolvedUrl, false)
    private fun toggleFavorite(s: Station) { prefs.edit().putBoolean(s.resolvedUrl, !isFavorite(s)).apply(); adapter.notifyDataSetChanged() }
    private fun tab(text: String, selected: Boolean, action: () -> Unit) = TextView(this).apply { this.text = text; textSize = 12f; setTextColor(if (selected) orange else muted); gravity = Gravity.CENTER; setOnClickListener { animateTap(this); action() }; layoutParams = LinearLayout.LayoutParams(0, -1, 1f) }
    private fun button(res: Int) = ImageButton(this).apply { setImageResource(res); setBackgroundColor(Color.TRANSPARENT); setColorFilter(white); layoutParams = LinearLayout.LayoutParams(52.dp(), 52.dp()) }
    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply { setColor(color); cornerRadius = radiusDp.dp().toFloat() }
    private fun animateTap(v: View) { v.animate().scaleX(.96f).scaleY(.96f).setDuration(70).withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(130).setInterpolator(DecelerateInterpolator()).start() }.start() }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    override fun onDestroy() { controllerFuture?.let { MediaController.releaseFuture(it) }; executor.shutdownNow(); super.onDestroy() }
}
