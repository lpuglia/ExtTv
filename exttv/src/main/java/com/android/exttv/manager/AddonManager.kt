package com.android.exttv.manager

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.android.exttv.util.getFromGit
import com.android.exttv.util.getFromRepository
import java.io.File
import java.util.SortedSet
import java.util.TreeSet

object AddonManager {
    data class Addon(
        val addonid: String,
        val name: String,
        val icon: String,
        val description: String
    )

    private lateinit var addons : SortedSet<String>
    var addonsPath = File("")
    var focusedContextIndex by mutableIntStateOf(-1)
    var selectedIndex by mutableIntStateOf(-1)

    fun init(context: Context){
        addonsPath = File(context.filesDir, "exttv_home/addons")
        if (addonsPath.exists() && addonsPath.isDirectory) {
            addons = TreeSet(addonsPath.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList())
        } else {
            addons = TreeSet()
        }
    }

    fun getSelectedAddon(): Int {
        return selectedIndex
    }

    fun installAddon(url: String, force: Boolean = false){
        StatusManager.loadingState = LoadingStatus.INSTALLING_ADDON
        fun isValidUrl(url: String): Boolean {
            val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
            return url.matches(urlRegex)
        }
        val currentlySelected = addons.elementAtOrNull(selectedIndex)
        val pluginName = if(isValidUrl(url)){
            getFromRepository(url, force)
        }else{
            getFromGit(url, force)
        }
        addons.add(pluginName)
        if(currentlySelected != null)
            selectedIndex = addons.indexOf(currentlySelected)
        focusedContextIndex = -1
        PythonManager.selectAddon(pluginName)
    }

    fun uninstallAddon(index: Int) {
        val addonName = addons.elementAt(index)
        val directory = File("$addonsPath/$addonName")
        try {
            directory.deleteRecursively()
            val currentlySelected = addons.elementAtOrNull(selectedIndex)
            addons.remove(addonName)
            focusedContextIndex = -1
            if(selectedIndex==index){
                selectedIndex = -1
                SectionManager.clearSections()
            }else if(currentlySelected != null){
                selectedIndex = addons.indexOf(currentlySelected)
            }

        } catch (e: Exception) {
            Log.d("AddonManager","Error deleting directory: ${e.message}")
            e.printStackTrace()
            throw e
        }
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

    fun size(): Int {
        return addons.size
    }

}