package com.android.exttv.manager

import android.content.Context
import android.util.Log
import com.android.exttv.util.ContextManager
import com.android.exttv.util.getFromGit
import com.android.exttv.util.getFromRepository
import java.io.File
import java.util.SortedSet
import java.util.TreeSet
import com.android.exttv.manager.StatusManager as Status

object AddonManager {
    data class Addon(
        val addonid: String,
        val name: String,
        val icon: String,
        val description: String
    )

    private lateinit var addons : SortedSet<String>
    var addonsPath = File("")

    fun init(context: Context){
        addonsPath = File(context.filesDir, "exttv_home/addons")
        if (addonsPath.exists() && addonsPath.isDirectory) {
            addons = TreeSet(addonsPath.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList())
        } else {
            addons = TreeSet()
        }
    }

    fun installAddon(url: String, force: Boolean = false){
        Status.loadingState = LoadingStatus.INSTALLING_ADDON
        fun isValidUrl(url: String): Boolean {
            val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
            return url.matches(urlRegex)
        }
        val currentlySelected = addons.elementAtOrNull(Status.selectedIndex)
        val pluginName = if(isValidUrl(url)){
            getFromRepository(url, force)
        }else{
            getFromGit(url, force)
        }
        this.add(pluginName)
        if(currentlySelected != null)
            Status.selectedIndex = addons.indexOf(currentlySelected)
        Status.focusedContextIndex = -1
        PythonManager.selectAddon(pluginName)
    }

    fun uninstallAddon(index: Int) {
        val addonName = addons.elementAt(index)
        val directory = File("$addonsPath/$addonName")
        try {
            directory.deleteRecursively()
            val currentlySelected = addons.elementAtOrNull(Status.selectedIndex)
            this.remove(addonName)
            Status.focusedContextIndex = -1
            if(Status.selectedIndex==index){
                Status.selectedIndex = -1
                SectionManager.clearSections()
            }else if(currentlySelected != null){
                Status.selectedIndex = addons.indexOf(currentlySelected)
            }

        } catch (e: Exception) {
            Log.d("AddonManager","Error deleting directory: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun isSelected(index: Int): Boolean {
        return Status.selectedIndex == index
    }

    fun getAllAddonNames(): SortedSet<String> {
        return addons
    }

    fun add(pluginName: String) {
        addons.add(pluginName)
        ContextManager.init()
    }

    fun remove(pluginName: String) {
        addons.remove(pluginName)
        ContextManager.init()
    }

    fun selectAddon(pluginName: String) {
        Status.selectedIndex = addons.indexOf(pluginName)
    }

    fun get(index: Int): String {
        return addons.elementAt(index)
    }

    fun size(): Int {
        return addons.size
    }

}