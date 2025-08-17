package com.drgia.golcast.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drgia.golcast.PlayerActivity // <-- Importación añadida
import com.drgia.golcast.PlayerService
import com.drgia.golcast.R
import com.drgia.golcast.data.Podcast
import com.drgia.golcast.data.PodcastRepository
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.launch

class EpisodesActivity : AppCompatActivity() {
    private val repository = PodcastRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episodes)

        val podcast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("PODCAST_EXTRA", Podcast::class.java)
        } else {
            intent.getSerializableExtra("PODCAST_EXTRA") as? Podcast
        }

        if (podcast == null) {
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsingToolbar)
        collapsingToolbar.title = podcast.name

        val cover: ImageView = findViewById(R.id.podcastCover)
        Glide.with(this).load(podcast.artworkUrl).into(cover)

        val recyclerView: RecyclerView = findViewById(R.id.episodesRecycler)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val episodes = repository.fetchEpisodes(podcast.rssUrl)
            progressBar.visibility = View.GONE
            recyclerView.adapter = EpisodesAdapter(episodes) { episode ->
                val serviceIntent = Intent(this@EpisodesActivity, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_PLAY
                    putExtra(PlayerService.EXTRA_EPISODE, episode)
                    putExtra(PlayerService.EXTRA_PODCAST, podcast)
                }
                startService(serviceIntent)
                startActivity(Intent(this@EpisodesActivity, PlayerActivity::class.java))
            }
        }
    }
}