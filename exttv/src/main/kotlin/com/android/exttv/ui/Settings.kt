package com.android.exttv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import com.android.exttv.model.data.Setting
import com.android.exttv.model.data.Setting.*
import com.android.exttv.model.manager.AddonManager
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.model.manager.readSettingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ButtonDefaults


@Composable
fun SettingsWindow() {
    if (StatusManager.showSettingsDialog) {
        var isFocusAssigned by remember { mutableStateOf(false) }
        var focusedIndex by remember { mutableIntStateOf(-1) }

        val pluginName = AddonManager.getIdByName(AddonManager[StatusManager.focusedContextIndex])

        val settings = readSettingValues(pluginName.toString())

        val scrollState = rememberScrollState()

        val settingListList = mutableListOf<MutableList<Setting>>()
        var currentSublist = mutableListOf<Setting>()

        settings.categories.forEach { category ->
            category.settings.forEach { setting ->
                when (setting) {
                    is LsepSetting -> {
                        currentSublist.add(setting)
                    }
                    else -> {
                        currentSublist.add(setting)
                        settingListList.add(currentSublist)
                        currentSublist = mutableListOf()
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxSize(0.75f),
                shape = MaterialTheme.shapes.medium,
                colors = SurfaceDefaults.colors(containerColor = Color(0xFF111111))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Settings:",
                        color = Color.White,
                        fontSize = 20.sp,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        settingListList.forEachIndexed() { index, settingList ->
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(Unit) {
                                if (index == 0 && !isFocusAssigned) {
                                    focusedIndex = 0
                                    focusRequester.requestFocus()
                                    isFocusAssigned = true
                                }

//                                if(index == focusedIndex){
//                                    scrollState.scrollTo(index)
//                                }
                            }

                            var backgroundColor by remember { mutableStateOf( Color(0x00000000) )}
                            var modifier = Modifier
                                .focusable()
                                .focusRequester(focusRequester)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(backgroundColor)
                                .onKeyEvent { event ->
                                    if (index == 0) {
                                        if (event.key == Key.DirectionUp) {
                                            return@onKeyEvent true
                                        }
                                    }
                                    when (event.key) {
                                        Key.DirectionLeft -> { return@onKeyEvent true }
                                        Key.DirectionRight -> { return@onKeyEvent true }
                                    }
                                    if (event.type == KeyEventType.KeyUp) {
                                        when (event.key) {
                                            Key.DirectionUp -> {
                                                var prevIndex = index - 1
                                                while (prevIndex >= 0 && settingListList[prevIndex].lastOrNull { it.visible == "true" || it.visible == "" } == null) {
                                                    prevIndex--
                                                }
                                                if (prevIndex >= 0) {
                                                    focusedIndex = prevIndex
                                                }
                                                return@onKeyEvent true
                                            }

                                            Key.DirectionDown -> {
                                                var nextIndex = index + 1
                                                while (nextIndex < settingListList.size && settingListList[nextIndex].lastOrNull { it.visible == "true" || it.visible == "" } == null) {
                                                    nextIndex++
                                                }
                                                if (nextIndex < settingListList.size) {
                                                    focusedIndex = nextIndex
                                                }
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    return@onKeyEvent true
                                }

                            Box {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                )
                                {
                                    for(setting in settingList) {
                                        if(setting is LsepSetting)
                                            LineSeparatorSetting(setting)
                                        else{
                                            if (setting.visible == "true" || setting.visible == "") {
                                                Box(
                                                    modifier = modifier
                                                        .fillMaxWidth()
                                                ) {
                                                    Text(text = setting.label,
                                                        modifier = Modifier.align(Alignment.CenterStart),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White)

                                                    Box(
                                                        modifier = Modifier.align(Alignment.CenterEnd),
                                                    ) {
                                                        when (setting) {
                                                            is TextSetting -> StringSetting(setting)
                                                            is BoolSetting -> BooleanSetting(setting)
                                                            is SelectSetting -> SelectSetting(setting)
                                                            is SliderSetting -> SliderSetting(setting)
                                                            is ActionSetting -> ActionSetting(setting)
                                                            is FolderSetting -> FolderSetting(setting)
                                                            else -> Text(
                                                                "Unsupported setting type: ${setting::class.simpleName}",
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(focusedIndex) {
                                backgroundColor = if(focusedIndex == index) Color(0x33333333) else Color(0x00000000)
                                if (index == focusedIndex) {
                                    focusRequester.requestFocus()
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LineSeparatorSetting(setting: LsepSetting) {
    Text(
        text = setting.label,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White
    )
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun StringSetting(setting: TextSetting) {
    var value by remember { mutableStateOf(setting.currentValue) }
    var showdText by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf(setting.currentValue) }

    Button(
        shape = ButtonDefaults.shape(shape = RectangleShape),
        onClick = { showdText = !showdText },
        enabled = setting.enable
    ) {
        Text(text = currentText)
    }

    if (showdText) {
        Dialog(
            onDismissRequest = { showdText = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
            ) {
                TextField(
                    value = value,
                    onValueChange = {
                        value = it
                        setting.currentValue = it
                    },
                    enabled = setting.enable,
                    label = { Text(text = setting.label) }
                )
            }
        }
    }
}


@Composable
fun BooleanSetting(setting: BoolSetting) {
    var value by remember { mutableStateOf(setting.currentValue) }
    Switch(
        checked = value == true,
        onCheckedChange = {
            value = it
            setting.currentValue = it
        },
        enabled = setting.enable
    )
}

@Composable
fun SliderSetting(setting: SliderSetting) {
    var value by remember { mutableStateOf(setting.currentValue) }
    val (start, step, stop) = setting.range.split(",").map { it.toInt() }
    val sliderRange = start.toFloat()..stop.toFloat()

    Slider(
        value = value.toFloat(),
        onValueChange = {
            // Snap to the nearest step
            val snappedValue = ((it - start) / step).toInt() * step + start
            value = snappedValue.coerceIn(start, stop)
            setting.currentValue = value
        },
        valueRange = sliderRange,
        steps = ((stop - start) / step) - 1,
//            enabled = setting.enable
    )
    Text(
        text = "Current Value: $value",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun SelectSetting(setting: SelectSetting) {
    var value by remember { mutableStateOf(setting.currentValue) }
    var showdDropDown by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Select an option") }

    Button(
        onClick = { showdDropDown = !showdDropDown },
        enabled = setting.enable
    ) {
        Text(text = selectedOption)
    }

    if (showdDropDown) {
        Dialog(
            onDismissRequest = { showdDropDown = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(200.dp)
                        .fillMaxWidth()
                ) {
                    setting.values.forEach { option ->
                        Text(
                            text = option.toString(),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    selectedOption = option.toString()
                                    showdDropDown = false
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionSetting(setting: ActionSetting) {
    Button(
        onClick = {},
        enabled = setting.enable
    ) {
        Text(text = setting.label)
    }
}

@Composable
fun FolderSetting(setting: FolderSetting) {
    var folderPath by remember { mutableStateOf(setting.default) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = folderPath ?: "",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
            },
            enabled = setting.enable
        ) {
            Text(text = "Select")
        }
    }
}
