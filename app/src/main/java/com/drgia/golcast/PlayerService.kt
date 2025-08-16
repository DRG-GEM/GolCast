package com.drgia.golcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlin.math.max
import kotlin.math.min
import java.util.concurrent.Executors

class PlayerService : Service() {

    companion object {
        const val ACTION_PLAY = "com.drgia.golcast.action.PLAY"
        const val ACTION_PAUSE = "com.drgia.golcast.action.PAUSE"
        const val ACTION_RESUME = "com.drgia.golcast.action.RESUME"
        const val ACTION_STOP = "com.drgia.golcast.action.STOP"
        const val ACTION_REWIND_15 = "com.drgia.golcast.action.REWIND_15"
        const val ACTION_FORWARD_30 = "com.drgia.golcast.action.FORWARD_30"
        const val ACTION_SEEK_TO = "com.drgia.golcast.action.SEEK_TO"

        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PODCAST = "podcast"
        const val EXTRA_ART = "art"

        private const val CHANNEL_ID = "golcast_playback"
        private const val NOTIF_ID = 1001
    }

    inner class LocalBinder : Binder() { fun getService(): PlayerService = this@PlayerService }
    private val binder = LocalBinder()

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    private val ui = Handler(Looper.getMainLooper())
    private var isForeground = false
    private var largeIcon: Bitmap? = null
    private val artworkExec = Executors.newSingleThreadExecutor()

