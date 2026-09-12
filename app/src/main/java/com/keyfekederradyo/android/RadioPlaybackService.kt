package com.keyfekederradyo.android

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class RadioPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val timerHandler = Handler(Looper.getMainLooper())
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (!::player.isInitialized) return
            val item = player.currentMediaItem ?: return
            if (!player.playWhenReady) return
            val until = getSharedPreferences("radio", MODE_PRIVATE).getLong("sleep_until", 0L)
            if (until > 0L && until <= System.currentTimeMillis()) return
            player.prepare()
            player.play()
        }
    }
    private val timerRunnable = object : Runnable {
        override fun run() {
            val until = getSharedPreferences("radio", MODE_PRIVATE).getLong("sleep_until", 0L)
            if (until <= 0L) return
            val remaining = until - System.currentTimeMillis()
            if (remaining <= 0L) {
                reconnectHandler.removeCallbacks(reconnectRunnable)
                player.pause()
                player.clearMediaItems()
                PlaybackState.setPlaying(null)
                getSharedPreferences("radio", MODE_PRIVATE).edit().remove("sleep_until").apply()
                stopSelf()
            } else {
                timerHandler.postDelayed(this, minOf(remaining, 30_000L))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (player.isPlaying) PlaybackState.setPlaying(mediaItem?.mediaId)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlaybackState.setPlaying(if (isPlaying) player.currentMediaItem?.mediaId else null)
            }

            override fun onPlayerError(error: PlaybackException) {
                PlaybackState.setPlaying(null)
                reconnectHandler.removeCallbacks(reconnectRunnable)
                if (player.currentMediaItem != null && player.playWhenReady) {
                    reconnectHandler.postDelayed(reconnectRunnable, 4_000L)
                }
            }
        })

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        timerHandler.post(timerRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep the foreground media session alive while a station is playing.
        // The explicit exit choice in MainActivity is the user-controlled way to stop playback.
        if (!player.isPlaying && !player.playWhenReady) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(timerRunnable)
        reconnectHandler.removeCallbacks(reconnectRunnable)
        PlaybackState.setPlaying(null)
        if (::mediaSession.isInitialized) mediaSession.release()
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }
}
