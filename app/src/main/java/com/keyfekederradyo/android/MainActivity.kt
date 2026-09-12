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
        connectPlayer()
        loadStations()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(14.dp(), 8.dp(), 10.dp(), 8.dp()); setBackgroundColor(bg) }
        val menu = button(android.R.drawable.ic_menu_sort_by_size)
        val brandBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, 62.dp(), 1f) }
        val brand = TextView(this).apply { text = "KEYFE KEDER"; textSize = 18f; setTextColor(white); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD); letterSpacing = .08f }
        tagline = TextView(this).apply { text = taglines[0]; textSize = 10.5f; setTextColor(orange); gravity = Gravity.CENTER; alpha = .9f; letterSpacing = .03f }
        brandBox.addView(brand, LinearLayout.LayoutParams(-1, 31.dp())); brandBox.addView(tagline, LinearLayout.LayoutParams(-1, 22.dp()))
        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val searchButton = button(android.R.drawable.ic_menu_search)
        liveBadge = TextView(this).apply { text = "● CANLI"; textSize = 10f; setTextColor(orange); gravity = Gravity.CENTER; setPadding(8.dp(), 0, 8.dp(), 0); background = GradientDrawable().apply { setColor(Color.rgb(45, 27, 15)); setStroke(1.dp(), Color.rgb(110, 62, 20)); cornerRadius = 14.dp().toFloat() }; layoutParams = LinearLayout.LayoutParams(64.dp(), 30.dp()).apply { leftMargin = 2.dp() } }
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
        searchButton.setOnClickListener { search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE; if (search.visibility == View.VISIBLE) search.requestFocus() }

        val loader = ProgressBar(this).apply { tag = "loader" }
        root.addView(loader, LinearLayout.LayoutParams(-1, 40.dp()).apply { gravity = Gravity.CENTER })
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity); overScrollMode = View.OVER_SCROLL_NEVER; setPadding(0, 0, 0, 6.dp()); clipToPadding = false }
        adapter = StationAdapter({ play(it) }, { isFavorite(it) }, { toggleFavorite(it) }); list.adapter = adapter; root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        mini = buildMiniPlayer(); root.addView(mini, LinearLayout.LayoutParams(-1, 76.dp()).apply { setMargins(8.dp(), 4.dp(), 8.dp(), 6.dp()) }); root.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, 68.dp()))
        return root
    }

    private fun buildMiniPlayer(): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(12.dp(), 8.dp(), 10.dp(), 8.dp()); background = rounded(surface2, 24); elevation = 8.dp().toFloat(); setOnClickListener { showFullPlayer() } }
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_keyfe_keder_logo); scaleType = ImageView.ScaleType.CENTER_CROP; layoutParams = LinearLayout.LayoutParams(54.dp(), 54.dp()).apply { rightMargin = 10.dp() } }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        title = TextView(this).apply { text = "Bir radyo seç"; textSize = 15f; setTextColor(white); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        status = TextView(this).apply { text = "Hazır"; textSize = 12f; setTextColor(muted); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        spectrum = AudioSpectrumView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 18.dp()) }
        info.addView(title); info.addView(status); info.addView(spectrum)
        play = button(android.R.drawable.ic_media_play).apply { layoutParams = LinearLayout.LayoutParams(50.dp(), 50.dp()) }; play.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
        row.addView(logo); row.addView(info); row.addView(play); return row
    }

    private fun buildBottomNav(): View {
        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(6.dp(), 5.dp(), 6.dp(), 5.dp()); setBackgroundColor(surface) }
        val items = listOf("⌂\nAna Sayfa", "▣\nRadyolar", "✦\nKeşfet", "♡\nFavoriler", "⚙\nAyarlar")
        items.forEachIndexed { index, label -> val item = TextView(this).apply { text = label; textSize = if (index == 0) 12f else 11f; gravity = Gravity.CENTER; setTextColor(if (index == 0) orange else muted); setPadding(4.dp(), 2.dp(), 4.dp(), 2.dp()); layoutParams = LinearLayout.LayoutParams(0, -1, 1f); setOnClickListener { animateTap(this); handleNav(index) } }; nav.addView(item) }
        return nav
    }

    private fun handleNav(index: Int) { when (index) { 0, 1 -> adapter.submitList(stations); 2 -> showCategories(); 3 -> showFavorites(); 4 -> showSettings() } }

    private fun showFullPlayer() {
        val dialog = android.app.Dialog(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(24.dp(), 20.dp(), 24.dp(), 22.dp()); background = rounded(bg, 30) }
        val close = TextView(this).apply { text = "⌄"; textSize = 28f; setTextColor(muted); gravity = Gravity.CENTER; setOnClickListener { dialog.dismiss() } }; root.addView(close, LinearLayout.LayoutParams(-1, 36.dp()))
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_keyfe_keder_logo); scaleType = ImageView.ScaleType.CENTER_INSIDE; background = rounded(Color.rgb(24,24,25), 120); elevation = 18.dp().toFloat() }; root.addView(logo, LinearLayout.LayoutParams(190.dp(), 190.dp()).apply { setMargins(0, 12.dp(), 0, 18.dp()) })
        val name = TextView(this).apply { text = title.text; textSize = 25f; setTextColor(white); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD) }; root.addView(name, LinearLayout.LayoutParams(-1, 40.dp()))
        root.addView(TextView(this).apply { text = "●  CANLI"; textSize = 12f; setTextColor(orange); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, 30.dp()))
        val bigSpectrum = AudioSpectrumView(this).apply { setPlaying(controller?.isPlaying == true) }; root.addView(bigSpectrum, LinearLayout.LayoutParams(-1, 48.dp()).apply { setMargins(18.dp(), 18.dp(), 18.dp(), 18.dp()) })
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }; val previous = button(android.R.drawable.ic_media_previous); val main = button(if (controller?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(orange) }; setPadding(22.dp(), 22.dp(), 22.dp(), 22.dp()); layoutParams = LinearLayout.LayoutParams(78.dp(), 78.dp()).apply { setMargins(20.dp(), 0, 20.dp(), 0) } }; val next = button(android.R.drawable.ic_media_next)
        previous.setOnClickListener { playRelative(-1); name.text = title.text }; next.setOnClickListener { playRelative(1); name.text = title.text }; main.setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() }; main.setImageResource(if (controller?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play); bigSpectrum.setPlaying(controller?.isPlaying == true) }; controls.addView(previous); controls.addView(main); controls.addView(next); root.addView(controls)
        val favorite = TextView(this).apply { text = if (currentIndex >= 0 && isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle"; textSize = 14f; setTextColor(white); gravity = Gravity.CENTER; setPadding(0, 20.dp(), 0, 0); setOnClickListener { if (currentIndex >= 0) { toggleFavorite(stations[currentIndex]); text = if (isFavorite(stations[currentIndex])) "♥  Favorilerde" else "♡  Favorilere ekle" } } }; root.addView(favorite, LinearLayout.LayoutParams(-1, 48.dp()))
        dialog.setContentView(root); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1, -2)
    }

    private fun playRelative(delta: Int) { if (stations.isEmpty()) return; currentIndex = if (currentIndex < 0) 0 else (currentIndex + delta + stations.size) % stations.size; play(stations[currentIndex]) }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java)); controllerFuture = MediaController.Builder(this, token).buildAsync(); controllerFuture?.addListener({ try { controller = controllerFuture?.get(); controller?.addListener(object : androidx.media3.common.Player.Listener { override fun onIsPlayingChanged(isPlaying: Boolean) { status.text = if (isPlaying) "Canlı" else "Durduruldu"; play.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play); spectrum.setPlaying(isPlaying); liveBadge.text = if (isPlaying) "● CANLI" else "RADYO"; liveBadge.setTextColor(if (isPlaying) orange else muted) }; override fun onMediaItemTransition(item: MediaItem?, reason: Int) { title.text = item?.mediaMetadata?.title ?: "Bir radyo seç" }; override fun onPlayerError(error: androidx.media3.common.PlaybackException) { status.text = "Yayın açılamadı"; spectrum.setPlaying(false); liveBadge.text = "RADYO"; liveBadge.setTextColor(muted) } }) } catch (_: Exception) { status.text = "Oynatıcı başlatılamadı" } }, ContextCompat.getMainExecutor(this))
    }

    private fun loadStations() { executor.execute { try { val loaded = StationRepository().load(); runOnUiThread { stations = loaded; adapter.submitList(loaded); findViewById<View>(android.R.id.content).findViewWithTag<View>("loader")?.visibility = View.GONE } } catch (e: Exception) { runOnUiThread { status.text = "Radyolar yüklenemedi"; Toast.makeText(this, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show() } } } }
    private fun play(s: Station) { currentIndex = stations.indexOfFirst { it.resolvedUrl == s.resolvedUrl }.coerceAtLeast(0); val item = MediaItem.Builder().setMediaId(s.resolvedUrl).setUri(s.resolvedUrl).setMediaMetadata(MediaMetadata.Builder().setTitle(s.name).setArtist("Keyfe Keder Radyo").setAlbumTitle("Canlı Yayın").setArtworkUri(Uri.parse("android.resource://$packageName/drawable/ic_keyfe_keder_logo")).build()).build(); controller?.setMediaItem(item); controller?.prepare(); controller?.play(); title.text = s.name; status.text = "Bağlanıyor..."; liveBadge.text = "BAĞLANIYOR"; liveBadge.setTextColor(orange); animateTap(mini) }
    private fun filter(q: String) { val x = q.trim().lowercase(); adapter.submitList(if (x.isBlank()) stations else stations.filter { it.name.lowercase().contains(x) || it.genre.lowercase().contains(x) || it.country.lowercase().contains(x) }) }
    private fun showFavorites() { adapter.submitList(stations.filter { isFavorite(it) }) }

    private fun showCategories() {
        val genres = stations.flatMap { it.genre.split(",", ";") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val dialog = android.app.Dialog(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22.dp(), 20.dp(), 22.dp(), 20.dp()); background = rounded(Color.rgb(24,24,26), 28) }
        root.addView(TextView(this).apply { text = "Kategoriler"; textSize = 23f; setTextColor(white); setTypeface(typeface, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(-1, 42.dp()))
        genres.forEach { genre -> root.addView(TextView(this).apply { text = genre; textSize = 15f; setTextColor(white); gravity = Gravity.CENTER_VERTICAL; setPadding(16.dp(),0,16.dp(),0); background = rounded(surface2,18); setOnClickListener { adapter.submitList(stations.filter { it.genre.contains(genre, ignoreCase = true) }); dialog.dismiss() } }, LinearLayout.LayoutParams(-1,48.dp()).apply { bottomMargin=8.dp() }) }
        root.addView(TextView(this).apply { text="Tüm radyolar"; textSize=14f; setTextColor(orange); gravity=Gravity.CENTER; setOnClickListener { adapter.submitList(stations); dialog.dismiss() } }, LinearLayout.LayoutParams(-1,44.dp()))
        dialog.setContentView(root); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1,-2)
    }

    private fun showSettings() {
        val dialog = android.app.Dialog(this); val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22.dp(),20.dp(),22.dp(),20.dp()); background=rounded(Color.rgb(24,24,26),28) }
        root.addView(TextView(this).apply { text="Ayarlar"; textSize=23f; setTextColor(white); setTypeface(typeface,android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(-1,42.dp()))
        root.addView(TextView(this).apply { text="🌙  Uyku zamanlayıcısı"; textSize=15f; setTextColor(white); gravity=Gravity.CENTER_VERTICAL; setPadding(16.dp(),0,16.dp(),0); background=rounded(surface2,18); setOnClickListener { val options=arrayOf("Kapalı","15 dakika","30 dakika","45 dakika","60 dakika","90 dakika"); android.app.AlertDialog.Builder(this@MainActivity).setTitle("Uyku zamanlayıcısı").setItems(options) { d,which -> val mins=when(which){0->0L;1->15L;2->30L;3->45L;4->60L;else->90L}; prefs.edit().putLong("sleep_until",if(mins==0L)0L else System.currentTimeMillis()+mins*60_000L).apply(); d.dismiss() }.show() } }, LinearLayout.LayoutParams(-1,54.dp()).apply{topMargin=12.dp()})
        root.addView(TextView(this).apply { text="Kapat"; textSize=14f; setTextColor(orange); gravity=Gravity.CENTER; setOnClickListener{dialog.dismiss()} }, LinearLayout.LayoutParams(-1,44.dp()).apply{topMargin=8.dp()})
        dialog.setContentView(root); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.show(); dialog.window?.setLayout(-1,-2)
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