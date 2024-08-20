package com.android.exttv.util

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import com.android.exttv.manager.AddonManager as Addon
import com.android.exttv.manager.StatusManager as Status

@Composable
fun UninstallSettingButtons(
    addonIndex: Int,
    item: Pair<String, ImageVector>,
    uninstallSettingsRequesters: List<FocusRequester>
) {
    val animatedDpList = animateDpAsState(
            targetValue = if(Addon.focusedIndex==addonIndex) 120.dp else 0.dp,
            label = "Animated Dp"
        ).value

    Row(
        Modifier
//            .align(Alignment.CenterVertically)
            .height(50.dp)
            .width(animatedDpList)
    ) {
        Button(
            onClick = {
                Status.showContextMenu = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .onFocusChanged{
                    Log.d("Focus", item.first)
                }
                .onKeyEvent { event -> uninstallButtonsKeyEvent(event, addonIndex, uninstallSettingsRequesters) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF1D2E31),
                focusedContainerColor = Color(0xFF2B474D)
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Favorite Icon",
                modifier = Modifier.size(60.dp),
                tint = Color.White,
            )
        }
        Button(
            onClick = {},
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .onFocusChanged{
                    Log.d("Focus", item.first)
                }
                .focusRequester(uninstallSettingsRequesters[addonIndex])
                .onKeyEvent { event -> settingButtonsKeyEvent(event, addonIndex) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF1D2E31),
                focusedContainerColor = Color(0xFF2B474D)
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Favorite Icon",
                modifier = Modifier.size(60.dp),
                tint = Color.White,
            )
        }
    }
}

fun uninstallButtonsKeyEvent(
    event: KeyEvent,
    addonIndex: Int,
    uninstallSettingsRequesters: List<FocusRequester>
): Boolean {
    if(
        addonIndex==0 && event.key == Key.DirectionUp //||
//        addonIndex==Status.uninstallSettingsState.size-1 && event.key == Key.DirectionDown
    ){
        return true
    } else if (event.key == Key.DirectionUp || event.key == Key.DirectionDown) {
        Addon.focusedIndex = -1
        return false
    }else if (event.key == Key.DirectionLeft){
        return true
    } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight){
        return true
    } else if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionRight){
        uninstallSettingsRequesters[addonIndex].requestFocus()
        return true
    }else{
        return false
    }
}

fun settingButtonsKeyEvent(
    event: KeyEvent,
    addonIndex: Int,
): Boolean{
    if(
        addonIndex==0 && event.key == Key.DirectionUp //||
    ){
        return true
    }else if (event.key == Key.DirectionUp || event.key == Key.DirectionDown || event.key == Key.DirectionRight) {
        Addon.focusedIndex = -1
    }
    return false
}

fun addonKeyEvent(
    event: KeyEvent,
    addonIndex: Int,
    uninstallSettingsRequesters: List<FocusRequester>
): Boolean {
    if (addonIndex == 0 && event.key == Key.DirectionUp){
        return true
    } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
        Addon.focusedIndex = addonIndex
        uninstallSettingsRequesters[addonIndex].requestFocus()
        return true
    } else if(event.type == KeyEventType.KeyUp && event.key == Key.DirectionLeft){
        return true
    } else {
        return false
    }
}


fun nonAddonKeyEvent(
    event: KeyEvent
): Boolean {
    return event.key == Key.DirectionLeft
}