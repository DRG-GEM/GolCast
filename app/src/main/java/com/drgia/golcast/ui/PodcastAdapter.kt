package com.drgia.golcast.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drgia.golcast.R
import com.drgia.golcast.data.Podcast

class PodcastAdapter(
    private val items: List<Podcast>,
    private val onClick: (Podcast) -> Unit
) : RecyclerView.Adapter<PodcastAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.podcastCover)
        val name: TextView = v.findViewById(R.id.podcastName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_podcast, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.name.text = p.name
        Glide.with(h.itemView).load(p.artworkUrl).placeholder(R.drawable.ic_music_note).into(h.cover)
        h.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size
}