package com.android.exttv.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
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
import com.android.exttv.models.LoadingStatus
import com.android.exttv.utils.PluginData
import com.android.exttv.utils.getFromGit
import com.android.exttv.utils.getFromRepository
import com.android.exttv.utils.getLatestZipName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import com.android.exttv.models.AddonManager as Addons
import com.android.exttv.models.FavouriteManager as Favourites
import com.android.exttv.models.StatusManager as Status
import com.android.exttv.models.PythonManager as Python
import com.android.exttv.models.SectionManager as Sections

@Composable
fun NewPlaylistMenu() {
    if (!Status.showNewPlaylistMenu) return
    val cardItem = Sections.getFocusedCard()
    var playlistName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = { Status.showNewPlaylistMenu = false }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter Playlist Name",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = playlistName,
                    onValueChange = { newName -> playlistName = newName },
                    placeholder = { Text("New Playlist") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.Enter -> {
                                        Favourites.addCardOrCreateFavourite(playlistName, cardItem)
                                        Status.showNewPlaylistMenu = false // Close dialog after creation
                                        Status.showFavouriteMenu = false
                                        focusManager.clearFocus() // Clear focus
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            Favourites.addCardOrCreateFavourite(playlistName, cardItem)
                            Status.showNewPlaylistMenu = false // Close dialog after creation
                            Status.showFavouriteMenu = false
                            focusManager.clearFocus() // Clear focus
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Request focus when the dialog is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun OptionItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        color = Color.Black
    )
}

@Composable
fun FavouriteMenu() {
    if (!Status.showFavouriteMenu) return
    Dialog(onDismissRequest = { Status.showFavouriteMenu = false }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.onKeyEvent { keyEvent ->
                // Consume the enter key event on key up to prevent accidental clicks
                keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp
            }.padding(16.dp)) {
                OptionItem(
                    text = "Add to new playlist",
                    onClick = {
                        if(Status.reboundEnter){
                            Status.reboundEnter = false
                        }else {
                            Status.showNewPlaylistMenu = true
                        }
                    }
                )
                OptionItem(text = "Add content to new playlist", onClick = {})
                Favourites.getAllFavouriteNames().forEach { name ->
                    OptionItem(text = "Add to $name", onClick = {
                        Favourites.addCardOrCreateFavourite(name, Sections.getFocusedCard())
                        Status.showFavouriteMenu = false
                    })
                    OptionItem(text = "Add content to $name", onClick = {})
                }
            }
        }
    }
}

