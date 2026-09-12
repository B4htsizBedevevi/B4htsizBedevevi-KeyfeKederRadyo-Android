package com.keyfekederradyo.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object StationImageLoader {
    private val cache = object : LruCache<String, Bitmap>(24) {}
    private val executor = Executors.newFixedThreadPool(3)

    fun load(imageView: ImageView, url: String, fallbackRes: Int) {
        imageView.tag = url.takeIf { it.isNotBlank() }
        imageView.setImageResource(fallbackRes)
        if (url.isBlank()) return

        synchronized(cache) {
            cache.get(url)?.let {
                imageView.setImageBitmap(it)
                return
            }
        }

        executor.execute {
            val bitmap = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 4500
                    connection.readTimeout = 6500
                    connection.instanceFollowRedirects = true
                    connection.useCaches = true
                    connection.setRequestProperty("User-Agent", "KeyfeKederRadyo/1.0")
                    connection.connect()
                    if (connection.responseCode !in 200..299) null
                    else connection.inputStream.use { BitmapFactory.decodeStream(it) }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()

            if (bitmap != null) synchronized(cache) { cache.put(url, bitmap) }

            imageView.post {
                if (imageView.tag == url && bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
}
