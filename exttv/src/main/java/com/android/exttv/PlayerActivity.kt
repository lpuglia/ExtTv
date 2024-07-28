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
import android.os.PersistableBundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import com.android.exttv.model.Episode
import com.android.exttv.model.Program
import com.android.exttv.model.ProgramDatabase
import com.android.exttv.scrapers.DisplayerManager
import com.android.exttv.util.AppLinkHelper
import com.android.exttv.util.AppLinkHelper.PlaybackAction
import com.android.exttv.util.RemoteKeyEvent
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import kotlinx.serialization.Serializable
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.net.CookieManager
import java.security.Security
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okio.GzipSource
import okio.buffer


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
        val art: Map<String, String>
    )

    private var playerView: PlayerView? = null
    private var trackSelector: DefaultTrackSelector? = null

    private var remoteKeyEvent: RemoteKeyEvent? = null
    private var currentEpisode: Episode? = null
    private var paused = false

    var player: SimpleExoPlayer? = null
    var cardsReady: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gson = Gson()
        val mPrefs = getSharedPreferences("test", MODE_PRIVATE)
        val json = mPrefs.getString("programs", "")
        val type = object : TypeToken<LinkedHashMap<Int?, Program?>?>() {}.type
        ProgramDatabase.programs = gson.fromJson(json, type)

        Security.insertProviderAt(Conscrypt.newProvider(), 1) //without this I get handshake error

        // disable strict mode because ScraperManager.postfinished may need to scrape a proxy when onDemand is called
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)

        val intent = intent
        val data = intent!!.data

        // Check if the intent and data are not null
        if (intent != null && data != null) {
            val uriString = data.toString()
            if (uriString.startsWith("kodi://")) {
                val mediaSource = data.getQueryParameter("media_source")
                    ?.let { Json.decodeFromString<ExtTvMediaSource>(it) }

                initializePlayer(true)
                currentEpisode = Episode().setPageURL(uriString).setTitle("External Video Stream")
                    .setDescription(uriString).setAirDate(
                        GregorianCalendar()
                    )
                val program =
                    Program().setType("OnDemand").setVideoUrl(uriString).setEpisode(currentEpisode)
                remoteKeyEvent = RemoteKeyEvent(this, program.isLive, program.hashCode().toLong())

                val displayerManager = DisplayerManager(this, false)
                displayerManager.setTopContainer(currentEpisode)
                preparePlayer(mediaSource!!)
            }
        }
    }

    override fun onRestart() { //only called at standby thanks to onUserLeaveHint
        super.onRestart()

        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)

        val currentProgram = setCurrentProgramFromIntent(intent.data)
        paused = false
        initializePlayer(currentProgram!!.isLive)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return remoteKeyEvent!!.dispatchKeyEvent(event)
    }

    private val currentEpisodeCursor: Long
        get() {
            val position = AppLinkHelper.getEpisodeCursor(
                currentEpisode,
                baseContext
            )
            val duration = currentEpisode!!.durationLong
            if (duration == 0L) return position
            if (position < duration - (duration / 99)) return position // if cursor is before 99% of the duration

            return 0
        }

    fun setCurrentEpisodeCursor() {
        AppLinkHelper.setEpisodeCursor(player!!.currentPosition, currentEpisode, baseContext)
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

//                    Log.d("DRM_RESPONSE", "URL: ${request.url}, Response Code: ${originalResponse.code}")
//                    Log.d("DRM_RESPONSE", "Headers: ${request.headers} -> ${originalResponse.headers}")
//                    Log.d("DRM_RESPONSE", "Body: $originalResponse")
//                    Log.d("DRM_RESPONSE", "BodyDECOMP: ${decompressedBody}")
                    originalResponse.newBuilder()
                        .header("Content-Encoding", "identity")
                        .removeHeader("Content-Length")
                        .body(decompressedBody)
                        .build()
                } else {
//                    Log.d("DRM_RESPONSE", "URL: ${request.url}, Response Code: ${originalResponse.code}")
//                    Log.d("DRM_RESPONSE", "Headers: ${request.headers} -> ${originalResponse.headers}")
//                    Log.d("DRM_RESPONSE", "Body: $originalResponse")
                    originalResponse
                }

            }
//        if (requiresProxy) initClientProxy(clientBuilder)
        clientBuilder.cookieJar(JavaNetCookieJar(CookieManager()))
        return OkHttpDataSource.Factory(clientBuilder.build())
    }

    fun preparePlayer(mediaSource: ExtTvMediaSource) {

        player?.stop()
        if (!paused) player?.playWhenReady = true
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

        player?.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        player?.prepare()

        if (currentEpisode != null) {
            val position = currentEpisodeCursor
            if (position != 0L) player?.seekTo(position)
        }
    }

    private fun setCurrentProgramFromIntent(intentUri: Uri?): Program? {
        val action = AppLinkHelper.extractAction(intentUri)
        var program: Program? = null
        if (AppLinkHelper.PLAYBACK == action.action) {
            val paction = action as PlaybackAction
            program = ProgramDatabase.programs[paction.movieId.toInt()]
            remoteKeyEvent = RemoteKeyEvent(this, program!!.isLive, program.hashCode().toLong())

            if (program.isLive) {
                val watermark = findViewById<ImageView>(R.id.watermark)
                Picasso.with(baseContext).load(program.logo).into(watermark)
            }
            //        } else if (AppLinkHelper.BROWSE.equals(action.getAction())) {
        } else {
            throw IllegalArgumentException("Invalid Action $action")
        }
        val plugin_files: MutableSet<String> = LinkedHashSet()
        val bundle = PersistableBundle()
        for (p in ProgramDatabase.programs.values) {
            plugin_files.add(p.scraperURL)
            bundle.putLong(p.type, p.channelId)
        }
//        val syncProgramsJobService = SyncProgramsJobService()

//        for (p in plugin_files) syncProgramsJobService.idMap[p] = HashSet()
//        for (p in plugin_files) {
//            syncProgramsJobService.syncProgramManager.add(
//                syncProgramsJobService.SyncProgramManager(
//                    this, p, bundle
//                )
//            )
//        }
        return program
    }

