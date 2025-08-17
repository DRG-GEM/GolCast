package com.drgia.golcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.drgia.golcast.data.Episode
import com.drgia.golcast.data.Podcast
import kotlinx.coroutines.flow.StateFlow

// Nuevo objeto para mantener el estado actual de forma centralizada
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentEpisode: Episode? = null,
    val currentPodcast: Podcast? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

class PlayerViewModel : ViewModel() {
    // El ViewModel ahora expone el StateFlow del PlayerService directamente como LiveData
    val playerState: LiveData<PlayerState> = PlayerService.playerState.asLiveData()
}