    // Estado actual para reenviar a PlayerActivity
    private var currentTitle: String = "Reproduciendo"
    private var currentPodcast: String = "GolCast"
    private var currentArtUrl: String? = null
    private var currentDescription: String? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW
                    ).apply { setShowBadge(false) }
                )
            }
        }

        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(30_000)
            .build()

        mediaSession = MediaSessionCompat(this, "GolCastSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { player.play(); updateAndNotify() }
                override fun onPause() { player.pause(); updateAndNotify() }
                override fun onSeekTo(pos: Long) { player.seekTo(pos); updateAndNotify() }
                override fun onSkipToPrevious() {
                    val p = max(0, player.currentPosition - 15_000)
                    player.seekTo(p); updateAndNotify()
                }
                override fun onSkipToNext() {
                    val p = min(player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE,
                        player.currentPosition + 30_000)
                    player.seekTo(p); updateAndNotify()
                }
                override fun onStop() { stopAll() }
            })
        }

        player.addListener(object : com.google.android.exoplayer2.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateAndNotify() }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Episodio"
                val podcast = intent.getStringExtra(EXTRA_PODCAST) ?: "GolCast"
                val artUrl = intent.getStringExtra(EXTRA_ART)
                val desc = intent.getStringExtra("description")

                currentTitle = title
                currentPodcast = podcast
                currentArtUrl = artUrl
                currentDescription = desc

                PlaybackStateHolder.lastTitle = title
                PlaybackStateHolder.lastDescription = desc ?: ""
                PlaybackStateHolder.lastArtUrl = artUrl ?: ""

                artworkExec.execute {
                    largeIcon = loadBitmap(artUrl)
                    runOnUi { startPlayback(url, title, podcast) }
                }
            }
            ACTION_PAUSE -> mediaSession.controller.transportControls.pause()
            ACTION_RESUME -> mediaSession.controller.transportControls.play()
            ACTION_REWIND_15 -> mediaSession.controller.transportControls.skipToPrevious()
            ACTION_FORWARD_30 -> mediaSession.controller.transportControls.skipToNext()
            ACTION_SEEK_TO -> {
                val pos = intent.getLongExtra("pos", player.currentPosition)
                mediaSession.controller.transportControls.seekTo(pos)
            }
            ACTION_STOP -> mediaSession.controller.transportControls.stop()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(url: String, title: String, podcast: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()

        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, podcast)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, podcast)
            .apply { largeIcon?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
            .build()
        mediaSession.setMetadata(meta)
        updateAndNotify()
    }

    private fun buildState(): PlaybackStateCompat {
        val isPlaying = player.isPlaying
        val actions = (PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        return PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                player.currentPosition.coerceAtLeast(0L),
                if (isPlaying) 1f else 0f,
                SystemClock.elapsedRealtime()
            ).build()
    }

    private fun updateAndNotify() {
        mediaSession.setPlaybackState(buildState())
        showNotif()
        ui.removeCallbacks(progressTick)
        if (player.isPlaying) ui.postDelayed(progressTick, 1000L)

        // Publicar estado para UI
        PlaybackStateHolder.isPlaying = player.isPlaying
        PlaybackStateHolder.duration  = player.duration.takeIf { it > 0 } ?: 0L
        PlaybackStateHolder.position  = player.currentPosition
    }

    private val progressTick = object : Runnable {
        override fun run() {
            if (player.isPlaying) {
                mediaSession.setPlaybackState(buildState())
                showNotif()
                PlaybackStateHolder.isPlaying = true
                PlaybackStateHolder.duration  = player.duration.takeIf { it > 0 } ?: 0L
                PlaybackStateHolder.position  = player.currentPosition
                ui.postDelayed(this, 1000L)
            }
        }
    }

    private fun showNotif() {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, PlayerActivity::class.java).apply {
                putExtra("title", currentTitle)
                putExtra("description", currentDescription ?: "")
                putExtra("coverUrl", currentArtUrl ?: "")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val actPrev = PendingIntent.getService(this, 1,
            Intent(this, PlayerService::class.java).setAction(ACTION_REWIND_15),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        val actToggle = PendingIntent.getService(this, 2,
            Intent(this, PlayerService::class.java)
                .setAction(if (player.isPlaying) ACTION_PAUSE else ACTION_RESUME),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        val actNext = PendingIntent.getService(this, 3,
            Intent(this, PlayerService::class.java).setAction(ACTION_FORWARD_30),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        val actStop = PendingIntent.getService(this, 4,
            Intent(this, PlayerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(currentPodcast)
            .setContentIntent(contentIntent)
            .setOngoing(player.isPlaying)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(actStop)
            )
            .addAction(R.drawable.ic_replay_15, "15s", actPrev)
            .addAction(
                if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (player.isPlaying) "Pausar" else "Reproducir",
                actToggle
            )
            .addAction(R.drawable.ic_forward_30, "30s", actNext)
            .addAction(R.drawable.ic_close, "Cerrar", actStop)

        largeIcon?.let { b.setLargeIcon(it) }

        val dur = player.duration.takeIf { it > 0 } ?: 0L
        if (dur > 0) b.setProgress(dur.toInt(), player.currentPosition.toInt(), false)
        else b.setProgress(0, 0, true)

        val notif = b.build()
        if (!isForeground) { startForeground(NOTIF_ID, notif); isForeground = true }
        else (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, notif)
    }

    private fun stopAll() {
        ui.removeCallbacks(progressTick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        player.stop(); player.clearMediaItems()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        ui.removeCallbacks(progressTick)
        mediaSession.isActive = false
        mediaSession.release()
        player.release()
        artworkExec.shutdownNow()
    }

    override fun onBind(intent: Intent?) = binder

    private fun loadBitmap(url: String?): Bitmap? =
        try { if (url.isNullOrBlank()) null
        else Glide.with(this).asBitmap().load(url).submit(512, 512).get() } catch (_: Throwable) { null }

    private fun runOnUi(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else Handler(Looper.getMainLooper()).post { block() }
    }
    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
}

/** Estado global simple para la UI */
object PlaybackStateHolder {
    @Volatile var isPlaying: Boolean = false
    @Volatile var duration: Long = 0L
    @Volatile var position: Long = 0L

    @Volatile var lastTitle: String? = null
    @Volatile var lastDescription: String? = null
    @Volatile var lastArtUrl: String? = null
}
