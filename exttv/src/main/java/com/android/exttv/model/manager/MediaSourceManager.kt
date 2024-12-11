package com.android.exttv.model.manager

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.android.exttv.model.data.ExtTvMediaSource

object MediaSourceManager {

    fun preparePlayer(extTvMediaSource: ExtTvMediaSource): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(extTvMediaSource.source))

        if (extTvMediaSource.streamType == "application/dash+xml") {
            val drmConfiguration = MediaItem.DrmConfiguration.Builder(
                if (extTvMediaSource.license.licenseType == "com.widevine.alpha") C.WIDEVINE_UUID else C.CLEARKEY_UUID
            ).apply {
                setLicenseUri(extTvMediaSource.license.licenseKey)
                setLicenseRequestHeaders(extTvMediaSource.license.headers)
            }.build()

            mediaItemBuilder.setDrmConfiguration(drmConfiguration)
        }

        return mediaItemBuilder.build()
    }
}