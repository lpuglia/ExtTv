package com.android.exttv.model.data

import com.android.exttv.model.manager.AddonManager.addonsPath
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CardItem(
    val uri: String,
    val label : String,
    val label2 : String = "",
    val plot : String = "",
    val thumbnailUrl: String = "",
    val posterUrl: String = "",
    val fanartUrl: String = "",
    val isFolder: Boolean = true,
    val uriContainer: String = "", // used to update favourites thumb and info
    val uriParent: String = uriContainer, // uri to distinguish between plugin card and favourite card, may be equal to uriContainer
    val mediaSource: String = "",
    val favouriteLabel: String = "",
    val firstDiscovered: Long = 0,
    val isLive: Boolean = false,
    val card: CardItem? = null,
) {
    val pluginName: String
        get() = uri.split("://")[1].split("/")[0]

    val primaryArt: String
        get() = sanitizeArt(posterUrl.ifEmpty { thumbnailUrl.ifEmpty { fanartUrl } })

    val secondaryArt: String
        get() = sanitizeArt(
            when {
                thumbnailUrl.isNotEmpty() && thumbnailUrl != primaryArt -> thumbnailUrl
                fanartUrl.isNotEmpty() -> fanartUrl
                thumbnailUrl.isNotEmpty() -> thumbnailUrl
                else -> posterUrl
            }
        )

    val tertiaryArt: String
        get() = sanitizeArt(
            when {
                fanartUrl.isNotEmpty() -> fanartUrl
                thumbnailUrl.isNotEmpty() && thumbnailUrl != posterUrl -> thumbnailUrl
                else -> posterUrl
            }
        )

    // Helper function to check and add prefix for local resources
    private fun sanitizeArt(url: String): String {
        return if (isLocalResource(url)) File(File(addonsPath, pluginName), url).toString() else url
    }

    // Checks if URL is a local resource by ensuring it’s not an absolute URL or path
    private fun isLocalResource(url: String): Boolean {
        // Check if URL is not empty, does not start with http(s), and is not an absolute path
        return url.isNotEmpty() &&
                !url.startsWith("http://") &&
                !url.startsWith("https://") &&
                !File(url).isAbsolute
    }
}