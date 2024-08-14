package com.android.exttv.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LoadingStatus {
    DONE,
    LOADING,
    ADDON,
    SECTION,
}

object StatusManager {
    val titleMap: MutableMap<String, String> = mutableMapOf()
    var sectionList by mutableStateOf(listOf<Section>())
    var loadingState by mutableStateOf(LoadingStatus.DONE)
    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var backgroundImageState by mutableStateOf("")
}