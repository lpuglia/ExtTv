package com.android.exttv.model.data

import kotlinx.serialization.Serializable

@Serializable
data class License(
    val headers: Map<String, String> = emptyMap(),
    val licenseType: String = "",
    val licenseKey: String = ""
)