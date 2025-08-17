package com.drgia.golcast

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drgia.golcast.data.PodcastRepository
import com.drgia.golcast.ui.EpisodesActivity
import com.drgia.golcast.ui.PlayerActivity // <-- Importación añadida
import com.drgia.golcast.ui.PodcastAdapter

class MainActivity : AppCompatActivity() {

    private val repository = PodcastRepository()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.podcastsRecycler)
        val podcastList = repository.getPodcasts()

        val adapter = PodcastAdapter(podcastList) { podcast ->
            val intent = Intent(this, EpisodesActivity::class.java).apply {
                putExtra("PODCAST_EXTRA", podcast)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        setupMiniPlayer()
    }

    private fun setupMiniPlayer() {
        val miniPlayer: View = findViewById(R.id.miniPlayer)
        val miniCover: ImageView = findViewById(R.id.miniCover)
        val miniTitle: TextView = findViewById(R.id.miniTitle)
        val miniPlayPause: ImageButton = findViewById(R.id.playPause)

        playerViewModel.playerState.observe(this) { state ->
            if (state.currentEpisode != null) {
                miniPlayer.visibility = View.VISIBLE
                miniTitle.text = state.currentEpisode.title
                Glide.with(this).load(state.currentPodcast?.artworkUrl).into(miniCover)
                miniPlayPause.setImageResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
            } else {
                miniPlayer.visibility = View.GONE
            }
        }

        miniPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        miniPlayPause.setOnClickListener {
            val intent = Intent(this, PlayerService::class.java).apply {
                action = if (playerViewModel.playerState.value?.isPlaying == true) PlayerService.ACTION_PAUSE else PlayerService.ACTION_RESUME
            }
            startService(intent)
        }
    }
}