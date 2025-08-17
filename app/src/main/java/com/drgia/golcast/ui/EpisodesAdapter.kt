package com.drgia.golcast.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drgia.golcast.R
import com.drgia.golcast.data.Episode
import java.text.SimpleDateFormat
import java.util.Locale

class EpisodesAdapter(
    private var episodes: List<Episode>,
    private val onClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodesAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.episodeTitle)
        val date: TextView = view.findViewById(R.id.episodeDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episode = episodes[position]
        holder.title.text = episode.title
        holder.date.text = dateFormat.format(episode.pubDate)
        holder.itemView.setOnClickListener { onClick(episode) }
    }

    override fun getItemCount() = episodes.size
}