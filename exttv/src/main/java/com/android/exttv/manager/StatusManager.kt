package com.android.exttv.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester

enum class LoadingStatus {
    DONE,
    FETCHING_ADDON,
    INSTALLING_ADDON,
    SELECTING_ADDON,
    SELECTING_SECTION,
    SECTION_LOADED,
}

object StatusManager {
    val titleMap: MutableMap<String, String> = mutableMapOf()
    val focusRequester = FocusRequester()
    var sectionList by mutableStateOf(listOf<Section>())
    var loadingState by mutableStateOf(LoadingStatus.DONE)
    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var bgImage by mutableStateOf("")
    var showContextMenu by mutableStateOf(false)
}