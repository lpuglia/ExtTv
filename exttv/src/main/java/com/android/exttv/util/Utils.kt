package com.android.exttv.util

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

        downloadAndExtractPlugin(zipPath, pluginName, force)
        return pluginName
    } catch (e: Exception) {
        throw Exception("Error occurred while processing URL: ${e.message}", e)
    }
}

fun getFromRepository(url: String, force: Boolean = false): String {
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

            downloadAndExtractPlugin(zipPath, pluginName, force)
            return pluginName
        }
    } catch (e: Exception) {
        throw Exception("Failed to fetch or decode JSON data: ${e.message}", e)
    }
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

fun downloadAndExtractPlugin(url: String, pluginName: String, force: Boolean = false) {
    Thread {
        val filename = url.substringAfterLast('/')
        val pluginPath = Addons.addonsPath.resolve(pluginName)

        if (force || !pluginPath.exists()) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.inputStream.use { input ->
                    val cache = Addons.addonsPath.resolve("../cache")
                    cache.deleteRecursively()
                    cache.mkdir()
                    val zipFile = cache.resolve(filename)
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw DownloadError("Failed to download $url. Status code: ${connection.responseCode}")
                    }

                    Log.d("D&E","Downloaded ${zipFile.toPath()} successfully")

                    try {
                        if (pluginPath.exists()) {
                            pluginPath.deleteRecursively()
                        }

                        unzip(zipFile, cache)

                        val extractedFolder = cache.resolve(getRootDirectoryName(zipFile)!!)
                        extractedFolder.renameTo(pluginPath)
                        Log.d("D&E","Plugin $pluginName extracted successfully.")
                    } catch (e: Exception) {
                        throw ExtractionError("Failed to extract ${zipFile.toPath()}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error occurred while downloading and extracting plugin: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("Plugin $pluginName already exists. Skipping download.")
        }
        Addons.add(pluginName)
    }.apply { start(); join() }
}
