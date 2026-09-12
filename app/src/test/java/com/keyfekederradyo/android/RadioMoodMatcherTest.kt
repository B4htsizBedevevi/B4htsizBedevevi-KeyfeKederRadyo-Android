package com.keyfekederradyo.android

import org.junit.Assert.assertTrue
import org.junit.Test

class RadioMoodMatcherTest {
    private fun station(name: String, genre: String) = Station(
        name = name,
        url = "https://example.com/$name",
        genre = genre,
        country = "Türkiye"
    )

    @Test
    fun calm_prefers_lounge_and_jazz() {
        val list = listOf(
            station("Pop Hit", "Pop"),
            station("Late Lounge", "Lounge"),
            station("Night Jazz", "Jazz")
        )
        val ranked = RadioMoodMatcher.rank(list, "Sakin")
        assertTrue(ranked.first().genre in setOf("Lounge", "Jazz"))
    }

    @Test
    fun energetic_finds_pop_and_rock() {
        val list = listOf(station("Soft", "Lounge"), station("Power Rock", "Rock"))
        assertTrue(RadioMoodMatcher.rank(list, "Enerjik").first().name == "Power Rock")
    }
}
