package com.android.exttv.model

import android.content.Context
import android.util.Log
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object AddonManager {
    var addonsPath = File("")

    fun init(context: Context) {
        addonsPath = File(context.filesDir, "exttv_home/addons")
        if (!addonsPath.exists() || !addonsPath.isDirectory) {
            addonsPath.mkdirs()
        }
    }

    private fun getAllAddons(): List<Map<String, String>> {
        return addonsPath.listFiles { file -> file.isDirectory && file.name.startsWith("plugin.video.") }
            ?.mapNotNull { parseAddon(it) }
            ?.sortedBy { it["folder_name"] }
            ?: emptyList()
    }

    private fun parseAddon(directory: File): Map<String, String>? {
        val addonXml = File(directory, "addon.xml")
        if (!addonXml.exists()) return null

        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(addonXml)
            doc.documentElement.normalize()

            // val id = doc.getElementsByTagName("addon").item(0).attributes.getNamedItem("id")?.nodeValue
            val name = doc.getElementsByTagName("addon").item(0).attributes.getNamedItem("name")?.nodeValue
            val icon = doc.getElementsByTagName("icon").item(0)?.textContent

            mapOf(
                "folder_name" to directory.name,
                "addon_name" to (name ?: "Unknown"),
                "addon_icon" to (icon ?: "icon not found")
            )
        } catch (e: Exception) {
            Log.d("AddonManager", "Error parsing addon.xml in ${directory.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }

//    fun installAddon(url: String, force: Boolean = false): String {
//    }

    fun uninstallAddon(index: Int) {
        val installedAddons = getAllAddons()
        val addonFolderName = installedAddons.getOrNull(index)?.get("folder_name") ?: return
        val directory = File(addonsPath, addonFolderName)

        try {
            directory.deleteRecursively()
        } catch (e: Exception) {
            Log.d("AddonManager", "Error deleting directory: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getAllAddonNames(): List<String> {
        return getAllAddons().map { it["addon_name"] ?: "Unknown" }
    }

    operator fun get(index: Int): String {
        return getAllAddons()[index]["addon_name"] ?: "Unknown"
    }

    fun getIdByName(addonName: String): String? {
        return getAllAddons().find { it["addon_name"] == addonName }?.get("folder_name")
    }

    fun getIconByName(addonName: String): String? {
        return getAllAddons().find { it["addon_name"] == addonName }?.get("addon_icon")
    }

    fun getIconByFolderName(addonName: String): String? {
        return getAllAddons().find { it["folder_name"] == addonName }?.get("addon_icon")
    }

    val size: Int get() = getAllAddons().size
}