package com.keyfekederradyo.android

import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

class StationRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val sourceUrl =
        "https://raw.githubusercontent.com/B4htsizBedevevi/KeyfeKederRadyo-Web/main/stations.json"

    fun load(): List<Station> {
        val request = Request.Builder().url(sourceUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return parse(response.body?.string().orEmpty())
        }
    }

    private fun parse(json: String): List<Station> {
        val array = JSONArray(json)
        val result = ArrayList<Station>(array.length())
        val seen = HashSet<String>()

        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }.trim()
            if (name.isBlank() || url.isBlank()) continue

            val key = url.lowercase(Locale.ROOT)
            if (!seen.add(key)) continue

            val homepage = o.optString("homepage").trim()
            val host = runCatching {
                Uri.parse(homepage).host.orEmpty().removePrefix("www.")
            }.getOrDefault("")
            val logoUrl = if (host.isNotBlank()) {
                "https://icons.duckduckgo.com/ip3/$host.ico"
            } else ""

            val rawGenre = o.optString("genre").trim()
            val fallbackText = buildString {
                append(name).append(' ')
                append(rawGenre).append(' ')
                append(o.optString("tags")).append(' ')
                append(o.optString("language"))
            }
            val genre = normalizeGenre(rawGenre, fallbackText)

            result += Station(
                name = name,
                url = o.optString("url"),
                resolvedUrl = url,
                genre = genre,
                language = o.optString("language").trim(),
                country = o.optString("country").trim(),
                quality = o.optString("quality").trim(),
                song = o.optString("song").ifBlank { "Canlı yayın" },
                homepage = homepage,
                logoUrl = logoUrl
            )
        }

        return result.sortedWith(
            compareBy<Station> { it.name.lowercase(Locale.ROOT) }
        )
    }

    private fun normalizeGenre(raw: String, source: String): String {
        if (raw.isNotBlank()) return raw.split(',').first().trim()
        val text = source.lowercase(Locale.ROOT)
        return when {
            "arabesk" in text || "fantazi" in text || "damar" in text -> "Arabesk"
            "rock" in text -> "Rock"
            "jazz" in text -> "Jazz"
            "classical" in text || "klasik" in text -> "Klasik"
            "lounge" in text || "chill" in text -> "Lounge"
            "dance" in text || "electro" in text || "electronic" in text -> "Elektronik"
            "pop" in text -> "Pop"
            "folk" in text || "türk halk" in text || "turku" in text -> "Türk Halk"
            "news" in text || "haber" in text -> "Haber"
            "oldies" in text || "80s" in text || "90s" in text -> "Nostalji"
            else -> "Radyo"
        }
    }
}
