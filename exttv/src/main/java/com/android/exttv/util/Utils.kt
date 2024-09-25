package com.android.exttv.util

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.android.exttv.manager.AddonManager as Addons
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import com.android.exttv.manager.StatusManager as Status

fun parseText(input: String): AnnotatedString {
    val stripped = input.replace("\n", "; ").trim()//.replace(Regex(";\\s*;+\\s*"), ";")
    val annotatedString = AnnotatedString.Builder()
    val regex = "\\[(B|I|LIGHT|UPPERCASE|LOWERCASE|CAPITALIZE)](.*?)\\[/\\1]".toRegex()

    var currentIndex = 0
    for (match in regex.findAll(stripped)) {
        val (tag, content) = match.destructured
        val startIndex = match.range.first

        if (currentIndex < startIndex) {
            annotatedString.append(AnnotatedString(stripped.substring(currentIndex, startIndex)))
        }

        when (tag) {
            "B" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    annotatedString.append(parseText(content))
                }
            }
            "I" -> {
                annotatedString.withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    annotatedString.append(parseText(content))
                }
            }
            "LIGHT" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                    annotatedString.append(parseText(content))
                }
            }
            "UPPERCASE" -> {
                annotatedString.append(AnnotatedString(content.uppercase()))
            }
            "LOWERCASE" -> {
                annotatedString.append(AnnotatedString(content.lowercase()))
            }
            "CAPITALIZE" -> {
                annotatedString.append(AnnotatedString(content.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }))
            }
        }

        currentIndex = match.range.last + 1
    }

    if (currentIndex < stripped.length) {
        annotatedString.append(AnnotatedString(stripped.substring(currentIndex)))
    }

    return annotatedString.toAnnotatedString()
}

fun cleanText(input: String): String {
    // Remove color tags

    // Remove carriage returns
    var cleanedText = input.replace(Regex("\\[CR\\]"), "")

    // Remove tabulators
    cleanedText = cleanedText.replace(Regex("\\[TABS](\\d+)\\[/TABS]")) { match ->
        val tabs = match.groupValues[1].toIntOrNull() ?: 0
        "\t".repeat(tabs)
    }
    cleanedText = cleanedText.replace(Regex("\\[COLOR\\s+[^\\]]*](.*?)\\[/COLOR]"), "$1")

    return cleanedText
}

fun getFromGit(url: String, force: Boolean = false) : String {
    val parts = url.split("/")
    if (parts.size < 3) throw IllegalArgumentException("Invalid URL format")

    val user = parts[0]
    val repo = parts[1]
    val branch = parts[2]

    val addonXmlUrl = "https://raw.githubusercontent.com/$user/$repo/$branch/addon.xml"
    val zipPath = "https://github.com/$user/$repo/archive/refs/heads/$branch.zip"

    try {
        // Check if the addon.xml file is accessible
        val connection = URL(addonXmlUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Error occurred while checking URL: ${connection.responseCode}")
        }

        // Fetch the addon.xml content
        val addonXml = URL(addonXmlUrl).readText()

        // Extract plugin name
        val pattern = Pattern.compile("""id="([^"]+)"""")
        val matcher = pattern.matcher(addonXml)
        val pluginName = if (matcher.find()) {
            matcher.group(1)
        } else {
            throw Exception("Failed to get plugin id")
        }

        installAddon(zipPath, pluginName, zipPath, false)
        return pluginName
    } catch (e: Exception) {
        throw Exception("Error occurred while processing URL: ${e.message}", e)
    }
}

fun getLatestZipName(url: String): Pair<String, String>{
    try {
        // Make an HTTP GET request to the provided URL
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.inputStream.use { inputStream ->
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Error occurred while fetching data: ${connection.responseCode}")
            }

            // Parse the JSON response
            val data = JSONObject(inputStream.bufferedReader().use { it.readText() })

            // Extract the necessary fields from the JSON
            val zipPath = data.getJSONObject("result")
                .getJSONObject("data")
                .getJSONObject("addon")
                .getJSONArray("platforms")
                .getJSONObject(0)
                .getString("path")

            val pluginName = data.getJSONObject("result")
                .getJSONObject("data")
                .getJSONObject("addon")
                .getString("addonid")
            return Pair(zipPath, pluginName)
        }
    } catch (e: Exception) {
        throw Exception("Failed to fetch or decode JSON data: ${e.message}", e)
    }
}

