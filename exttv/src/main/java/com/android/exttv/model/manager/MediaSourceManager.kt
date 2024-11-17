package com.android.exttv.model.manager

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.android.exttv.model.data.ExtTvMediaSource
import com.android.exttv.util.clientFactory

object MediaSourceManager {

    @OptIn(UnstableApi::class)
    fun preparePlayer(extTvMediaSource: ExtTvMediaSource): MediaSource {
        val mediaItem = MediaItem.fromUri(Uri.parse(extTvMediaSource.source))

        val mediaDataSourceFactory = clientFactory(extTvMediaSource.headers)

        val mediaSourceFactory = when (extTvMediaSource.streamType) {
            "application/dash+xml" -> {
                val manifestDataSourceFactory = clientFactory(extTvMediaSource.license.headers)
                val playreadyCallback = HttpMediaDrmCallback(extTvMediaSource.license.licenseKey, manifestDataSourceFactory)

                val drmManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        if (extTvMediaSource.license.licenseType == "com.widevine.alpha") C.WIDEVINE_UUID else C.CLEARKEY_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(playreadyCallback)

                DashMediaSource.Factory(mediaDataSourceFactory).setDrmSessionManagerProvider { drmManager }
            }
            "application/x-mpegURL" -> HlsMediaSource.Factory(mediaDataSourceFactory)
            "extractor" -> ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            "mp4", "mkv", "" -> DefaultMediaSourceFactory(mediaDataSourceFactory)
            else -> {
                throw IllegalArgumentException("Unsupported media source type: ${extTvMediaSource.streamType}")
            }
        }
        return mediaSourceFactory.createMediaSource(mediaItem)
    }
}