@Composable
fun RemoveDialog() {
    if (!Status.showRemoveDialog) return
    val favouriteIndex = Status.focusedContextIndex-Addons.size
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {focusRequester.requestFocus()}

    AlertDialog(
        onDismissRequest = { Status.showRemoveDialog = false },
        title = { Text(text = "Remove") },
        text = { Text(text = "Do you want to remove ${Favourites[favouriteIndex]}") },
        confirmButton = {
            Button(
                onClick = {
                    Status.showRemoveDialog = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            Status.focusedContextIndex = -1
                            if (Status.selectedIndex == favouriteIndex+Addons.size) {
                                Status.selectedIndex = -1
                                Sections.clearSections()
                            } else if (Status.selectedIndex > favouriteIndex+Addons.size) {
                                Status.selectedIndex -= 1
                            }
                            Favourites.deleteFavourite(favouriteIndex)
                            Status.update()
                        }
                    }
                }
            ) { Text("Remove") }
        },
        dismissButton = {
            Button(
                onClick = { Status.showRemoveDialog = false },
                modifier = Modifier.focusRequester(focusRequester) // Assign the focusRequester to the Cancel button
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun UninstallDialog() {
    if (!Status.showUninstallDialog) return
    val indexAddon = Status.focusedContextIndex
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {focusRequester.requestFocus()}

    AlertDialog(
        onDismissRequest = { Status.showUninstallDialog = false },
        title = { Text(text = "Uninstall") },
        text = { Text(text = "Do you want to uninstall ${Addons[indexAddon]}") },
        confirmButton = {
            Button(
                onClick = {
                    Status.showUninstallDialog = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            Status.focusedContextIndex = -1
                            if (Status.selectedIndex == indexAddon) {
                                Status.selectedIndex = -1
                                Sections.clearSections()
                            } else if (Status.selectedIndex > indexAddon) {
                                Status.selectedIndex -= 1
                            }
                            Addons.uninstallAddon(indexAddon)
                            Status.update()
                        }
                    }
                }
            ) { Text("Uninstall") }
        },
        dismissButton = {
            Button(
                onClick = { Status.showUninstallDialog = false },
                modifier = Modifier.focusRequester(focusRequester) // Assign the focusRequester to the Cancel button
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun UpdateDialog() {
    if (!Status.showUpdateDialog) return
    val indexAddon = Status.focusedContextIndex
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {focusRequester.requestFocus()}

    AlertDialog(
        onDismissRequest = { Status.showUpdateDialog = false },
        title = { Text(text = "Update") },
        text = { Text(text = "Do you want to update ${Addons[indexAddon]}") },
        confirmButton = {
            Button(
                onClick = {
                    val json = Addons.addonsPath.resolve("${Addons[indexAddon]}/addon.json").readText()
                    val data = Json.decodeFromString(PluginData.serializer(), json)
                    if(data.sourceURL == data.zipURL){
                        val regex = Regex("""https://github\.com/([^/]+)/([^/]+)/archive/refs/heads/([^/]+)\.zip""")
                        val matchResult = regex.find(data.zipURL)
                        if (matchResult != null) {
                            val owner = matchResult.groupValues[1]
                            val repository = matchResult.groupValues[2]
                            val branch = matchResult.groupValues[3]
                            // Addons.installAddon("$owner/$repository/$branch", true)
                            getFromGit("$owner/$repository/$branch", true)
                        }
                    }else{
                        val (zipPath, _) = getLatestZipName(data.sourceURL)
                        if(zipPath!=data.zipURL) { // update if different zip name
                            Status.showToast("Installing updates", Toast.LENGTH_SHORT)
                            // Addons.installAddon(data.sourceURL, true)
                            getFromRepository(data.pluginName, true)
                        }else{
                            Status.showToast("No updates available", Toast.LENGTH_SHORT)
                        }
                    }
                    Status.showUpdateDialog = false
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = { Status.showUpdateDialog = false },
                   modifier = Modifier.focusRequester(focusRequester) // Assign the focusRequester to the Cancel button
            ){
                Text("Cancel")
            }
        }
    )
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
        Log.e("RepositoryDialog", "Failed to fetch data, check your connection!", e)
        null
    }
}

data class Addon(
    val addonid: String,
    val name: String,
    val icon: String,
    val description: String
)

@Composable
fun RepositoryDialog() {
    if (!Status.showRepositoryDialog) return
    Status.loadingState = LoadingStatus.FETCHING_ADDON
    val videoAddons = mutableListOf<Addon>()
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
                    val videoAddon = Addon(
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
        Status.showToast("Failed to fetch addons", Toast.LENGTH_LONG)
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
                                    Status.loadingState = LoadingStatus.INSTALLING_ADDON
                                    Status.focusedContextIndex = -1

                                    val pluginName = getFromRepository(addon.addonid)//Addons.installAddon(addon.addonid)
                                    Status.update()
                                    Python.selectAddon(pluginName)
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(100.dp)
                            .let {
                                    if (index == 0){
                                        LaunchedEffect(Unit) {
                                           focusRequester.requestFocus()
                                        }
                                        it.focusRequester(focusRequester)
                                    }else it
                                 },
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
fun AddonBox(addon: Addon) {
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
    if(!Status.showGithubDialog) return
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

    Dialog(
        onDismissRequest = {
            Status.loadingState = LoadingStatus.DONE
            Status.showGithubDialog = false
        }
    ) {
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

                                    Status.loadingState = LoadingStatus.INSTALLING_ADDON
                                    Status.focusedContextIndex = -1

//                                    val pluginName = Addons.installAddon(url)
                                    val pluginName = getFromGit(url, true)

                                    Status.update()
                                    Python.selectAddon(pluginName)
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