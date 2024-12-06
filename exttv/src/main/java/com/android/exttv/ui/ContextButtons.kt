package com.android.exttv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import com.android.exttv.model.manager.SectionManager
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.model.manager.AddonManager as Addons
import com.android.exttv.model.manager.StatusManager as Status

@Composable
fun FavouriteButtons(
    favouriteIndex: Int
){
    val removeFR = FocusRequester()
    var width by remember { mutableStateOf(0.dp) }
    width = if(Status.focusedContextIndex==Addons.size+favouriteIndex) 280.dp else 0.dp
    Row(
        Modifier
            .width(width)
            .height(56.dp)
            .zIndex(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = {
                Status.showRemoveDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .height(50.dp)
                .focusRequester(removeFR)
                .onKeyEvent { event -> removeFavButtonKE(event) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF000000),
                focusedContainerColor = Color(0xFF333333)
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Favorite Icon",
                modifier = Modifier.size(60.dp),
                tint = Color.White,
            )
        }
    }
    LaunchedEffect(width) {
        if(width==280.dp){
            removeFR.requestFocus()
        }
    }
}

fun removeFavButtonKE(
    event: KeyEvent
): Boolean{
    if (event.type == KeyEventType.KeyUp){ return true }

    when (event.key) {
        Key.DirectionLeft, Key.DirectionUp, Key.DirectionDown, Key.DirectionRight -> {
            Status.focusedContextIndex = -1
            StatusManager.refocus = true
        }
    }
    return false
}



@Composable
fun ContextButtons(
    addonIndex: Int,
) {
    val uninstallReqs = FocusRequester()
    val updateReqs = FocusRequester()
    val settingReqs = FocusRequester()
    var width by remember { mutableStateOf(0.dp) }
    width = if(Status.focusedContextIndex==addonIndex) 280.dp else 0.dp

    Row(
        Modifier
            .width(width)
            .height(56.dp)
            .zIndex(1f),
//            .background(Color.Red),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                Status.showUninstallDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .height(50.dp)
                .focusRequester(uninstallReqs)
                .onKeyEvent { event -> uninstallButtonKE(event, addonIndex, updateReqs) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF000000),
                focusedContainerColor = Color(0xFF333333)
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Favorite Icon",
                modifier = Modifier.size(60.dp),
                tint = Color.White,
            )
        }
//        println("tFocusRequester assigned: ${System.identityHashCode(tFocusRequester)}")
        Button(
            onClick = {
                Status.showUpdateDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .height(50.dp)
                .focusRequester(updateReqs)
                .onKeyEvent { event -> updateButtonKE(event, addonIndex, uninstallReqs, settingReqs) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF000000),
                focusedContainerColor = Color(0xFF333333)
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Favorite Icon",
                modifier = Modifier.size(60.dp),
                tint = Color.White,
            )
        }
        Button(
            onClick = {
                Status.showSettingsDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .height(50.dp)
                .focusRequester(settingReqs)
                .onKeyEvent { event -> settingButtonKE(event, addonIndex, updateReqs) },
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF000000),
                focusedContainerColor = Color(0xFF333333)
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
    LaunchedEffect(width) {
        if(width==280.dp){
            settingReqs.requestFocus()
        }
    }

}

fun uninstallButtonKE(
    event: KeyEvent,
    addonIndex: Int,
    updateReqs: FocusRequester,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
      (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionLeft -> {
            Status.focusedContextIndex = -1
            Status.refocus = true
        }
        Key.DirectionUp, Key.DirectionDown -> {
            Status.focusedContextIndex = -1
        } Key.DirectionRight -> {
            updateReqs.requestFocus()
            return true
        }
    }
    return false
}

fun updateButtonKE(
    event: KeyEvent,
    addonIndex: Int,
    uninstallReqs: FocusRequester,
    settingReqs: FocusRequester,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
      (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionUp, Key.DirectionDown -> {
            Status.focusedContextIndex = -1
        } Key.DirectionLeft -> {
            uninstallReqs.requestFocus()
            return true
        } Key.DirectionRight -> {
            settingReqs.requestFocus()
            return true
        }
    }
    return false
}

fun settingButtonKE(
    event: KeyEvent,
    addonIndex: Int,
    updateReqs: FocusRequester,
): Boolean{
    if (event.type == KeyEventType.KeyUp ||
       (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionUp, Key.DirectionDown, Key.DirectionRight -> {
            Status.focusedContextIndex = -1
            StatusManager.refocus = true
        } Key.DirectionLeft -> {
            updateReqs.requestFocus()
            return true
        }
    }
    return false
}

fun addonKE(
    event: KeyEvent,
    itemIndex: Int,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
      (itemIndex == 0 && event.key == Key.DirectionUp)){ return true }
    if (event.key == Key.DirectionLeft) {
        Status.focusedContextIndex = itemIndex
        return true
    }
    if (event.key == Key.DirectionRight) {
        SectionManager.refocusCard()
        return true
    }
    return false
}

fun nonAddonKE(
    event: KeyEvent
): Boolean {
    if (event.key == Key.DirectionRight) {
        SectionManager.refocusCard()
        return true
    }
    return event.key == Key.DirectionLeft
}