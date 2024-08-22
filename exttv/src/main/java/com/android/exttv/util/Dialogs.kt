package com.android.exttv.util

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.android.exttv.MainActivity
import com.android.exttv.manager.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.StatusManager as Status

@Composable
fun UninstallDialog(indexAddon: Int) {
    if (Status.showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { Status.showUninstallDialog = false },
            title = { Text(text = "Uninstall") },
            text = { Text(text = "Do you want to uninstall ${Addons.get(indexAddon)}") },
            confirmButton = {
                Button(
                    onClick = {
                        Addons.uninstallAddon(indexAddon)
                        Status.showUninstallDialog = false
                    }
                ) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                Button(onClick = { Status.showUninstallDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UpdateDialog(
    context: MainActivity,
    indexAddon: Int
) {
    if (Status.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { Status.showUpdateDialog = false },
            title = { Text(text = "Update") },
            text = { Text(text = "Do you want to update ${Addons.get(indexAddon)}") },
            confirmButton = {
                Button(
                    onClick = {
                        val json = Addons.addonsPath.resolve("${Addons.get(indexAddon)}/addon.json").readText()
                        val data = Json.decodeFromString(PluginData.serializer(), json)
                        if(data.sourceURL == data.zipURL){
                            val regex = Regex("""https://github\.com/([^/]+)/([^/]+)/archive/refs/heads/([^/]+)\.zip""")
                            val matchResult = regex.find(data.zipURL)
                            if (matchResult != null) {
                                val owner = matchResult.groupValues[1]
                                val repository = matchResult.groupValues[2]
                                val branch = matchResult.groupValues[3]
                                Addons.installAddon("$owner/$repository/$branch", true)
                            }
                        }else{
                            val (zipPath, _) = getLatestZipName(data.sourceURL)
                            if(zipPath!=data.zipURL) { // update if different zip name
                                context.showToast("Installing updates", Toast.LENGTH_SHORT)
                                Addons.installAddon(data.sourceURL, true)
                            }else{
                                context.showToast("No updates available", Toast.LENGTH_SHORT)
                            }
                        }
                        Status.showUpdateDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                Button(onClick = { Status.showUpdateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun fetchUrlContent(url: String): String? {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            response.body?.string()
        }
    } catch (e: IOException) {
        Log.e("RepositoryDialog", "Failed to fetch data", e)
        null
    }
}

@Composable
fun RepositoryDialog(
    context: MainActivity,
) {
    Status.loadingState = LoadingStatus.FETCHING_ADDON
    val videoAddons = mutableListOf<Addons.Addon>()
    val url = "https://kodi.tv/page-data/addons/omega/search/page-data.json"
    val coroutineScope = rememberCoroutineScope()

    val jsonData = fetchUrlContent(url)
    if (jsonData != null) {
        val jsonObject = JSONObject(jsonData)
        val result = jsonObject.getJSONObject("result").getJSONObject("data")
        val allAddons = result.getJSONObject("allAddon").getJSONArray("nodes")

        for (i in 0 until allAddons.length()) {
            val addon = allAddons.getJSONObject(i)
            val categories = addon.getJSONArray("categories")

            for (j in 0 until categories.length()) {
                val category = categories.getJSONObject(j).getString("name")
                if (category == "Video addons") {
                    val videoAddon = Addons.Addon(
                        addonid = addon.getString("addonid"),
                        name = addon.getString("name"),
                        icon = addon.getString("icon"),
                        description = addon.getString("description")
                    )
                    videoAddons.add(videoAddon)
                    break
                }
            }
        }
    } else {
        context.showToast("Failed to fetch addons", Toast.LENGTH_LONG)
        Status.showRepositoryDialog = false
    }
    Status.loadingState = LoadingStatus.DONE

    Dialog(onDismissRequest = {
        Status.loadingState = LoadingStatus.DONE
        Status.showRepositoryDialog = false
    }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(16.dp)
                .width(450.dp)
                .height(500.dp),
            tonalElevation = 8.dp,
            colors = SurfaceDefaults.colors(containerColor = Color(0xA30F2B31))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(), // Make sure the Box takes the full width
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select an Addon:",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            val listState = rememberTvLazyListState()
            val focusRequester = remember {FocusRequester()}

            LaunchedEffect(Unit) {
               focusRequester.requestFocus()
            }
            TvLazyColumn(
                state = listState,
                modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xA30F2B31))
                            .padding(top = 38.dp)
                            .padding(horizontal = 23.dp)
            ) {
                itemsIndexed(videoAddons) { index, addon ->
                    Card(
                        onClick = {
                            Status.showRepositoryDialog = false
                            Status.loadingState = LoadingStatus.SELECTING_SECTION
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    Addons.installAddon("https://kodi.tv/page-data/addons/omega/${addon.addonid}/page-data.json")
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(100.dp)
                            .let { if (index==0) it.focusRequester(focusRequester) else it },
//                            .clickable {
//                                selectedItem = addon
//                            },
                        colors = CardDefaults.colors(containerColor = Color(0xCB2B474D)),
                    ){
                        AddonBox(addon)
                    }
                }
            }
        }
    }
}

@Composable
fun AddonBox(addon: Addons.Addon) {
    Box(
        modifier = Modifier
            .size(width = 400.dp, height = 100.dp)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = "https://kodi.tv/${addon.icon}",
                contentDescription = addon.description,
                modifier = Modifier
                    .size(95.dp) // Set a fixed size for the image
                    .padding(2.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = addon.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = addon.description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 2, // Limit to 2 lines to fit in the space
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}


@Composable
fun GithubDialog() {
    var username by remember { mutableStateOf("") }
    var repoName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }

    val textFieldValues = listOf(
        Pair("Username", username) to { newValue: String -> username = newValue },
        Pair("Repository", repoName) to { newValue: String -> repoName = newValue },
        Pair("Branch", branchName) to { newValue: String -> branchName = newValue }
    )

    val focusRequesters = List(textFieldValues.size) { FocusRequester() }
    val saveButtonRequester = FocusRequester()
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = {
        Status.loadingState = LoadingStatus.DONE
        Status.showGithubDialog = false
    }) {
        Surface(
            modifier = Modifier.background(Color.White),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Enter GitHub Details")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textFieldValues.forEachIndexed { index, (labelValue, onValueChange) ->
                        val (label, value) = labelValue
                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            label = { Text(label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = if (index < textFieldValues.size - 1) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (index < focusRequesters.size - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                },
                                onDone = {
                                    saveButtonRequester.requestFocus()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[index])
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.Tab -> {
                                                if (index < focusRequesters.size - 1) {
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { Status.showGithubDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            Status.showGithubDialog = false
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val owner = "kodiondemand"
                                    val repo = "addon"
                                    val branch = "master"
                                    val url = "$owner/$repo/$branch"
                                    Addons.installAddon(url)
                                }
                            }
                        },
                        modifier = Modifier.focusRequester(saveButtonRequester)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}