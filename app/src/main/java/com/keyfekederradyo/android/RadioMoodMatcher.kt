package com.keyfekederradyo.android

import java.util.Locale

object RadioMoodMatcher {
    private val words = mapOf(
        "Sakin" to listOf("chill", "lounge", "jazz", "classical", "klasik", "easy", "soft", "ambient", "acoustic", "relax"),
        "Dertli" to listOf("arabesk", "fantazi", "damar", "slow", "fantezi", "nostalji", "turku", "türkü"),
        "Enerjik" to listOf("pop", "hit", "dance", "electro", "electronic", "disco", "house", "club", "rock"),
        "Yoldayım" to listOf("road", "drive", "car", "80", "90", "2000", "pop", "rock", "hit"),
        "Kafamı dinliyorum" to listOf("chill", "lounge", "jazz", "ambient", "acoustic", "classical", "klasik", "soft")
    )

    fun score(station: Station, mood: String): Int {
        val haystack = listOf(station.name, station.genre, station.song, station.language, station.country)
            .joinToString(" ").lowercase(Locale.ROOT)
        return words[mood].orEmpty().sumOf { token ->
            if (token.length <= 3) {
                if (Regex("\\b${Regex.escape(token)}\\b").containsMatchIn(haystack)) 3 else 0
            } else if (token in haystack) 2 else 0
        }
    }

    fun rank(stations: List<Station>, mood: String): List<Station> = stations
        .mapIndexed { index, station -> Triple(station, score(station, mood), index) }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Triple<Station, Int, Int>> { it.second }.thenBy { it.third })
        .map { it.first }
}
