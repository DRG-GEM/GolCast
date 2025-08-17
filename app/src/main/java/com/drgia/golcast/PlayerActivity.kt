package com.drgia.golcast.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.drgia.golcast.PlayerService
import com.drgia.golcast.PlayerViewModel
import com.drgia.golcast.R
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val artworkImageView: ImageView = findViewById(R.id.artworkImageView)
        val episodeTitleTextView: TextView = findViewById(R.id.episodeTitleTextView)
        val podcastTitleTextView: TextView = findViewById(R.id.podcastTitleTextView)
        val seekBar: SeekBar = findViewById(R.id.seekBar)
        val currentTimeTextView: TextView = findViewById(R.id.currentTimeTextView)
        val durationTextView: TextView = findViewById(R.id.durationTextView)
        val rewindButton: ImageButton = findViewById(R.id.rewindButton)
        val playPauseButton: ImageButton = findViewById(R.id.playPauseButton)
        val forwardButton: ImageButton = findViewById(R.id.forwardButton)

        playerViewModel.playerState.observe(this) { state ->
            if (state.currentEpisode == null || state.currentPodcast == null) {
                // Si no hay nada reproduciendo, cerramos la actividad
                finish()
                return@observe
            }

            episodeTitleTextView.text = state.currentEpisode.title
            podcastTitleTextView.text = state.currentPodcast.name
            Glide.with(this).load(state.currentPodcast.artworkUrl).into(artworkImageView)

            playPauseButton.setImageResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)

            seekBar.max = state.durationMs.toInt()
            seekBar.progress = state.positionMs.toInt()
            durationTextView.text = formatTime(state.durationMs)
            currentTimeTextView.text = formatTime(state.positionMs)
        }

        playPauseButton.setOnClickListener {
            val intent = Intent(this, PlayerService::class.java).apply {
                action = if (playerViewModel.playerState.value?.isPlaying == true) PlayerService.ACTION_PAUSE else PlayerService.ACTION_RESUME
            }
            startService(intent)
        }

        rewindButton.setOnClickListener {
            val intent = Intent(this, PlayerService::class.java).setAction(PlayerService.ACTION_REWIND)
            startService(intent)
        }

        forwardButton.setOnClickListener {
            val intent = Intent(this, PlayerService::class.java).setAction(PlayerService.ACTION_FORWARD)
            startService(intent)
        }
    }

    private fun formatTime(millis: Long): String {
        return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }
}