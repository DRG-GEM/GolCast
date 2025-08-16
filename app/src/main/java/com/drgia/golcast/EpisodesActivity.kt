package com.drgia.golcast

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drgia.golcast.data.RssParser
import com.drgia.golcast.model.Episode
import com.drgia.golcast.ui.MiniPlayerController
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class EpisodesActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()
    private lateinit var mini: MiniPlayerController

    private lateinit var rssUrl: String
    private lateinit var podcastName: String
    private var podcastArt: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episodes)

        rssUrl = intent.getStringExtra("rss") ?: ""
        podcastName = intent.getStringExtra("name") ?: "Podcast"
        podcastArt = intent.getStringExtra("art")

        findViewById<android.widget.TextView>(R.id.titleToolbar).text = podcastName
        com.bumptech.glide.Glide.with(this).load(podcastArt).into(findViewById(R.id.headerArt))

        val rv = findViewById<RecyclerView>(R.id.recyclerEpisodes)
        rv.layoutManager = LinearLayoutManager(this)

        mini = MiniPlayerController(findViewById(R.id.miniPlayerRoot), this)

        io.execute {
            try {
                val episodes = RssParser.loadEpisodes(rssUrl)
                runOnUiThread {
                    rv.adapter = EpisodesAdapter(episodes) { ep ->
                        startService(
                            Intent(this, PlayerService::class.java).apply {
                                action = PlayerService.ACTION_PLAY
                                putExtra(PlayerService.EXTRA_URL, ep.audioUrl)
                                putExtra(PlayerService.EXTRA_TITLE, ep.title)
                                putExtra(PlayerService.EXTRA_PODCAST, podcastName)
                                putExtra(PlayerService.EXTRA_ART, ep.artworkUrl ?: podcastArt)
                            }
                        )
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }
                    findViewById<android.view.View>(R.id.loading).visibility = android.view.View.GONE
                    findViewById<android.view.View>(R.id.content).visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error cargando RSS: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onStart() { super.onStart(); mini.onStart() }
    override fun onStop() { super.onStop(); mini.onStop() }
}

private class EpisodesAdapter(
    private val items: List<Episode>,
    private val onClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
        return EpisodeVH(v)
    }

    override fun onBindViewHolder(holder: EpisodeVH, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount() = items.size
}

private class EpisodeVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
    private val title = v.findViewById<android.widget.TextView>(R.id.epTitle)
    private val date = v.findViewById<android.widget.TextView>(R.id.epDate)
    private val art = v.findViewById<android.widget.ImageView>(R.id.epArt)

    fun bind(ep: Episode, onClick: (Episode) -> Unit) {
        title.text = ep.title
        if (ep.pubMillis > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            date.text = sdf.format(Date(ep.pubMillis))
        } else date.text = ""
        com.bumptech.glide.Glide.with(itemView).load(ep.artworkUrl).into(art)
        itemView.setOnClickListener { onClick(ep) }
    }
}
