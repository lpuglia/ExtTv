package com.android.exttv.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester

enum class LoadingStatus {
    DONE,
    LOADING,
    ADDON,
    SECTION,
    SECTION_DONE,
}

object StatusManager {
    val titleMap: MutableMap<String, String> = mutableMapOf()
    var sectionList by mutableStateOf(listOf<Section>())
    var loadingState by mutableStateOf(LoadingStatus.DONE)
    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var bgImage by mutableStateOf("")
}