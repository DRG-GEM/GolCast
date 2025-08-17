package com.drgia.golcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.bumptech.glide.Glide
import com.drgia.golcast.data.Episode
import com.drgia.golcast.data.Podcast
import com.drgia.golcast.ui.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.Executors

class PlayerService : Service() {

    companion object {
        // --- StateFlow para comunicar con la UI ---
        private val _playerState = MutableStateFlow(PlayerState())
        val playerState = _playerState.asStateFlow()

        // Acciones
        const val ACTION_PLAY = "action.PLAY"
        const val ACTION_PAUSE = "action.PAUSE"
        const val ACTION_RESUME = "action.RESUME"
        const val ACTION_STOP = "action.STOP"
        const val ACTION_REWIND = "action.REWIND"
        const val ACTION_FORWARD = "action.FORWARD"

        // Extras
        const val EXTRA_EPISODE = "EXTRA_EPISODE"
        const val EXTRA_PODCAST = "EXTRA_PODCAST"
    }

    private val binder = LocalBinder()
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private val artworkExec = Executors.newSingleThreadExecutor()
    private var largeIcon: Bitmap? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSessionCompat(this, "PlayerServiceMediaSession")
        mediaSession.isActive = true

        setupMediaSessionCallback()
        setupPlayerListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val episode: Episode? = intent.getSerializableExtra(EXTRA_EPISODE) as? Episode
                val podcast: Podcast? = intent.getSerializableExtra(EXTRA_PODCAST) as? Podcast
                if (episode != null && podcast != null) {
                    _playerState.value = PlayerState(currentEpisode = episode, currentPodcast = podcast)
                    startPlayback(episode, podcast)
                }
            }
            ACTION_PAUSE -> mediaSession.controller.transportControls.pause()
            ACTION_RESUME -> mediaSession.controller.transportControls.play()
            ACTION_REWIND -> mediaSession.controller.transportControls.rewind()
            ACTION_FORWARD -> mediaSession.controller.transportControls.fastForward()
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(episode: Episode, podcast: Podcast) {
        artworkExec.execute {
            largeIcon = try {
                Glide.with(this).asBitmap().load(podcast.artworkUrl).submit().get()
            } catch (e: Exception) { null }

            runOnUiThread {
                exoPlayer.setMediaItem(MediaItem.fromUri(episode.audioUrl))
                exoPlayer.prepare()
                exoPlayer.play()
                updateMetadata()
            }
        }
    }

    private fun updateMetadata() {
        val state = _playerState.value
        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state.currentEpisode?.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, state.currentPodcast?.name)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
            .build()
        mediaSession.setMetadata(meta)
    }

    private fun updatePlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                exoPlayer.currentPosition,
                1.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())

        // Actualizamos nuestro StateFlow para la UI
        _playerState.value = _playerState.value.copy(
            isPlaying = exoPlayer.isPlaying,
            positionMs = exoPlayer.currentPosition,
            durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0
        )

        updateNotification()
    }

    private fun updateNotification() {
        if (_playerState.value.currentEpisode == null) return
        startForeground(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = _playerState.value
        val isPlaying = state.isPlaying
        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val rewindIntent = PendingIntent.getService(this, 1, Intent(this, PlayerService::class.java).setAction(ACTION_REWIND), PendingIntent.FLAG_IMMUTABLE)
        val playPauseIntent = PendingIntent.getService(this, 2, Intent(this, PlayerService::class.java).setAction(if (isPlaying) ACTION_PAUSE else ACTION_RESUME), PendingIntent.FLAG_IMMUTABLE)
        val forwardIntent = PendingIntent.getService(this, 3, Intent(this, PlayerService::class.java).setAction(ACTION_FORWARD), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, 4, Intent(this, PlayerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "playback_channel")
            .setContentTitle(state.currentEpisode?.title)
            .setContentText(state.currentPodcast?.name)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_replay_15, "Rewind", rewindIntent)
            .addAction(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow, if(isPlaying) "Pause" else "Play", playPauseIntent)
            .addAction(R.drawable.ic_forward_30, "Forward", forwardIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
            .build()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopSelf()
                }
                updatePlaybackState()
            }
        })
    }

    private fun setupMediaSessionCallback() {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { exoPlayer.play() }
            override fun onPause() { exoPlayer.pause() }
            override fun onRewind() { exoPlayer.seekTo(exoPlayer.currentPosition - 15000) }
            override fun onFastForward() { exoPlayer.seekTo(exoPlayer.currentPosition + 30000) }
            override fun onStop() { stopSelf() }
        })
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = serviceScope.launch {
            while (true) {
                updatePlaybackState()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun runOnUiThread(block: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch { block() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("playback_channel", "Playback", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        exoPlayer.release()
        stopProgressUpdates()
        _playerState.value = PlayerState() // Reseteamos el estado
    }
}