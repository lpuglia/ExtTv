package com.android.exttv.manager

import android.content.Context
import android.util.Log
import com.android.exttv.util.getFromGit
import com.android.exttv.util.getFromRepository
import java.io.File

object AddonManager {
    var addonsPath = File("")

    fun init(context: Context) {
        addonsPath = File(context.filesDir, "exttv_home/addons")
        if (!addonsPath.exists() || !addonsPath.isDirectory) {
            addonsPath.mkdirs()
        }
    }

    private fun getAllAddons(): List<String> {
        return addonsPath.listFiles { file -> file.isDirectory && file.name.startsWith("plugin.video.") }
            ?.map { it.name }
            ?.sorted() // Ensure the list is sorted
            ?: emptyList()
    }

//    fun installAddon(url: String, force: Boolean = false): String {
//    }

    fun uninstallAddon(index: Int) {
            val installedAddons = getAllAddons()
            val addonName = installedAddons.getOrNull(index) ?: return
            val directory = File("$addonsPath/$addonName")

            try {
                directory.deleteRecursively()
            } catch (e: Exception) {
                Log.d("AddonManager", "Error deleting directory: ${e.message}")
                e.printStackTrace()
                throw e
        }
    }

    fun getAllAddonNames(): List<String> {
        return getAllAddons()
    }

    operator fun get(index: Int): String {
        return getAllAddons()[index]
    }

    val size: Int get() = getAllAddons().size
}