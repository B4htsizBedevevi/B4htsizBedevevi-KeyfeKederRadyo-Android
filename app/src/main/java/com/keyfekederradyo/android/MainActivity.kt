package com.keyfekederradyo.android

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.GridLayoutManager
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
    private val taglineHandler = Handler(Looper.getMainLooper())
    private var taglineIndex = 0
    private var stations = emptyList<Station>()
    private var currentIndex = -1
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var adapter: StationAdapter
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var tagline: TextView
    private lateinit var liveBadge: TextView
    private lateinit var search: EditText
    private lateinit var play: ImageButton
    private lateinit var spectrum: AudioSpectrumView
    private lateinit var mini: View
    private lateinit var miniLogo: ImageView

    private val taglines = listOf("Keyfinin Frekansı.", "Keyfin Neyi Çekerse.", "Ruh Haline Bir Frekans.", "Her Moda Bir Radyo.", "Bir Frekans, Bin Keyif.")

    private val taglineRunnable = object : Runnable {
        override fun run() {
            taglineIndex = (taglineIndex + 1) % taglines.size
            tagline.animate().alpha(0f).translationY(-4.dp().toFloat()).setDuration(180).withEndAction {
                tagline.text = taglines[taglineIndex]
                tagline.translationY = 4.dp().toFloat()
                tagline.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
            }.start()
            taglineHandler.postDelayed(this, 3600)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val ui = buildUi()
        ui.alpha = 0f
        ui.translationY = 10.dp().toFloat()
        setContentView(ui)
        ui.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(500).setInterpolator(DecelerateInterpolator()).start()
        taglineHandler.postDelayed(taglineRunnable, 1800)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (search.visibility == View.VISIBLE) {
                    search.visibility = View.GONE
                    search.clearFocus()
                } else finish()
            }
        })
        connectPlayer()
        loadStations()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(14.dp(), 8.dp(), 10.dp(), 8.dp()); setBackgroundColor(bg) }
        val menu = button(R.drawable.ic_menu)
        val brandBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, 62.dp(), 1f) }
        val brand = TextView(this).apply { text = "KEYFE KEDER"; textSize = 18f; setTextColor(white); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD); letterSpacing = .08f }
        tagline = TextView(this).apply { text = taglines[0]; textSize = 10.5f; setTextColor(orange); gravity = Gravity.CENTER; alpha = .9f; letterSpacing = .03f }
        brandBox.addView(brand, LinearLayout.LayoutParams(-1, 31.dp())); brandBox.addView(tagline, LinearLayout.LayoutParams(-1, 22.dp()))
        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val searchButton = button(R.drawable.ic_search)
        liveBadge = TextView(this).apply { text = "RADYO"; textSize = 10f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(8.dp(), 0, 8.dp(), 0); background = GradientDrawable().apply { setColor(Color.rgb(28, 24, 21)); setStroke(1.dp(), Color.rgb(80, 55, 30)); cornerRadius = 14.dp().toFloat() }; layoutParams = LinearLayout.LayoutParams(64.dp(), 30.dp()).apply { leftMargin = 2.dp() } }
        tools.addView(searchButton); tools.addView(liveBadge); top.addView(menu); top.addView(brandBox); top.addView(tools); root.addView(top)

        val tabs = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(10.dp(), 0, 10.dp(), 0); background = rounded(surface, 18) }
        tabs.addView(tab("TÜMÜ", true) { adapter.submitList(stations) })
        tabs.addView(tab("FAVORİLER", false) { showFavorites() })
        tabs.addView(tab("KATEGORİLER", false) { showCategories() })
        root.addView(tabs, LinearLayout.LayoutParams(-1, 48.dp()).apply { setMargins(10.dp(), 0, 10.dp(), 10.dp()) })

        search = EditText(this).apply {
            hint = "Radyo ara..."; setTextColor(white); setHintTextColor(muted); setSingleLine(true); setPadding(16.dp(), 0, 16.dp(), 0); background = rounded(surface, 18); visibility = View.GONE
            addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s?.toString().orEmpty()) }; override fun afterTextChanged(s: android.text.Editable?) = Unit })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 50.dp()).apply { setMargins(10.dp(), 0, 10.dp(), 8.dp()) })
        searchButton.setOnClickListener { animateTap(it); search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE; if (search.visibility == View.VISIBLE) search.requestFocus() }
        menu.setOnClickListener { animateTap(it); showMenu() }

        val loader = ProgressBar(this).apply { tag = "loader" }
        root.addView(loader, LinearLayout.LayoutParams(-1, 40.dp()).apply { gravity = Gravity.CENTER })
        val list = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(5.dp(), 0, 5.dp(), 8.dp())
            clipToPadding = false
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 220
                removeDuration = 180
                changeDuration = 180
                moveDuration = 220
            }
        }
        adapter = StationAdapter({ play(it) }, { isFavorite(it) }, { toggleFavorite(it) }); list.adapter = adapter; root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        mini = buildMiniPlayer(); root.addView(mini, LinearLayout.LayoutParams(-1, 76.dp()).apply { setMargins(8.dp(), 4.dp(), 8.dp(), 6.dp()) }); root.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, 68.dp()))
        return root
    }

    private fun buildMiniPlayer(): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(10.dp(), 7.dp(), 8.dp(), 7.dp()); background = rounded(surface2, 24); elevation = 8.dp().toFloat(); setOnClickListener { showFullPlayer() } }
        miniLogo = ImageView(this).apply { setImageResource(R.drawable.ic_keyfe_keder_logo); scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; background = rounded(Color.rgb(18,18,19), 16); layoutParams = LinearLayout.LayoutParams(50.dp(), 50.dp()).apply { rightMargin = 9.dp() } }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        title = TextView(this).apply { text = "Bir radyo seç"; textSize = 14f; setTextColor(white); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        status = TextView(this).apply { text = "Hazır"; textSize = 11f; setTextColor(muted); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        spectrum = AudioSpectrumView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 16.dp()) }
        info.addView(title); info.addView(status); info.addView(spectrum)
        val previous = button(R.drawable.ic_prev).apply { layoutParams = LinearLayout.LayoutParams(36.dp(), 44.dp()); setColorFilter(muted) }
        val center = button(R.drawable.ic_play).apply { layoutParams = LinearLayout.LayoutParams(44.dp(), 44.dp()) }
        val next = button(R.drawable.ic_next).apply { layoutParams = LinearLayout.LayoutParams(36.dp(), 44.dp()); setColorFilter(muted) }
        previous.setOnClickListener { animateTap(it); playRelative(-1) }
        center.setOnClickListener { animateTap(it); controller?.let { if (it.isPlaying) it.pause() else it.play() } }
        next.setOnClickListener { animateTap(it); playRelative(1) }
        play = center
        row.addView(miniLogo); row.addView(info); row.addView(previous); row.addView(center); row.addView(next)
        return row
    }

    private fun buildBottomNav(): View {
        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(6.dp(), 4.dp(), 6.dp(), 4.dp()); setBackgroundColor(surface) }
        val items = listOf(R.drawable.ic_home to "Ana Sayfa", R.drawable.ic_radio to "Radyolar", R.drawable.ic_explore to "Keşfet", R.drawable.ic_heart to "Favoriler", R.drawable.ic_settings to "Ayarlar")
        items.forEachIndexed { index, pair ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, -1, 1f); setOnClickListener { animateTap(this); handleNav(index) } }
            val icon = ImageView(this).apply { setImageResource(pair.first); setColorFilter(if (index == 0) orange else muted); layoutParams = LinearLayout.LayoutParams(20.dp(), 22.dp()) }
            val label = TextView(this).apply { text = pair.second; textSize = 10f; setTextColor(if (index == 0) orange else muted); gravity = Gravity.CENTER }
            item.addView(icon); item.addView(label); nav.addView(item)
        }
        return nav
    }

    private fun handleNav(index: Int) { when (index) { 0, 1 -> adapter.submitList(stations); 2 -> showCategories(); 3 -> showFavorites(); 4 -> showSettings() } }

    private fun showFullPlayer() {
        val dialog = android.app.Dialog(this)
        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(24.dp(), 12.dp(), 24.dp(), 16.dp()); background = rounded(bg, 30) }
        val close = TextView(this).apply { text = "⌄"; textSize = 28f; setTextColor(muted); gravity = Gravity.CENTER; contentDescription = "Kapat"; setOnClickListener { dialog.dismiss() } }
        root.addView(close, LinearLayout.LayoutParams(-1, 34.dp()))
        val logo = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(Color.rgb(24,24,25), 100); clipToOutline = true }
        if (currentIndex in stations.indices) StationImageLoader.load(logo, stations[currentIndex].logoUrl, R.drawable.ic_keyfe_keder_logo) else logo.setImageResource(R.drawable.ic_keyfe_keder_logo)
        root.addView(logo, LinearLayout.LayoutParams(170.dp(), 170.dp()).apply { setMargins(0, 2.dp(), 0, 10.dp()) })
        val name = TextView(this).apply { text = title.text; textSize = 24f; setTextColor(white); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
        root.addView(name, LinearLayout.LayoutParams(-1, 48.dp()))
        val track = TextView(this).apply { text = status.text; textSize = 13f; setTextColor(muted); gravity = Gravity.CENTER; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        root.addView(track, LinearLayout.LayoutParams(-1, 28.dp()))
        val live = TextView(this).apply { text = if (controller?.isPlaying == true) "●  CANLI" else "●  HAZIR"; textSize = 12f; setTextColor(orange); gravity = Gravity.CENTER }
        root.addView(live, LinearLayout.LayoutParams(-1, 28.dp()))
        val bigSpectrum = AudioSpectrumView(this).apply { setPlaying(controller?.isPlaying == true) }
        root.addView(bigSpectrum, LinearLayout.LayoutParams(-1, 40.dp()).apply { setMargins(18.dp(), 6.dp(), 18.dp(), 10.dp()) })
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val previous = button(R.drawable.ic_prev)
        val main = button(if (controller?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(orange) }; setPadding(20.dp(), 20.dp(), 20.dp(), 20.dp()); layoutParams = LinearLayout.LayoutParams(74.dp(), 74.dp()).apply { setMargins(18.dp(), 0, 18.dp(), 0) } }
        val next = button(R.drawable.ic_next)
        previous.setOnClickListener { playRelative(-1); name.text = title.text; track.text = status.text; live.text = if (controller?.isPlaying == true) "●  CANLI" else "●  HAZIR"; if (currentIndex in stations.indices) StationImageLoader.load(logo, stations[currentIndex].logoUrl, R.drawable.ic_keyfe_keder_logo) }
        next.setOnClickListener { playRelative(1); name.text = title.text; track.text = status.text; live.text = if (controller?.isPlaying == true) "●  CANLI" else "●  HAZIR"; if (currentIndex in stations.indices) StationImageLoader.load(logo, stations[currentIndex].logoUrl, R.drawable.ic_keyfe_keder_logo) }
        main.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() }; main.setImageResource(if (controller?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play); live.text = if (controller?.isPlaying == true) "●  CANLI" else "●  HAZIR"; bigSpectrum.setPlaying(controller?.isPlaying == true) }
        controls.addView(previous); controls.addView(main); controls.addView(next); root.addView(controls)
        root.addView(TextView(this).apply { text = if (currentIndex >= 0 && isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle"; textSize = 14f; setTextColor(white); gravity = Gravity.CENTER; setPadding(0, 12.dp(), 0, 0); setOnClickListener { if (currentIndex >= 0) { toggleFavorite(stations[currentIndex]); text = if (isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle" } } }, LinearLayout.LayoutParams(-1, 42.dp()))
        scroll.addView(root); dialog.setContentView(scroll); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1, (resources.displayMetrics.heightPixels * 0.88f).toInt())
    }

    private fun playRelative(delta: Int) { if (stations.isEmpty()) return; currentIndex = if (currentIndex < 0) 0 else (currentIndex + delta + stations.size) % stations.size; play(stations[currentIndex]) }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(object : androidx.media3.common.Player.Listener {
                    private fun refresh() {
                        val p = controller ?: return
                        val playing = p.isPlaying
                        when {
                            p.playbackState == androidx.media3.common.Player.STATE_BUFFERING -> status.text = "Bağlanıyor..."
                            playing -> status.text = "Canlı"
                            p.playbackState == androidx.media3.common.Player.STATE_READY -> status.text = "Durduruldu"
                        }
                        play.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
                        spectrum.setPlaying(playing)
                        liveBadge.text = when {
                            playing -> "● CANLI"
                            p.playbackState == androidx.media3.common.Player.STATE_BUFFERING -> "BAĞLANIYOR"
                            else -> "RADYO"
                        }
                        liveBadge.setTextColor(if (playing || p.playbackState == androidx.media3.common.Player.STATE_BUFFERING) orange else muted)
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
                    override fun onPlaybackStateChanged(playbackState: Int) = refresh()
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) { title.text = item?.mediaMetadata?.title ?: "Bir radyo seç"; refresh() }
                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        val artist = mediaMetadata.artist?.toString().orEmpty()
                        if (artist.isNotBlank() && artist != "Keyfe Keder Radyo" && artist != "Canlı Yayın") status.text = artist
                    }
                    override fun onMetadata(metadata: Metadata) {
                        for (i in 0 until metadata.length()) {
                            val icy = metadata.get(i) as? IcyInfo ?: continue
                            icy.title?.trim()?.takeIf { it.isNotBlank() }?.let { updateNowPlaying(it) }
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { status.text = "Yayın açılamadı"; spectrum.setPlaying(false); liveBadge.text = "RADYO"; liveBadge.setTextColor(muted) }
                })
                syncCurrentStation()
            } catch (_: Exception) { status.text = "Oynatıcı başlatılamadı" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun syncCurrentStation() {
        val p = controller ?: return
        val item = p.currentMediaItem ?: return
        val index = stations.indexOfFirst { it.resolvedUrl == item.mediaId }
        if (index >= 0) {
            currentIndex = index
            title.text = stations[index].name
            StationImageLoader.load(miniLogo, stations[index].logoUrl, R.drawable.ic_keyfe_keder_logo)
        } else {
            title.text = item.mediaMetadata.title ?: "Bir radyo seç"
        }
        play.setImageResource(if (p.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        spectrum.setPlaying(p.isPlaying)
        liveBadge.text = if (p.isPlaying) "● CANLI" else "RADYO"
        liveBadge.setTextColor(if (p.isPlaying) orange else muted)
    }

    private fun updateNowPlaying(raw: String) {
        val cleaned = raw.replace("\u0000", "").trim()
        if (cleaned.isBlank()) return
        val parts = cleaned.split(" - ", " – ", " — ", limit = 2)
        val label = if (parts.size == 2) parts[0].trim() + " • " + parts[1].trim() else cleaned
        status.text = label
        val current = controller?.currentMediaItem ?: return
        val index = controller?.currentMediaItemIndex ?: -1
        if (index >= 0) {
            val meta = MediaMetadata.Builder().setTitle(current.mediaMetadata.title ?: title.text).setArtist(label).setAlbumTitle("Keyfe Keder Radyo").setArtworkUri(Uri.parse("android.resource://$packageName/drawable/ic_keyfe_keder_logo")).build()
            controller?.replaceMediaItem(index, current.buildUpon().setMediaMetadata(meta).build())
        }
    }

    private fun loadStations() {
        executor.execute {
            try {
                val loaded = StationRepository().load()
                runOnUiThread {
                    stations = loaded
                    adapter.submitList(loaded)
                    syncCurrentStation()
                    findViewById<View>(android.R.id.content).findViewWithTag<View>("loader")?.visibility = View.GONE
                }
            } catch (_: Exception) {
                runOnUiThread { status.text = "Radyolar yüklenemedi"; findViewById<View>(android.R.id.content).findViewWithTag<View>("loader")?.visibility = View.GONE }
            }
        }
    }

    private fun play(s: Station) {
        currentIndex = stations.indexOfFirst { it.resolvedUrl == s.resolvedUrl }.coerceAtLeast(0)
        val item = MediaItem.Builder().setMediaId(s.resolvedUrl).setUri(s.resolvedUrl).setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).setArtist("Keyfe Keder Radyo").setAlbumTitle("Canlı Yayın").setArtworkUri(Uri.parse("android.resource://$packageName/drawable/ic_keyfe_keder_logo")).setExtras(android.os.Bundle().apply { putString("fallback_url", s.url) }).build()).build()
        controller?.setMediaItem(item); controller?.prepare(); controller?.play()
        title.text = s.name; status.text = "Bağlanıyor..."; liveBadge.text = "BAĞLANIYOR"; liveBadge.setTextColor(orange)
        StationImageLoader.load(miniLogo, s.logoUrl, R.drawable.ic_keyfe_keder_logo)
        animateTap(mini)
    }

    private fun filter(q: String) { val x = q.trim().lowercase(); adapter.submitList(if (x.isBlank()) stations else stations.filter { it.name.lowercase().contains(x) || it.genre.lowercase().contains(x) || it.country.lowercase().contains(x) || it.language.lowercase().contains(x) }) }
    private fun showFavorites() { adapter.submitList(stations.filter { isFavorite(it) }) }

    private fun showCategories() {
        val genres = stations.flatMap { it.genre.split(",", ";") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val dialog = android.app.Dialog(this)
        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22.dp(), 20.dp(), 22.dp(), 20.dp()); background = rounded(Color.rgb(24,24,26), 28) }
        root.addView(TextView(this).apply { text = "Kategoriler"; textSize = 23f; setTextColor(white); setTypeface(typeface, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(-1, 42.dp()))
        genres.forEach { genre -> root.addView(TextView(this).apply { text = genre; textSize = 15f; setTextColor(white); gravity = Gravity.CENTER_VERTICAL; setPadding(16.dp(),0,16.dp(),0); background = rounded(surface2,18); setOnClickListener { adapter.submitList(stations.filter { it.genre.contains(genre, ignoreCase = true) }); dialog.dismiss() } }, LinearLayout.LayoutParams(-1,48.dp()).apply { bottomMargin=8.dp() }) }
        root.addView(TextView(this).apply { text="Tüm radyolar"; textSize=14f; setTextColor(orange); gravity=Gravity.CENTER; setOnClickListener { adapter.submitList(stations); dialog.dismiss() } }, LinearLayout.LayoutParams(-1,44.dp()))
        scroll.addView(root); dialog.setContentView(scroll); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1, (resources.displayMetrics.heightPixels * 0.82f).toInt())
    }

    private fun showSettings() {
        val dialog = android.app.Dialog(this); val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22.dp(),20.dp(),22.dp(),20.dp()); background=rounded(Color.rgb(24,24,26),28) }
        root.addView(TextView(this).apply { text="Ayarlar"; textSize=23f; setTextColor(white); setTypeface(typeface,android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(-1,42.dp()))
        root.addView(TextView(this).apply { text="🌙  Uyku zamanlayıcısı"; textSize=15f; setTextColor(white); gravity=Gravity.CENTER_VERTICAL; setPadding(16.dp(),0,16.dp(),0); background=rounded(surface2,18); setOnClickListener { val options=arrayOf("Kapalı","15 dakika","30 dakika","45 dakika","60 dakika","90 dakika"); android.app.AlertDialog.Builder(this@MainActivity).setTitle("Uyku zamanlayıcısı").setItems(options) { d,which -> val mins=when(which){0->0L;1->15L;2->30L;3->45L;4->60L;else->90L}; prefs.edit().putLong("sleep_until",if(mins==0L)0L else System.currentTimeMillis()+mins*60_000L).apply(); d.dismiss() }.show() } }, LinearLayout.LayoutParams(-1,54.dp()).apply{topMargin=12.dp()})
        root.addView(TextView(this).apply { text="Kapat"; textSize=14f; setTextColor(orange); gravity=Gravity.CENTER; setOnClickListener{dialog.dismiss()} }, LinearLayout.LayoutParams(-1,44.dp()).apply{topMargin=8.dp()})
        dialog.setContentView(root); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1,-2)
    }

    private fun showMenu() {
        val dialog = android.app.Dialog(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp()); background = rounded(surface, 28) }
        root.addView(TextView(this).apply { text = "Keyfe Keder"; textSize = 22f; setTextColor(white); setTypeface(typeface, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(-1, 38.dp()))
        root.addView(TextView(this).apply { text = "Keyfinin Frekansı."; textSize = 12f; setTextColor(orange) }, LinearLayout.LayoutParams(-1, 28.dp()))
        fun option(icon: Int, label: String, action: () -> Unit) {
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(10.dp(), 0, 10.dp(), 0); background = rounded(surface2, 18); setOnClickListener { animateTap(this); action(); dialog.dismiss() } }
            row.addView(ImageView(this).apply { setImageResource(icon); setColorFilter(white); layoutParams = LinearLayout.LayoutParams(24.dp(), 24.dp()).apply { rightMargin = 12.dp() } })
            row.addView(TextView(this).apply { text = label; textSize = 15f; setTextColor(white); gravity = Gravity.CENTER_VERTICAL })
            root.addView(row, LinearLayout.LayoutParams(-1, 52.dp()).apply { bottomMargin = 8.dp() })
        }
        option(R.drawable.ic_home, "Ana Sayfa") { adapter.submitList(stations) }
        option(R.drawable.ic_radio, "Radyolar") { adapter.submitList(stations) }
        option(R.drawable.ic_heart, "Favoriler") { showFavorites() }
        option(R.drawable.ic_explore, "Kategoriler") { showCategories() }
        option(R.drawable.ic_settings, "Ayarlar") { showSettings() }
        dialog.setContentView(root); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show()
        val lp = dialog.window?.attributes; lp?.gravity = Gravity.START or Gravity.TOP; lp?.x = 8.dp(); lp?.y = 70.dp(); dialog.window?.attributes = lp; dialog.window?.setLayout(290.dp(), -2)
    }
    private fun isFavorite(s: Station) = prefs.getBoolean(s.resolvedUrl, false)
    private fun toggleFavorite(s: Station) { prefs.edit().putBoolean(s.resolvedUrl,!isFavorite(s)).apply(); adapter.notifyDataSetChanged() }
    private fun tab(text:String,selected:Boolean,action:()->Unit)=TextView(this).apply{this.text=text;textSize=12f;setTextColor(if(selected)orange else muted);gravity=Gravity.CENTER;setOnClickListener{animateTap(this);action()};layoutParams=LinearLayout.LayoutParams(0,-1,1f)}
    private fun button(res:Int)=ImageButton(this).apply{setImageResource(res);setBackgroundColor(Color.TRANSPARENT);setColorFilter(white);layoutParams=LinearLayout.LayoutParams(52.dp(),52.dp())}
    private fun rounded(color:Int,radiusDp:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radiusDp.dp().toFloat()}
    private fun animateTap(v:View){v.animate().scaleX(.96f).scaleY(.96f).setDuration(70).withEndAction{v.animate().scaleX(1f).scaleY(1f).setDuration(130).setInterpolator(DecelerateInterpolator()).start()}.start()}
    private fun Int.dp()=(this*resources.displayMetrics.density).toInt()
    override fun onDestroy(){taglineHandler.removeCallbacks(taglineRunnable);controllerFuture?.let{MediaController.releaseFuture(it)};executor.shutdownNow();super.onDestroy()}
}