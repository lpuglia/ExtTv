package com.android.exttv.manager

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import java.io.File
import java.util.SortedSet
import java.util.TreeSet

object AddonManager {
    private lateinit var addons : SortedSet<String>
    private val selectedIndex: MutableState<Int> = mutableIntStateOf(-1)

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
        return selectedIndex.value
    }

    fun isSelected(index: Int): Boolean {
        return selectedIndex.value == index
    }

    fun getAllAddons(): SortedSet<String> {
        return addons
    }

    fun add(pluginName: String) {
        addons.add(pluginName)
    }

    fun selectAddon(pluginName: String) {
        selectedIndex.value = addons.indexOf(pluginName)
    }

}