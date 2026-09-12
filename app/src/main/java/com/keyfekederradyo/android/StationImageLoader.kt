package com.keyfekederradyo.android

import android.graphics.BitmapFactory
import android.widget.ImageView
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object StationImageLoader {
    private val cache = object : LruCache<String, android.graphics.Bitmap>(24) {}
    private val executor = Executors.newFixedThreadPool(3)

    fun load(imageView: ImageView, url: String, fallbackRes: Int) {
        imageView.setImageResource(fallbackRes)
        if (url.isBlank()) return

        synchronized(cache) {
            cache.get(url)?.let {
                imageView.setImageBitmap(it)
                return
            }
        }

        imageView.tag = url
        executor.execute {
            val bitmap = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 4500
                connection.readTimeout = 6500
                connection.instanceFollowRedirects = true
                connection.useCaches = true
                connection.connect()
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
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
