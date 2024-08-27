package com.android.exttv.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    val focusRequester = FocusRequester()
    var loadingState by mutableStateOf(LoadingStatus.DONE)
    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var bgImage by mutableStateOf("")
    var showUninstallDialog by mutableStateOf(false)
    var showUpdateDialog by mutableStateOf(false)
    var showFavouriteMenu by mutableStateOf(false)
    var showNewPlaylistMenu by mutableStateOf(false)
    var selectedIndex by mutableIntStateOf(-1)
    var focusedContextIndex by mutableIntStateOf(-1)

}