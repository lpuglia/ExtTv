package com.android.exttv.model.manager

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.android.exttv.model.data.CardItem

object PlayerManager {
    var isLoading by mutableStateOf(true)
    var isProgressBarVisible by mutableStateOf(false)
    var isVisibleCardList by mutableStateOf(false)
    var currentCard: CardItem? by mutableStateOf(null)
    var cardList by mutableStateOf(listOf<CardItem>())

    lateinit var player : ExoPlayer;

    fun init(context: Context) {
        player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
        }
    }

    @OptIn(UnstableApi::class)
    fun setMediaSource(newMediaSource: MediaSource) {

        // Prepare the player with the new MediaSource
        player.setMediaSource(newMediaSource)

        // Optionally, seek to a specific position or the start
        player.seekToDefaultPosition() // Starts from beginning
        // or player.seekTo(specificPositionMs) // Start at specific time

        // Prepare and play
        player.prepare()
    }
}