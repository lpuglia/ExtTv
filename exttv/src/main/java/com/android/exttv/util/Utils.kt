package com.android.exttv.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.android.exttv.model.manager.AddonManager
import com.android.exttv.model.manager.FavouriteManager
import com.android.exttv.model.manager.StatusManager.drawerItems
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.GzipSource
import okio.buffer
import com.android.exttv.model.manager.AddonManager as Addons
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import com.android.exttv.model.manager.StatusManager as Status

fun updateSection() {
    drawerItems = AddonManager.getAllAddonNames() + FavouriteManager.getAllFavouriteNames() +
            listOf("Add from Repository", "Add from GitHub")
}

object ToastUtils {
    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        handler.post {
            Toast.makeText(Status.appContext, message, duration).show()
        }
    }
}

object IntentUtils {
    @JvmStatic
    fun fireMagnetIntent(magnetUri: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(magnetUri)
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            Status.appContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(
                "No application found to open the magnet.\nSupported applications: Amnis, Splayer, Stremio, ...",
                Toast.LENGTH_LONG
            )
        }
    }

    @JvmStatic
    fun executeStartActivity(command: String) {
        Log.d("python", "Executing command: $command")
        val parts = command.removeSurrounding("StartAndroidActivity(", ")")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }

        // Check that we have exactly 4 parameters
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid command format")
        }
        var (_, action, _, url) = parts
        when (action) {
            "android.intent.action.VIEW" -> {
                action = Intent.ACTION_VIEW
            }

            else -> {
                throw IllegalArgumentException("Invalid action")
            }
        }

        val intent = Intent(action).apply {
            data = Uri.parse(url)
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Status.appContext.startActivity(intent)
    }
}

fun clientFactory(headers: Map<String, String>): OkHttpDataSource.Factory {
    val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val newRequestBuilder = chain.request().newBuilder()
            for ((key, value) in headers) {
                newRequestBuilder.header(key, value)
            }
            chain.proceed(newRequestBuilder.build())
        }
        .addInterceptor { chain ->
            val request = chain.request()
            val originalResponse = chain.proceed(request)
            val contentEncoding = originalResponse.header("Content-Encoding")

            if (contentEncoding != null && contentEncoding.equals("gzip", ignoreCase = true)) {
                val responseBody = originalResponse.body
                val gzipSource = GzipSource(responseBody!!.source())
                val decompressedBody = ResponseBody.create(responseBody.contentType(), -1, gzipSource.buffer())

                originalResponse.newBuilder()
                    .header("Content-Encoding", "identity")
                    .removeHeader("Content-Length")
                    .body(decompressedBody)
                    .build()
            } else {
                originalResponse
            }

        }
//        if (requiresProxy) initClientProxy(clientBuilder)
//        val cookieManager = CookieManager()
//        val cookieJar = object : CookieJar {override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
//            for (cookie in cookies) {
//                cookieManager.cookieStore.add(url.toUri(), cookie)
//            }
//        }
//        }
//        clientBuilder.cookieJar(cookieJar)
    return OkHttpDataSource.Factory(clientBuilder.build())
}

fun parseText(input: String): AnnotatedString {
    val stripped = input.replace("\n", " ◆ ").trim()//.replace(Regex(";\\s*;+\\s*"), ";")
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

fun stripTags(input: String): String {
    val regex = "\\[(B|I|LIGHT|UPPERCASE|LOWERCASE|CAPITALIZE)](.*?)\\[/\\1]".toRegex()
    return regex.replace(input) { matchResult ->
        matchResult.groupValues[2] // Extract the content without tags
    }.replace("\n", " ◆ ") // Replace newlines with a diamond
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
        val pattern = Pattern.compile("""name="([^"]+)"""")
        val matcher = pattern.matcher(addonXml)
        val pluginName = if (matcher.find()) {
            matcher.group(1)
        } else {
            throw Exception("Failed to get plugin name")
        }

        // Extract plugin id
        val patternId = Pattern.compile("""id="([^"]+)"""")
        val matcherId = patternId.matcher(addonXml)
        val pluginId = if (matcherId.find()) {
            matcherId.group(1)
        } else {
            throw Exception("Failed to get plugin id")
        }

        installAddon(zipPath, pluginId, zipPath, false)
        return pluginName
    } catch (e: Exception) {
        throw Exception("Error occurred while processing URL: ${e.message}", e)
    }
}

fun getLatestZipName(url: String): Triple<String, String, String>{
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

            val pluginId = data.getJSONObject("result")
                .getJSONObject("data")
                .getJSONObject("addon")
                .getString("addonid")

            val pluginName = data.getJSONObject("result")
                .getJSONObject("data")
                .getJSONObject("addon")
                .getString("name")
            return Triple(zipPath, pluginId, pluginName)
        }
    } catch (e: Exception) {
        throw Exception("Failed to fetch or decode JSON data: ${e.message}", e)
    }
}

fun getFromRepository(addonId: String, force: Boolean = false): String {
    val url = "https://kodi.tv/page-data/addons/omega/${addonId}/page-data.json"
    val (zipPath, pluginId, pluginName) = getLatestZipName(url)
    val mirrorZip = "https://mirrors.kodi.tv/addons/omega/" + zipPath.split("addons/omega/")[1]
    installAddon(mirrorZip, pluginId, url, false)
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
//            Status.showToast("Dependency found: $addonName, installing...", Toast.LENGTH_SHORT)

            try{
                getFromRepository(addonName, false)
            }catch (e: Exception) {
                ToastUtils.showToast("Error while installing dependency $addonName: ${e.message}", Toast.LENGTH_SHORT)
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
//                ToastUtils.showToast("Installing $pluginName", Toast.LENGTH_SHORT)
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

// Helper function to convert Drawable to Bitmap
fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}