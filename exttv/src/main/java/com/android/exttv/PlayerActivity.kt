/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.exttv

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okio.GzipSource
import okio.buffer


@UnstableApi
class PlayerActivity : Activity() {

    @Serializable
    data class License(
        val headers: Map<String, String> = emptyMap(),
        val licenseType: String = "",
        val licenseKey: String = ""
    )

    @Serializable
    data class ExtTvMediaSource(
        val headers: Map<String, String> = emptyMap(),
        val source: String,
        val streamType: String,
        val license: License = License(),
        val label: String,
        val label2: String,
        val plot: String,
        val art: Map<String, String>
    )

    private lateinit var playerView: PlayerView
    private var trackSelector: DefaultTrackSelector? = null

    private var paused = false

    lateinit var player: ExoPlayer
    var cardsReady: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Security.insertProviderAt(Conscrypt.newProvider(), 1) //without this I get handshake error

        // disable strict mode because ScraperManager.postfinished may need to scrape a proxy when onDemand is called
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)

        val data = intent.data
        data?.let {
            val uriString = data.toString()
            if (uriString.startsWith("exttv://")) {
                val mediaSource = data.getQueryParameter("media_source")
                    ?.let { Json.decodeFromString<ExtTvMediaSource>(it) }

                initializePlayer(true)
                preparePlayer(mediaSource!!)
            }
        }
    }

    override fun onRestart() { //only called at standby thanks to onUserLeaveHint
        super.onRestart()

        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return false;
    }

    fun clientFactory(headers: Map<String, String>): OkHttpDataSource.Factory {
        val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val newRequestBuilder = chain.request().newBuilder()
                for ((key, value) in headers) {
                    newRequestBuilder.header(key, value)
                }
                chain.proceed(newRequestBuilder.build())
            }
            .addInterceptor { chain ->
                val request = chain.request()
                val originalResponse = chain.proceed(request)
                val contentEncoding = originalResponse.header("Content-Encoding")

                if (contentEncoding != null && contentEncoding.equals("gzip", ignoreCase = true)) {
                    val responseBody = originalResponse.body
                    val gzipSource = GzipSource(responseBody!!.source())
                    val decompressedBody = ResponseBody.create(responseBody.contentType(), -1, gzipSource.buffer())

                    originalResponse.newBuilder()
                        .header("Content-Encoding", "identity")
                        .removeHeader("Content-Length")
                        .body(decompressedBody)
                        .build()
                } else {
                    originalResponse
                }

            }
