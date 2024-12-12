package com.android.exttv.model.data

import kotlinx.serialization.Serializable

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