package com.android.exttv.manager

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.SortedSet
import java.util.TreeSet

object AddonManager {
    private lateinit var addons : SortedSet<String>
    var focusedIndex by mutableIntStateOf(0)
    var selectedIndex by mutableIntStateOf(-1)

    fun init(context: Context){
        val addonsPath = File(context.filesDir, "exttv_home/addons")
        return if (addonsPath.exists() && addonsPath.isDirectory) {
            // Filter only directories and collect their names into a TreeSet
            addons = TreeSet(addonsPath.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList())
        } else {
            addons = TreeSet()
        }
    }

    fun getSelectedAddon(): Int {
        return selectedIndex
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

}