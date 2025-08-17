package com.drgia.golcast.data

import java.io.Serializable // <-- Añade esta importación
import java.util.Date

data class Episode(
    val title: String,
    val audioUrl: String,
    val pubDate: Date
) : Serializable // <-- Añade esto