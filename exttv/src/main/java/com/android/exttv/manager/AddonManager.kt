package com.android.exttv.manager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import java.io.File
import java.util.SortedSet
import java.util.TreeSet

data class Addon(
    val addonid: String,
    val name: String,
    val icon: String,
    val description: String
)

object AddonManager {
    private lateinit var addons : SortedSet<String>
    var focusedIndex by mutableIntStateOf(-1)
    var selectedIndex by mutableIntStateOf(-1)
    // this force focus to settings button when pressing left from addon item
    var settingsRequesters by mutableStateOf(listOf<FocusRequester>())

    @Composable
    fun init(context: Context){
        val addonsPath = File(context.filesDir, "exttv_home/addons")
        val toReturn = if (addonsPath.exists() && addonsPath.isDirectory) {
            addons = TreeSet(addonsPath.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList())
        } else {
            addons = TreeSet()
        }
        settingsRequesters = rememberUpdatedState(
            newValue = List(addons.size) { FocusRequester() }
        ).value
        return toReturn
    }

    fun getSelectedAddon(): Int {
        return selectedIndex
    }

    fun uninstallAddon(index: Int) {
        val addonName = addons.elementAt(index)
        addons.remove(addonName)
        selectedIndex = -1
    }

    fun isSelected(index: Int): Boolean {
        return selectedIndex == index
    }

    fun getAllAddons(): SortedSet<String> {
        return addons
    }

    fun add(pluginName: String) {
        addons.add(pluginName)
    }

    fun selectAddon(pluginName: String) {
        selectedIndex = addons.indexOf(pluginName)
    }

    fun get(index: Int): String {
        return addons.elementAt(index)
    }

}