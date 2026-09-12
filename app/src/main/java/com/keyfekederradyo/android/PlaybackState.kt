package com.keyfekederradyo.android

object PlaybackState {
    private val listeners = mutableSetOf<(String?) -> Unit>()
    var playingUrl: String? = null
        private set

    @Synchronized
    fun setPlaying(url: String?) {
        if (playingUrl == url) return
        playingUrl = url
        listeners.toList().forEach { it(url) }
    }

    @Synchronized
    fun addListener(listener: (String?) -> Unit) {
        listeners += listener
        listener(playingUrl)
    }

    @Synchronized
    fun removeListener(listener: (String?) -> Unit) {
        listeners -= listener
    }
}
