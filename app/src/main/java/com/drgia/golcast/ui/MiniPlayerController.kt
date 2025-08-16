package com.drgia.golcast.ui

import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.drgia.golcast.PlayerActivity
import com.drgia.golcast.PlayerService
import com.drgia.golcast.R

class MiniPlayerController(
    private val root: View,
    private val context: Context
) {

    private var service: PlayerService? = null
    private val ui = Handler(Looper.getMainLooper())

    private val art: ImageView = root.findViewById(R.id.miniArt)
    private val title: TextView = root.findViewById(R.id.miniTitle)
    private val podcast: TextView = root.findViewById(R.id.miniSubtitle)
    private val playPause: ImageButton = root.findViewById(R.id.miniPlayPause)
    private val progress: ProgressBar = root.findViewById(R.id.miniProgress)

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as PlayerService.LocalBinder).getService()
            refresh()
            startTicks()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            stopTicks()
            root.visibility = View.GONE
        }
    }

    fun onStart() {
        // mostramos solo si hay algo (heurística: duración>0 una vez conectado)
        context.bindService(Intent(context, PlayerService::class.java), conn, Context.BIND_AUTO_CREATE)
        root.setOnClickListener {
            context.startActivity(Intent(context, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        playPause.setOnClickListener {
            service?.playPause()
            refresh()
        }
    }

    fun onStop() {
        stopTicks()
        try { context.unbindService(conn) } catch (_: Throwable) {}
    }

    private fun startTicks() {
        ui.post(object : Runnable {
            override fun run() {
                val s = service ?: return
                val dur = s.duration()
                val pos = s.position()
                if (dur > 0) {
                    root.visibility = View.VISIBLE
                    progress.max = 1000
                    val pct = ((pos.toDouble() / dur) * 1000).toInt().coerceIn(0,1000)
                    progress.progress = pct
                } else {
                    // si no hay duración, lo ocultamos (nada en cola)
                    root.visibility = View.GONE
                }
                updatePlayPause()
                ui.postDelayed(this, 500L)
            }
        })
    }

    private fun stopTicks() = ui.removeCallbacksAndMessages(null)

    private fun refresh() {
        val s = service ?: return
        title.text = s.title()
        podcast.text = s.podcast()
        val bmp = s.artwork()
        if (bmp != null) art.setImageBitmap(bmp)
        else Glide.with(root).load(R.mipmap.ic_launcher).into(art)
        updatePlayPause()
        root.visibility = if (s.duration() > 0 || s.isPlaying()) View.VISIBLE else View.GONE
    }

    private fun updatePlayPause() {
        val playing = service?.isPlaying() ?: false
        playPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }
}
