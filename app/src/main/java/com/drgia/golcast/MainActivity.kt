package com.drgia.golcast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: si lo niega, igual se puede reproducir pero sin mostrar notif */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 13+: pedir permiso de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Botón: reproducir un episodio real de tu lista (COPE "Tiempo de Juego")
        findViewById<Button>(R.id.btnPlayTest).setOnClickListener {
            val ep = Episode(
                audioUrl = "https://rss.megaphone.fm/redirect/COPE8262292209.mp3", // ejemplo estable
                title = "Tiempo de Juego (prueba)",
                podcast = "Tiempo de Juego",
                artUrl = "https://imagenes.cope.es/uploads/2025/01/20/original_678e8defc69bc.jpeg",
                description = "Episodio de prueba desde MainActivity"
            )
            playEpisode(ep)
        }

        // Abrir la pantalla completa (sin reiniciar reproducción)
        findViewById<Button>(R.id.btnOpenNowPlaying).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra("title", PlaybackStateHolder.lastTitle ?: "Reproduciendo")
                putExtra("description", PlaybackStateHolder.lastDescription ?: "")
                putExtra("coverUrl", PlaybackStateHolder.lastArtUrl ?: "")
            })
        }
    }

    private fun playEpisode(ep: Episode) {
        // Guardar como último, para la pantalla completa
        PlaybackStateHolder.lastTitle = ep.title
        PlaybackStateHolder.lastDescription = ep.description ?: ""
        PlaybackStateHolder.lastArtUrl = ep.artUrl ?: ""

        val i = Intent(this, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_URL, ep.audioUrl)
            putExtra(PlayerService.EXTRA_TITLE, ep.title)
            putExtra(PlayerService.EXTRA_PODCAST, ep.podcast)
            putExtra(PlayerService.EXTRA_ART, ep.artUrl)
            putExtra("description", ep.description ?: "")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
    }
}

// Modelo simple para pruebas
data class Episode(
    val audioUrl: String,
    val title: String,
    val podcast: String,
    val artUrl: String?,
    val description: String? = null
)
