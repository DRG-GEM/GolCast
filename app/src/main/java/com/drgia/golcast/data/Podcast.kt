package com.drgia.golcast.data

import java.io.Serializable // <-- Añade esta importación

data class Podcast(
    val name: String,
    val rssUrl: String,
    val artworkUrl: String
) : Serializable // <-- Añade esto