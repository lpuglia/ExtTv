package com.android.exttv.model

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.android.exttv.util.clientFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object MediaSourceManager {

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

    fun preparePlayer(source: String): MediaSource {
        val mediaSource = source.let { Json.decodeFromString<ExtTvMediaSource>(it) }
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
        return mediaSourceFactory.createMediaSource(mediaItem)
    }
}