fun getFromRepository(addonId: String, force: Boolean = false): String {
    val url = "https://kodi.tv/page-data/addons/omega/${addonId}/page-data.json"
    val (zipPath, pluginName) = getLatestZipName(url)
    val mirrorZip = "https://mirrors.kodi.tv/addons/omega/" + zipPath.split("addons/omega/")[1]
    installAddon(mirrorZip, pluginName, url, false)
    return pluginName
}

fun unzip(zipFile: File, targetDir: File) {
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }

    ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
        var entry: ZipEntry? = zipInputStream.nextEntry
        while (entry != null) {
            val file = File(targetDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }
}

fun getRootDirectoryName(zipFile: File): String? {
    ZipFile(zipFile).use { zip ->
        val rootDirs = zip.entries().asSequence()
            .mapNotNull { entry ->
                entry.name.substringBefore("/").takeIf { it.isNotEmpty() }
            }
            .toSet()

        return if (rootDirs.size == 1) rootDirs.first() else null
    }
}

class DownloadError(message: String) : Exception(message)
class ExtractionError(message: String) : Exception(message)

@Serializable
data class PluginData(
    val zipURL: String,
    val pluginName: String,
    val sourceURL: String
)

fun installDependencies(pluginPath: File) {
    val xmlFile = pluginPath.resolve("addon.xml")
//    if (!xmlFile.exists()) {
//        throwFileNotFound()
//    }

    val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
    xmlDoc.documentElement.normalize()

    // Get all <import> nodes within <requires>
    val importNodes = xmlDoc.getElementsByTagName("import")

    // Iterate through the import nodes and extract the addon attribute
    for (i in 0 until importNodes.length) {
        val node = importNodes.item(i)
        if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
            val element = node as org.w3c.dom.Element
            val addonName = element.getAttribute("addon")
            if (addonName == "xbmc.python") continue
            Status.showToast("Dependency found: $addonName, installing...", Toast.LENGTH_SHORT)

            try{
                getFromRepository(addonName, false)
            }catch (e: Exception) {
                Status.showToast("Error while installing dependency $addonName: ${e.message}", Toast.LENGTH_SHORT)
            }
        }
    }
}

fun installAddon(zipURL: String, pluginName: String, sourceURL: String, force: Boolean = false) {
    Thread {
        val filename = zipURL.substringAfterLast('/')
        val pluginPath = Addons.addonsPath.resolve(pluginName)

        if (force || !pluginPath.exists()) {
            try {
                val connection = URL(zipURL).openConnection() as HttpURLConnection
                connection.inputStream.use { input ->
                    val cache = Addons.addonsPath.resolve("../cache")
                    cache.deleteRecursively()
                    cache.mkdir()
                    val zipFile = cache.resolve(filename)
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw DownloadError("Failed to download $zipURL. Status code: ${connection.responseCode}")
                    }

                    try {
                        if (pluginPath.exists()) {
                            pluginPath.deleteRecursively()
                        }

                        unzip(zipFile, cache)

                        val extractedFolder = cache.resolve(getRootDirectoryName(zipFile)!!)
                        extractedFolder.renameTo(pluginPath)

                        val pluginData = PluginData(zipURL, pluginName, sourceURL)
                        pluginPath.resolve("addon.json").writeText(Json.encodeToString(pluginData))

                        Log.d("D&E","Plugin $pluginName extracted successfully.")

                        installDependencies(pluginPath)

                    } catch (e: Exception) {
                        throw ExtractionError("Failed to extract ${zipFile.toPath()}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error occurred while downloading and extracting plugin: $zipURL $pluginName $sourceURL \n ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("Plugin $pluginName already exists. Skipping download.")
        }
    }.apply { start(); join() }
}
