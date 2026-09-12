package com.keyfekederradyo.android

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
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
    private val bg = Color.rgb(18, 18, 18)
    private val orange = Color.rgb(255, 122, 0)
    private val prefs by lazy { getSharedPreferences("radio", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private var stations = emptyList<Station>()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var adapter: StationAdapter
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var search: EditText
    private lateinit var play: ImageButton

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
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp); setBackgroundColor(Color.rgb(31,31,31)) }
        val menu = button(android.R.drawable.ic_menu_sort_by_size)
        val brand = TextView(this).apply { text = "📡  KEYFE KEDER RADYO"; textSize = 18f; setTextColor(orange); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, 52.dp(), 1f) }
        val searchButton = button(android.R.drawable.ic_menu_search)
        bar.addView(menu); bar.addView(brand); bar.addView(searchButton); root.addView(bar)

        val tabs = LinearLayout(this).apply { gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(35,35,35)) }
        tabs.addView(tab("TÜM RADYOLAR") { adapter.submitList(stations) })
        tabs.addView(tab("FAVORİLER") { adapter.submitList(stations.filter { isFavorite(it) }) })
        root.addView(tabs, LinearLayout.LayoutParams(-1, 52.dp()))

        search = EditText(this).apply { hint = "Radyo ara..."; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine(); visibility = View.GONE }
        root.addView(search, LinearLayout.LayoutParams(-1, 50.dp()).apply { setMargins(10.dp(), 8.dp(), 10.dp(), 4.dp()) })
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        searchButton.setOnClickListener { search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE }

        val progress = ProgressBar(this)
        root.addView(progress, LinearLayout.LayoutParams(-1, 44.dp()).apply { gravity = Gravity.CENTER })
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity); overScrollMode = View.OVER_SCROLL_NEVER }
        adapter = StationAdapter({ play(it) }, { isFavorite(it) }, { toggleFavorite(it) })
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        progress.tag = "loader"

        val mini = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(14.dp(), 8.dp(), 8.dp(), 8.dp()); setBackgroundColor(Color.rgb(31,31,31)) }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        title = TextView(this).apply { text = "Bir radyo seç"; textSize = 16f; setTextColor(Color.WHITE); maxLines = 1 }
        status = TextView(this).apply { text = "Hazır"; textSize = 12f; setTextColor(Color.GRAY) }
        info.addView(title); info.addView(status); mini.addView(info)
        play = button(android.R.drawable.ic_media_play); play.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() } }; mini.addView(play)
        root.addView(mini, LinearLayout.LayoutParams(-1, 76.dp()))
        return root
    }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { status.text = if (isPlaying) "Canlı yayın" else "Durduruldu"; play.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play) }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) { title.text = item?.mediaMetadata?.title ?: "Bir radyo seç" }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) { status.text = "Yayın açılamadı" }
            })
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadStations() {
        executor.execute {
            try {
                val loaded = StationRepository().load()
                runOnUiThread { stations = loaded; adapter.submitList(loaded); findViewById<View>(android.R.id.content).findViewWithTag<View>("loader")?.visibility = View.GONE }
            } catch (e: Exception) { runOnUiThread { status.text = "Radyolar yüklenemedi"; Toast.makeText(this, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show() } }
        }
    }

    private fun play(s: Station) {
        val item = MediaItem.Builder().setMediaId(s.resolvedUrl).setUri(s.resolvedUrl).setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).setArtist(s.genre.ifBlank { "Canlı radyo" }).setDescription(s.song).build()).build()
        controller?.setMediaItem(item); controller?.prepare(); controller?.play(); title.text = s.name; status.text = "Bağlanıyor..."
    }

    private fun filter(q: String) { val x = q.trim().lowercase(); adapter.submitList(if (x.isBlank()) stations else stations.filter { it.name.lowercase().contains(x) || it.genre.lowercase().contains(x) || it.country.lowercase().contains(x) }) }
    private fun isFavorite(s: Station) = prefs.getBoolean(s.resolvedUrl, false)
    private fun toggleFavorite(s: Station) { prefs.edit().putBoolean(s.resolvedUrl, !isFavorite(s)).apply(); adapter.notifyDataSetChanged() }
    private fun tab(text: String, action: () -> Unit) = TextView(this).apply { this.text = text; textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(0, -1, 1f) }
    private fun button(res: Int) = ImageButton(this).apply { setImageResource(res); setBackgroundColor(Color.TRANSPARENT); setColorFilter(Color.WHITE); layoutParams = LinearLayout.LayoutParams(52.dp(), 52.dp()) }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    override fun onDestroy() { controllerFuture?.let { MediaController.releaseFuture(it) }; executor.shutdownNow(); super.onDestroy() }
}
