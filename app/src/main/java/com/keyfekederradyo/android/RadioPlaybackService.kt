package com.keyfekederradyo.android

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class RadioPlaybackService : MediaSessionService() {
    companion object {
        private const val EXIT_ACTION = "com.keyfekederradyo.android.action.EXIT"
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val exitCommand = SessionCommand(EXIT_ACTION)

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
            override fun onPlayerError(error: PlaybackException) {
                val item = player.currentMediaItem ?: return
                val uri = item.localConfiguration?.uri?.toString().orEmpty()
                val fallback = item.mediaMetadata.extras?.getString("fallback_url").orEmpty()
                if (fallback.isNotBlank() && fallback != uri) {
                    player.setMediaItem(item.buildUpon().setUri(fallback).build())
                    player.prepare()
                    player.play()
                }
            }
        })

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val base = if (controller.isTrusted) {
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                } else {
                    MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_SESSION_COMMANDS
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                    .setAvailableSessionCommands(base.buildUpon().add(exitCommand).build())
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == EXIT_ACTION) {
                    player.clearMediaItems()
                    player.stop()
                    stopSelf()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        val previous = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
        val playPause = CommandButton.Builder(CommandButton.ICON_PLAY)
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .build()
        val next = CommandButton.Builder(CommandButton.ICON_NEXT)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()
        val exit = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_exit)
            .setDisplayName("Çıkış")
            .setSessionCommand(exitCommand)
            .setSlots(intArrayOf(CommandButton.SLOT_OVERFLOW))
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .setMediaButtonPreferences(listOf(previous, playPause, next, exit))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