//        if (requiresProxy) initClientProxy(clientBuilder)
//        val cookieManager = CookieManager()
//        val cookieJar = object : CookieJar {override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
//            for (cookie in cookies) {
//                cookieManager.cookieStore.add(url.toUri(), cookie)
//            }
//        }
//        }
//        clientBuilder.cookieJar(cookieJar)
        return OkHttpDataSource.Factory(clientBuilder.build())
    }

    fun preparePlayer(mediaSource: ExtTvMediaSource) {

        player.stop()
        if (!paused) player.playWhenReady = true
        else paused = false

        val mediaItem = MediaItem.fromUri(Uri.parse(mediaSource.source))

        val mediaDataSourceFactory = clientFactory(mediaSource.headers)

        val mediaSourceFactory = when (mediaSource.streamType) {
            "application/dash+xml" -> {
                val manifestDataSourceFactory = clientFactory(mediaSource.license.headers)
                val playreadyCallback = HttpMediaDrmCallback(mediaSource.license.licenseKey, manifestDataSourceFactory)

                val drmManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        if (mediaSource.license.licenseType == "com.widevine.alpha") C.WIDEVINE_UUID else C.CLEARKEY_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(playreadyCallback)

                DashMediaSource.Factory(mediaDataSourceFactory).setDrmSessionManagerProvider { drmManager }
            }
            "application/x-mpegURL" -> HlsMediaSource.Factory(mediaDataSourceFactory)
            "extractor" -> ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            "mp4", "mkv", "" -> DefaultMediaSourceFactory(mediaDataSourceFactory)
            else -> {
                throw IllegalArgumentException("Unsupported media source type: ${mediaSource.streamType}")
            }
        }

        player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        player.prepare()
    }

    public override fun onPause() {
        super.onPause()
        player.playWhenReady = false
    }

    override fun onUserLeaveHint() { // this function is only called on home button press, not called on standby
        super.onUserLeaveHint()
        returnHomeScreen()
    }

    private var leaving = false
    fun returnHomeScreen() {
        leaving = true
//        scraper!!.cancel() // This ensure the release of all network resources;
        finish()
    }

    public override fun onStop() {
        super.onStop()
        player.release()
        paused = true
        cardsReady = false
    }

    var previousPlaybackState: Int = ExoPlayer.STATE_IDLE
    var previousState: Boolean = false

    private fun findTextRendererIndex(): Int {
        for (i in 0 until player.rendererCount) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i
            }
        }
        return -1 // Text renderer not found
    }

    private fun enableSubtitlesByDefault() {
        val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
            ?: return  // No tracks available

        // Assuming the text renderer is at a conventional index (often 2, but this can vary)
        val rendererIndex = findTextRendererIndex()
        if (rendererIndex == -1) {
            return  // No text renderer found
        }
        val params = trackSelector!!.parameters
        params.buildUpon().setRendererDisabled(rendererIndex, true)
    }

    fun toggleSubtitles() {
        // Obtain the current track selection parameters from the track selector
        val params = trackSelector!!.parameters

        // Identify the text (subtitle) renderer index
        var textRendererIndex = -1
        for (i in 0 until player.rendererCount) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                textRendererIndex = i
                break
            }
        }

        if (textRendererIndex == -1) {
            // No text renderer found, can't toggle subtitles
            return
        }

        // Toggle the enabling state of the text renderer
        val isDisabled = params.getRendererDisabled(textRendererIndex)
        val parametersBuilder = params.buildUpon()
            .setRendererDisabled(textRendererIndex, !isDisabled)
        parametersBuilder.clearSelectionOverrides()

        // Apply the changes to the track selector
        trackSelector!!.setParameters(parametersBuilder)
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(isLive: Boolean) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        val circle = ShapeDrawable(OvalShape())
        circle.paint.color = Color.parseColor("#DDDDDD")

        val playButton = findViewById<ImageButton>(R.id.exo_play)
        playButton.background = circle
        var drawable = playButton.drawable
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")))

        val pauseButton = findViewById<ImageButton>(R.id.exo_pause)
        pauseButton.background = circle
        drawable = pauseButton.drawable
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")))

        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .build()

        val playPauseLayout = findViewById<View>(R.id.playpause)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPauseLayout.visibility = if (isPlaying) View.INVISIBLE else View.VISIBLE
                playButton.visibility = if (isPlaying) View.GONE else View.VISIBLE
                pauseButton.visibility = if (isPlaying) View.VISIBLE else View.GONE
            }
        })

        playerView.controllerShowTimeoutMs = 0

        player.addListener(object : Player.Listener {
            private fun showUI() {
                val handler = Handler(Looper.getMainLooper())
                handler.removeCallbacksAndMessages(null)
                var hiddenPanel = findViewById<View>(R.id.top_container) as ViewGroup
                if (hiddenPanel.visibility != View.VISIBLE) {
//                    findViewById<View>(R.id.playpause).visibility = View.VISIBLE
                    var bottomUp = AnimationUtils.loadAnimation(
                        baseContext,
                        R.anim.controls_pop_in_top
                    )
                    hiddenPanel.startAnimation(bottomUp)
                    hiddenPanel.visibility = View.VISIBLE

                    bottomUp = AnimationUtils.loadAnimation(
                        baseContext,
                        R.anim.controls_pop_in
                    )
                    hiddenPanel = findViewById<View>(R.id.control_container) as ViewGroup
                    hiddenPanel.startAnimation(bottomUp)
                    hiddenPanel.visibility = View.VISIBLE
                }
            }

            private fun hideUI() {
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(Runnable {
                    var hiddenPanel =
                        findViewById<View>(R.id.top_container) as ViewGroup
                    if (hiddenPanel.visibility == View.VISIBLE) {
//                        findViewById<View>(R.id.playpause).visibility = View.INVISIBLE
                        var bottomUp =
                            AnimationUtils.loadAnimation(
                                baseContext,
                                R.anim.controls_pop_out_top
                            )
                        if (hiddenPanel.visibility == View.VISIBLE) {
                            hiddenPanel.startAnimation(bottomUp)
                            hiddenPanel.visibility = View.INVISIBLE
                        }
                        bottomUp = AnimationUtils.loadAnimation(
                            baseContext,
                            R.anim.controls_pop_out
                        )
                        hiddenPanel =
                            findViewById<View>(R.id.control_container) as ViewGroup
                        if (hiddenPanel.visibility == View.VISIBLE) {
                            hiddenPanel.startAnimation(bottomUp)
                            hiddenPanel.visibility = View.INVISIBLE
                        }
                    }
                }, 3000)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                val watermark = findViewById<ImageView>(R.id.watermark)

                if (isLive) {
//                    if (!(playWhenReady && playbackState == ExoPlayer.STATE_READY)) {
//                        findViewById<View>(R.id.playpause).visibility = View.VISIBLE
//                    } else {
//                        findViewById<View>(R.id.playpause).visibility = View.INVISIBLE
//                    }
                } else {
                    if (playbackState == ExoPlayer.STATE_ENDED) {
//                        scraper!!.displayerManager.playNextEpisode(currentEpisode)
                    } else if (!leaving) {
                        if ((previousState && previousPlaybackState == ExoPlayer.STATE_READY) && playbackState == ExoPlayer.STATE_BUFFERING) {
                            showUI()
                        } else if (previousPlaybackState == ExoPlayer.STATE_BUFFERING && (playWhenReady && playbackState == ExoPlayer.STATE_READY)) {
                            hideUI()
                        } else if ((previousPlaybackState == ExoPlayer.STATE_READY && playbackState == ExoPlayer.STATE_BUFFERING) ||
                            (playbackState == ExoPlayer.STATE_READY && previousPlaybackState == ExoPlayer.STATE_BUFFERING)
                        ) {
                            //DO NOTHING
                        } else if (!(playWhenReady && playbackState == ExoPlayer.STATE_READY) && playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_BUFFERING) {
                            showUI()
                        } else if (playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                            hideUI()
                        }
                    }
                }

                when (playbackState) {
                    ExoPlayer.STATE_IDLE -> Log.d("STATE", "STATE_IDLE")
                    ExoPlayer.STATE_BUFFERING -> Log.d("STATE", "STATE_BUFFERING")
                    ExoPlayer.STATE_READY -> if (playWhenReady) {
                        Log.d("STATE", "STATE_READY_PLAY")
                    } else {
                        Log.d("STATE", "STATE_READY_PAUSE")
                    }

                    ExoPlayer.STATE_ENDED -> Log.d("STATE", "STATE_ENDED")
                }
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    progressBar.visibility = View.VISIBLE
                    watermark.visibility = View.VISIBLE
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    progressBar.visibility = View.INVISIBLE
                    watermark.visibility = View.INVISIBLE
                }
                previousState = playWhenReady
                previousPlaybackState = playbackState
            }
        })

//        customizeSubtitlesAppearance()

        playerView.player = player
    }
}