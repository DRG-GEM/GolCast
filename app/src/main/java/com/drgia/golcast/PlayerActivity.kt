package com.drgia.golcast

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class PlayerActivity : AppCompatActivity() {

    private lateinit var playPauseButton: ImageButton
    private lateinit var rewindButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var titleText: TextView
    private lateinit var descText: TextView
    private lateinit var coverImage: ImageView

    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playPauseButton = findViewById(R.id.btnPlayPause)
        rewindButton = findViewById(R.id.btnRewind)
        forwardButton = findViewById(R.id.btnForward)
        closeButton = findViewById(R.id.btnClose)
        seekBar = findViewById(R.id.seekBar)
        titleText = findViewById(R.id.txtTitle)
        descText = findViewById(R.id.txtDesc)
        coverImage = findViewById(R.id.imgCover)

        val episodeTitle = intent.getStringExtra("title") ?: PlaybackStateHolder.lastTitle ?: "Reproduciendo"
        val episodeDesc = intent.getStringExtra("description") ?: PlaybackStateHolder.lastDescription ?: ""
        val episodeCover = intent.getStringExtra("coverUrl") ?: PlaybackStateHolder.lastArtUrl

        titleText.text = episodeTitle
        descText.text = episodeDesc
        Glide.with(this)
            .load(episodeCover)
            .placeholder(R.drawable.ic_music_note)
            .into(coverImage)

        playPauseButton.setOnClickListener {
            val act = Intent(this, PlayerService::class.java).apply {
                action = if (PlaybackStateHolder.isPlaying) PlayerService.ACTION_PAUSE else PlayerService.ACTION_RESUME
            }
            startService(act)
        }
        rewindButton.setOnClickListener {
            startService(Intent(this, PlayerService::class.java).setAction(PlayerService.ACTION_REWIND_15))
        }
        forwardButton.setOnClickListener {
            startService(Intent(this, PlayerService::class.java).setAction(PlayerService.ACTION_FORWARD_30))
        }
        closeButton.setOnClickListener {
            startService(Intent(this, PlayerService::class.java).setAction(PlayerService.ACTION_STOP))
            finish()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    startService(Intent(this@PlayerActivity, PlayerService::class.java).apply {
                        action = PlayerService.ACTION_SEEK_TO
                        putExtra("pos", progress.toLong())
                    })
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        ui.post(updateUiTick)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(updateUiTick)
    }

    private val updateUiTick = object : Runnable {
        override fun run() {
            val playing = PlaybackStateHolder.isPlaying
            playPauseButton.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow)
            seekBar.max = PlaybackStateHolder.duration.toInt().coerceAtLeast(0)
            seekBar.progress = PlaybackStateHolder.position.toInt().coerceAtLeast(0)
            ui.postDelayed(this, 500L)
        }
    }
}
