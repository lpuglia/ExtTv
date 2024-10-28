package com.android.exttv.model.data

import kotlinx.serialization.Serializable

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
    val uriParent: String = "",
    val mediaSource: String = "",
) {
    val pluginName: String
        get() = uri.split("://")[1].split("/")[0]
}