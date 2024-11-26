package com.android.exttv.model.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.exttv.model.data.CardItem

enum class LoadingStatus {
    DONE,
    FETCHING_ADDON,
    INSTALLING_ADDON,
    SELECTING_SECTION,
}


@SuppressLint("StaticFieldLeak") // i know what i'm doing (hopefully)
object StatusManager {
    var refocus by mutableStateOf(false)
    var reboundEnter = false
    var loadingState by mutableStateOf(LoadingStatus.DONE)

    var showGithubDialog by mutableStateOf(false)
    var showRepositoryDialog by mutableStateOf(false)
    var showUpdateDialog by mutableStateOf(false)
    var showUninstallDialog by mutableStateOf(false)
    var showRemoveDialog by mutableStateOf(false)
    var showFavouriteMenu by mutableStateOf(false)
    var showNewPlaylistMenu by mutableStateOf(false)
    var defaultPlaylistName by mutableStateOf("")

    var selectedAddonIndex by mutableIntStateOf(-1)
    var focusedAddonIndex by mutableIntStateOf(-1)
    var focusedContextIndex by mutableIntStateOf(-1)

    var drawerItems by mutableStateOf(listOf<String>())
    var bgImage by mutableStateOf("")

    lateinit var appContext: Context
    lateinit var lastSelectedCard : CardItem

    private lateinit var instance: Activity
    @JvmStatic // Optional annotation for Java interop
    fun getActivity(): Activity {
        return instance
    }

    fun init(activity: Activity?, context: Context){
        activity?.let { instance = it }
        if (!::appContext.isInitialized){
            appContext = context
            AddonManager.init(appContext)
            PythonManager.init(appContext)
            FavouriteManager.init(appContext)
        }
    }
}