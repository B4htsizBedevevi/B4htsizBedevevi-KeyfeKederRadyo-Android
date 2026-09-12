    private fun showSettings() {
        val dialog = android.app.Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(18), d(20), d(16))
            background = GradientDrawable().apply {
                setColor(Color.rgb(20,20,22))
                cornerRadius = d(28).toFloat()
                setStroke(d(1), Color.rgb(50,50,54))
            }
        }
        root.addView(TextView(this).apply {
            text = "Ayarlar"
            textSize = 25f
            setTextColor(white)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1, d(38)))

        root.addView(TextView(this).apply {
            text = "OYNATMA"
            textSize = 10f
            setTextColor(orange)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(d(4), d(16), d(4), d(7))
        })

        root.addView(settingCard("🌙", "Uyku zamanlayıcısı", sleepTimerLabel()) {
            val options = arrayOf("Kapalı", "15 dakika", "30 dakika", "45 dakika", "60 dakika", "90 dakika")
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Uyku zamanlayıcısı")
                .setItems(options) { _, which ->
                    val mins = when (which) { 0 -> 0L; 1 -> 15L; 2 -> 30L; 3 -> 45L; 4 -> 60L; else -> 90L }
                    prefs.edit().putLong("sleep_until", if (mins == 0L) 0L else System.currentTimeMillis() + mins * 60_000L).apply()
                }.show()
        })

        root.addView(settingCard("📡", "Arka planda çalma", "Uygulamayı kapatsan da yayın devam eder") { 
            android.widget.Toast.makeText(this, "Arka plan çalma aktif", android.widget.Toast.LENGTH_SHORT).show()
        })

        root.addView(TextView(this).apply {
            text = "GÖRÜNÜM"
            textSize = 10f
            setTextColor(orange)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(d(4), d(16), d(4), d(7))
        })

        root.addView(settingCard("✨", "Animasyonlar", "Akıcı geçişler ve spectrum efektleri", null))
        root.addView(settingCard("🎨", "Turuncu tema", "Keyfe Keder imza rengi", null))

        root.addView(TextView(this).apply {
            text = "Keyfe Keder Radyo  •  Android"
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, d(16), 0, d(2))
        })
        root.addView(TextView(this).apply {
            text = "Bir Frekans, Bin Keyif."
            textSize = 11f
            setTextColor(orange)
            gravity = Gravity.CENTER
        })

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(-1, -2)
    }

    private fun settingCard(icon: String, name: String, detail: String, action: (() -> Unit)?): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(14), d(10), d(12), d(10))
            background = rounded(surface2, 18)
            isClickable = action != null
            if (action != null) setOnClickListener { tap(this); action.invoke() }
        }
        val iconView = TextView(this).apply {
            text = icon
            textSize = 20f
            gravity = Gravity.CENTER
        }
        card.addView(iconView, LinearLayout.LayoutParams(d(42), d(50)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        info.addView(TextView(this).apply {
            text = name; textSize = 14f; setTextColor(white); setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        info.addView(TextView(this).apply {
            text = detail; textSize = 10f; setTextColor(muted); maxLines = 1
        })
        card.addView(info)
        if (action != null) card.addView(TextView(this).apply { text = "›"; textSize = 24f; setTextColor(muted) }, LinearLayout.LayoutParams(d(24), d(50)))
        return card
    }

    private fun sleepTimerLabel(): String {
        val until = prefs.getLong("sleep_until", 0L)
        if (until <= System.currentTimeMillis()) return "Kapalı"
        val mins = ((until - System.currentTimeMillis()) / 60000L).coerceAtLeast(1L)
        return "Yaklaşık $mins dk kaldı"
    }

    private fun showFullPlayer() {
        val dialog = android.app.Dialog(this)
        dialog.window?.setDimAmount(0.72f)
        val current = stations.getOrNull(currentIndex)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(d(22), d(10), d(22), d(18))
            background = GradientDrawable().apply {
                setColor(bg)
                cornerRadii = floatArrayOf(d(30).toFloat(), d(30).toFloat(), d(30).toFloat(), d(30).toFloat(), 0f, 0f, 0f, 0f)
                setStroke(d(1), Color.rgb(48, 48, 51))
            }
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, d(48))
        }
        val close = TextView(this).apply {
            text = "⌄"
            textSize = 30f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setOnClickListener { tap(this); dialog.dismiss() }
        }
        top.addView(close, LinearLayout.LayoutParams(d(54), d(48)))
        val now = TextView(this).apply {
            text = "ŞİMDİ ÇALIYOR"
            textSize = 10f
            setTextColor(orange)
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(43, 28, 20), 16)
            setPadding(d(12), 0, d(12), 0)
        }
        top.addView(now, LinearLayout.LayoutParams(-2, d(30)).apply { gravity = Gravity.CENTER })
        val spacer = View(this)
        top.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        val favorite = ImageButton(this).apply {
            setImageResource(R.drawable.ic_heart)
            setColorFilter(if (current != null && isFavorite(current)) orange else white)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Favoriye ekle"
            setOnClickListener {
                current?.let {
                    tap(this)
                    toggleFavorite(it)
                    setColorFilter(if (isFavorite(it)) orange else white)
                }
            }
        }
        top.addView(favorite, LinearLayout.LayoutParams(d(48), d(48)))
        root.addView(top)

        val logoFrame = FrameLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(d(226), d(226)).apply { topMargin = d(8); bottomMargin = d(18) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(22, 22, 24))
                setStroke(d(2), Color.rgb(77, 43, 24))
            }
        }
        val logo = StationArtworkView(this).apply {
            clipToOutline = true
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(24,24,25)) }
            contentDescription = current?.name ?: "Radyo"
            bind(current?.name ?: "RADYO", current?.genre ?: "")
        }
        logoFrame.addView(logo, FrameLayout.LayoutParams(d(210), d(210)).apply { gravity = Gravity.CENTER })
        root.addView(logoFrame)

        val stationName = TextView(this).apply {
            text = current?.name ?: title.text
            textSize = 25f
            setTextColor(white)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(stationName, LinearLayout.LayoutParams(-1, d(64)))

        val live = TextView(this).apply {
            text = if (controller?.isPlaying == true) "●  CANLI YAYIN" else "YAYIN HAZIR"
            textSize = 11f
            setTextColor(if (controller?.isPlaying == true) orange else muted)
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }
        root.addView(live, LinearLayout.LayoutParams(-1, d(28)))

        val track = TextView(this).apply {
            text = status.text.ifBlank { "Canlı yayın" }
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            isSelected = true
            background = rounded(surface, 20)
            setPadding(d(18), 0, d(18), 0)
        }
        root.addView(track, LinearLayout.LayoutParams(-1, d(42)).apply { topMargin = d(4) })

        val bigSpectrum = AudioSpectrumView(this).apply {
            setPlaying(controller?.isPlaying == true)
        }
        root.addView(bigSpectrum, LinearLayout.LayoutParams(-1, d(46)).apply { topMargin = d(8); bottomMargin = d(8) })

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, d(4), 0, 0)
        }
        val previous = iconButton(R.drawable.ic_prev).apply { layoutParams = LinearLayout.LayoutParams(d(60), d(60)) }
        val main = ImageButton(this).apply {
            setImageResource(if (controller?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
            setColorFilter(white)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(orange)
            }
            elevation = d(6).toFloat()
            contentDescription = "Oynat / duraklat"
            layoutParams = LinearLayout.LayoutParams(d(82), d(82)).apply { setMargins(d(20), 0, d(20), 0) }
            setOnClickListener {
                tap(this)
                togglePlay()
                postDelayed({
                    setImageResource(if (controller?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
                    bigSpectrum.setPlaying(controller?.isPlaying == true)
                    live.text = if (controller?.isPlaying == true) "●  CANLI YAYIN" else "YAYIN HAZIR"
                    live.setTextColor(if (controller?.isPlaying == true) orange else muted)
                }, 120)
            }
        }
        val next = iconButton(R.drawable.ic_next).apply { layoutParams = LinearLayout.LayoutParams(d(60), d(60)) }
        previous.setOnClickListener { tap(it); playRelative(-1); dialog.dismiss(); handler.postDelayed({ showFullPlayer() }, 180) }
        next.setOnClickListener { tap(it); playRelative(1); dialog.dismiss(); handler.postDelayed({ showFullPlayer() }, 180) }
        controls.addView(previous)
        controls.addView(main)
        controls.addView(next)
        root.addView(controls, LinearLayout.LayoutParams(-1, d(92)))

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.setLayout(-1, (resources.displayMetrics.heightPixels * 0.90f).toInt())
    }

    private fun iconButton(resId: Int) = ImageButton(this).apply {
        setImageResource(resId)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(white)
        layoutParams = LinearLayout.LayoutParams(d(52), d(52))
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = d(radiusDp).toFloat()
    }

    private fun tap(view: View) {
        view.animate().scaleX(.96f).scaleY(.96f).setDuration(70).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
        }.start()
    }

    private fun d(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(taglineRunnable)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        executor.shutdownNow()
        super.onDestroy()
    }
}
