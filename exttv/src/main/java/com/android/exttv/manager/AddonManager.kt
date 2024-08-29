package com.android.exttv.manager

import android.content.Context
import android.util.Log
import com.android.exttv.util.ContextManager
import com.android.exttv.util.getFromGit
import com.android.exttv.util.getFromRepository
import java.io.File
import com.android.exttv.manager.StatusManager as Status

object AddonManager {
    var addonsPath = File("")

    fun init(context: Context){
        addonsPath = File(context.filesDir, "exttv_home/addons")
        if (!addonsPath.exists() || !addonsPath.isDirectory) {
            addonsPath.mkdirs()
        }
    }

    private fun getAllAddons(): List<String> {
        return addonsPath.listFiles { file -> file.isDirectory }
            ?.map { it.name }
            ?.sorted() // Ensure the list is sorted
            ?: emptyList()
    }

    fun installAddon(url: String, force: Boolean = false){
        Status.loadingState = LoadingStatus.INSTALLING_ADDON

        fun isValidUrl(url: String): Boolean {
            val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
            return url.matches(urlRegex)
        }

        val currentlySelected = getAllAddons().getOrNull(Status.selectedIndex)
        val pluginName = if(isValidUrl(url)){
            getFromRepository(url, force)
        } else {
            getFromGit(url, force)
        }

        Status.focusedContextIndex = -1
        PythonManager.selectAddon(pluginName)
        if(currentlySelected != null) {
            Status.selectedIndex = getAllAddons().indexOf(currentlySelected)
        }
        ContextManager.update()
    }

    fun uninstallAddon(index: Int) {
        val installedAddons = getAllAddons()
        val addonName = installedAddons.getOrNull(index) ?: return
        val directory = File("$addonsPath/$addonName")

        try {
            directory.deleteRecursively()
            val currentlySelected = installedAddons.getOrNull(Status.selectedIndex)
            Status.focusedContextIndex = -1

            if (Status.selectedIndex == index) {
                Status.selectedIndex = -1
                SectionManager.clearSections()
            } else if (currentlySelected != null) {
                Status.selectedIndex = getAllAddons().indexOf(currentlySelected)
            }

        } catch (e: Exception) {
            Log.d("AddonManager", "Error deleting directory: ${e.message}")
            e.printStackTrace()
            throw e
        }
        ContextManager.update()
    }

    fun getAllAddonNames(): List<String> {
        return getAllAddons()
    }

    val size: Int get() = getAllAddons().size
}