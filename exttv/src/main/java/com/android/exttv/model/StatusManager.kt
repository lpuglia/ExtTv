package com.android.exttv.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.exttv.view.MainActivity

enum class LoadingStatus {
    DONE,
    FETCHING_ADDON,
    INSTALLING_ADDON,
    SELECTING_ADDON,
    SELECTING_SECTION,
    SECTION_LOADED,
}

object StatusManager {
    var reboundEnter = false
    var loadingState by mutableStateOf(LoadingStatus.DONE)
    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var showUpdateDialog by mutableStateOf(false)
    var showUninstallDialog by mutableStateOf(false)
    var showRemoveDialog by mutableStateOf(false)
    var showFavouriteMenu by mutableStateOf(false)
    var showNewPlaylistMenu by mutableStateOf(false)
    var selectedIndex by mutableIntStateOf(-1)
    var focusedContextIndex by mutableIntStateOf(-1)
    var drawerItems by mutableStateOf(listOf<String>())
    var bgImage by mutableStateOf("")

    lateinit var context : MainActivity
    lateinit var showToast : (String?, Int) -> Unit

    fun init(context: MainActivity){
        this.context = context
        showToast = context::showToast
        update()
    }

    fun update() {
        drawerItems = AddonManager.getAllAddonNames() + FavouriteManager.getAllFavouriteNames() +
                listOf("Add from Repository", "Add from GitHub")
    }
}