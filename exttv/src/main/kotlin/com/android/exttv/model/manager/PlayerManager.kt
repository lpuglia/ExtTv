package com.android.exttv.model.manager

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.android.exttv.model.data.CardItem

object PlayerManager {
    var isLoading by mutableStateOf(true)
    var playerState by mutableIntStateOf(ExoPlayer.STATE_IDLE)
    var isProgressBarVisible by mutableStateOf(false)
    var isVisibleCardList by mutableStateOf(false)
    var currentCard: CardItem? by mutableStateOf(null)
    var cardList by mutableStateOf(listOf<CardItem>())
    var isLive by mutableStateOf(false)

    lateinit var player : ExoPlayer;

    fun init(context: Context) {
        player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isLive = player.isCurrentMediaItemLive()
                        }
                        super.onPlaybackStateChanged(state)
                        playerState = state
                    }
                })
                playWhenReady = true
        }
    }

    @OptIn(UnstableApi::class)
    fun setMediaSource(newMediaItem: MediaItem) {

        // Prepare the player with the new MediaSource
        player.setMediaItem(newMediaItem)

        // Optionally, seek to a specific position or the start
        player.seekToDefaultPosition() // Starts from beginning
        // or player.seekTo(specificPositionMs) // Start at specific time

        // reset isLive before playing
        isLive = false
        // Prepare and play
        player.prepare()
    }
}