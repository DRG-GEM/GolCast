// Archivo: MiniPlayerController.kt
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
import com.drgia.golcast.PlaybackStateHolder // <-- Importante
import com.drgia.golcast.R

class MiniPlayerController(
    private val root: View,
    private val context: Context
) {

    // El servicio solo lo necesitamos para enviar acciones, no para obtener datos
    private var service: PlayerService? = null
    private val ui = Handler(Looper.getMainLooper())

    private val art: ImageView = root.findViewById(R.id.miniArt)
    private val title: TextView = root.findViewById(R.id.miniTitle)
    private val podcast: TextView = root.findViewById(R.id.miniSubtitle)
    private val playPause: ImageButton = root.findViewById(R.id.miniPlayPause)
    private val progress: ProgressBar = root.findViewById(R.id.miniProgress)

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // Guardamos la referencia al servicio para poder enviar acciones
            service = (binder as PlayerService.LocalBinder).getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun onStart() {
        context.bindService(Intent(context, PlayerService::class.java), conn, Context.BIND_AUTO_CREATE)
        root.setOnClickListener {
            context.startActivity(Intent(context, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        playPause.setOnClickListener {
            // Usamos el servicio para enviar la acción de reproducir/pausar
            val intent = Intent(context, PlayerService::class.java).apply {
                action = if (PlaybackStateHolder.isPlaying) PlayerService.ACTION_PAUSE else PlayerService.ACTION_RESUME
            }
            context.startService(intent)
        }
        // Empezamos a refrescar la UI inmediatamente
        startTicks()
    }

    fun onStop() {
        stopTicks()
        try { context.unbindService(conn) } catch (_: Throwable) {}
    }

    private fun startTicks() {
        ui.post(object : Runnable {
            override fun run() {
                // Leemos los datos directamente del PlaybackStateHolder
                val dur = PlaybackStateHolder.duration
                val pos = PlaybackStateHolder.position

                if (dur > 0) {
                    root.visibility = View.VISIBLE
                    title.text = PlaybackStateHolder.lastTitle
                    // El subtítulo podría ser el nombre del podcast, que no está en el holder.
                    // Podemos dejarlo como estaba o añadirlo al holder si es necesario.
                    podcast.text = PlaybackStateHolder.lastTitle // O un valor por defecto

                    Glide.with(root)
                        .load(PlaybackStateHolder.lastArtUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .into(art)

                    progress.max = 1000
                    val pct = ((pos.toDouble() / dur) * 1000).toInt().coerceIn(0,1000)
                    progress.progress = pct
                } else {
                    root.visibility = View.GONE
                }
                updatePlayPause()
                ui.postDelayed(this, 500L) // Refrescamos cada medio segundo
            }
        })
    }

    private fun stopTicks() = ui.removeCallbacksAndMessages(null)

    // El método refresh() ya no es necesario, startTicks() hace todo el trabajo.

    private fun updatePlayPause() {
        // Leemos si está reproduciendo desde el PlaybackStateHolder
        val playing = PlaybackStateHolder.isPlaying
        playPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }
}