//    fun showLogIn(currentProgram: Program?) {
//        val builder = AlertDialog.Builder(this)
//        // Get the layout inflater
//        val inflater = this.layoutInflater
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        val dialogView = inflater.inflate(R.layout.popup_login, null)
//        builder.setView(dialogView) // Add action buttons
//            .setPositiveButton("SignIn") { dialog: DialogInterface?, id: Int ->
//                val prefs =
//                    applicationContext.getSharedPreferences("com.android.exttv", 0)
//                val editor = prefs.edit()
//                var editText = dialogView.findViewById<EditText>(R.id.username)
//                editor.putString("username", editText.text.toString())
//                editText = dialogView.findViewById(R.id.password)
//                editor.putString("password", editText.text.toString())
//                editor.apply()
//                scrape(currentProgram)
//            }
//            .setNegativeButton(
//                "cancel"
//            ) { dialog: DialogInterface?, id: Int ->
//                scrape(
//                    currentProgram
//                )
//            }
//        val alert = builder.create()
//        alert.show()
//    }

    fun setCurrentEpisode(episode: Episode?) {
        currentEpisode = episode
    }

    public override fun onPause() {
        super.onPause()
        player!!.playWhenReady = false
        if (currentEpisode != null) { //if on-demand program
            setCurrentEpisodeCursor()
        }
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
//        if (scraper!!.isLive) {
//            player!!.release()
//            finish()
//        } else {
            player!!.release()
            paused = true
            cardsReady = false
//            scraper!!.cancel()
//            scraper = null
//        }
    }

    var previousPlaybackState: Int = ExoPlayer.STATE_IDLE
    var previousState: Boolean = false

    private fun findTextRendererIndex(): Int {
        for (i in 0 until player!!.rendererCount) {
            if (player!!.getRendererType(i) == C.TRACK_TYPE_TEXT) {
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
        for (i in 0 until player!!.rendererCount) {
            if (player!!.getRendererType(i) == C.TRACK_TYPE_TEXT) {
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

    private fun customizeSubtitlesAppearance() {
        // Example customization: White text, semi-transparent black background
        val style = CaptionStyleCompat(
            Color.YELLOW, Color.TRANSPARENT, Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null
        )

        val subtitleView = playerView!!.subtitleView
        if (subtitleView != null) {
            subtitleView.setStyle(style)
            subtitleView.setFixedTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.subtitle_font_size)
            )
        }
    }

    private fun initializePlayer(isLive: Boolean) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        val circle = ShapeDrawable(OvalShape())
        circle.paint.color = Color.parseColor("#DDDDDD")

        val play = findViewById<ImageButton>(R.id.exo_play)
        play.background = circle
        var drawable = play.drawable
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")))

        val pause = findViewById<ImageButton>(R.id.exo_pause)
        pause.background = circle
        drawable = pause.drawable
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")))

        trackSelector = DefaultTrackSelector(this)
        player = SimpleExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .build()

        playerView!!.controllerShowTimeoutMs = 0

        player!!.addListener(object : Player.Listener {
            private fun showUI() {
                if (handler != null) handler!!.removeCallbacksAndMessages(null)
                var hiddenPanel = findViewById<View>(R.id.top_container) as ViewGroup
                if (hiddenPanel.visibility != View.VISIBLE) {
                    findViewById<View>(R.id.playpause).visibility = View.VISIBLE
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

            var handler: Handler? = null
            private fun hideUI() {
                handler = Handler(Looper.getMainLooper())
                handler!!.postDelayed(Runnable {
                    var hiddenPanel =
                        findViewById<View>(R.id.top_container) as ViewGroup
                    if (hiddenPanel.visibility == View.VISIBLE) {
                        findViewById<View>(R.id.playpause).visibility = View.INVISIBLE
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
                    if (!(playWhenReady && playbackState == Player.STATE_READY)) {
                        findViewById<View>(R.id.playpause).visibility = View.VISIBLE
                    } else {
                        findViewById<View>(R.id.playpause).visibility = View.INVISIBLE
                    }
                } else {
                    if (playbackState == ExoPlayer.STATE_ENDED) {
//                        scraper!!.displayerManager.playNextEpisode(currentEpisode)
                    } else if (!leaving) {
                        if ((previousState && previousPlaybackState == Player.STATE_READY) && playbackState == ExoPlayer.STATE_BUFFERING) {
                            showUI()
                        } else if (previousPlaybackState == ExoPlayer.STATE_BUFFERING && (playWhenReady && playbackState == Player.STATE_READY)) {
                            hideUI()
                        } else if ((previousPlaybackState == Player.STATE_READY && playbackState == ExoPlayer.STATE_BUFFERING) ||
                            (playbackState == Player.STATE_READY && previousPlaybackState == ExoPlayer.STATE_BUFFERING)
                        ) {
                            //DO NOTHING
                        } else if (!(playWhenReady && playbackState == Player.STATE_READY) && playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_BUFFERING) {
                            showUI()
                        } else if (playWhenReady && playbackState == Player.STATE_READY) {
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
        enableSubtitlesByDefault()
        findViewById<View>(R.id.btnToggleSubtitles).setOnClickListener { toggleSubtitles() }

        customizeSubtitlesAppearance()

        playerView!!.player = player
    }
}