package com.drgia.golcast.model

data class Episode(
    val title: String,
    val audioUrl: String,
    val pubMillis: Long,
    val artworkUrl: String? = null
)
