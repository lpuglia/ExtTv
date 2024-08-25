package com.android.exttv.util

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.FavouriteManager as Favourites
import com.android.exttv.manager.StatusManager as Status

object ContextManager {
    var drawerItemRequesters by mutableStateOf(listOf<FocusRequester>())
    var uninstallReqs by mutableStateOf(listOf<FocusRequester>())
    var updateReqs    by mutableStateOf(listOf<FocusRequester>())
    var settingReqs   by mutableStateOf(listOf<FocusRequester>())
    var removeFavReqs   by mutableStateOf(listOf<FocusRequester>())

    @Composable
    fun Init(){
        drawerItemRequesters = rememberUpdatedState(newValue = List(Addons.size()+Favourites.size()+2) { FocusRequester() }).value
        settingReqs   = rememberUpdatedState(newValue = List(Addons.size()) { FocusRequester() }).value
        updateReqs    = rememberUpdatedState(newValue = List(Addons.size()) { FocusRequester() }).value
        uninstallReqs = rememberUpdatedState(newValue = List(Addons.size()) { FocusRequester() }).value
        removeFavReqs = rememberUpdatedState(newValue = List(Favourites.size()) { FocusRequester() }).value
    }
}

@Composable
fun FavouriteButtons(
    itemIndex: Int,
    label: String,
){
    Row(
        Modifier.height(50.dp).width(if(Addons.focusedContextIndex==Addons.size()+itemIndex) 60.dp else 0.dp)
    ) {
        Button(
            onClick = {
                Favourites.deleteFavourite(label)
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .focusRequester(ContextManager.removeFavReqs[itemIndex])
                .onKeyEvent { event -> removeFavButtonKE(event, itemIndex)},
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
    }
}

fun removeFavButtonKE(
    event: KeyEvent,
    favouriteIndex: Int,
): Boolean{
    if (event.type == KeyEventType.KeyUp){ return true }

    when (event.key) {
        Key.DirectionLeft -> {
            return true
        }
        Key.DirectionUp, Key.DirectionDown, Key.DirectionRight -> {
            Addons.focusedContextIndex = -1
        }
    }
    return false
}

@Composable
fun ContextButtons(
    addonIndex: Int,
) {
    Row(
        Modifier.width(if(Addons.focusedContextIndex==addonIndex) 180.dp else 0.dp,)
    ) {
        Button(
            onClick = {
                Status.showUninstallDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp).height(50.dp)
                .focusRequester(ContextManager.uninstallReqs[addonIndex])
                .onKeyEvent { event -> uninstallButtonKE(event, addonIndex)},
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
            onClick = {
                Status.showUpdateDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp).height(50.dp)
                .focusRequester(ContextManager.updateReqs[addonIndex])
                .onKeyEvent { event -> updateButtonKE(event, addonIndex)},
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF1D2E31),
                focusedContainerColor = Color(0xFF2B474D)
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
            onClick = {},
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp).height(50.dp)
                .focusRequester(ContextManager.settingReqs[addonIndex])
                .onKeyEvent { event -> settingButtonKE(event, addonIndex) },
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

fun uninstallButtonKE(
    event: KeyEvent,
    addonIndex: Int,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
       event.key == Key.DirectionLeft   ||
      (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionUp, Key.DirectionDown -> {
            Addons.focusedContextIndex = -1
        } Key.DirectionRight -> {
            ContextManager.updateReqs[addonIndex].requestFocus()
            return true
        }
    }
    return false
}

fun updateButtonKE(
    event: KeyEvent,
    addonIndex: Int,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
      (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionUp, Key.DirectionDown -> {
            Addons.focusedContextIndex = -1
        } Key.DirectionLeft -> {
            ContextManager.uninstallReqs[addonIndex].requestFocus()
            return true
        } Key.DirectionRight -> {
            ContextManager.settingReqs[addonIndex].requestFocus()
            return true
        }
    }
    return false
}

fun settingButtonKE(
    event: KeyEvent,
    addonIndex: Int,
): Boolean{
    if (event.type == KeyEventType.KeyUp ||
       (addonIndex==0 && event.key == Key.DirectionUp)){ return true }

    when (event.key) {
        Key.DirectionUp, Key.DirectionDown, Key.DirectionRight -> {
            Addons.focusedContextIndex = -1
        } Key.DirectionLeft -> {
            ContextManager.updateReqs[addonIndex].requestFocus()
            return true
        }
    }
    return false
}

fun addonKE(
    event: KeyEvent,
    addonIndex: Int,
): Boolean {
    if(event.type == KeyEventType.KeyUp ||
      (addonIndex == 0 && event.key == Key.DirectionUp)){ return true }
    if (event.key == Key.DirectionLeft) {
        Addons.focusedContextIndex = addonIndex
        if(addonIndex < Addons.size()){
            ContextManager.settingReqs[addonIndex].requestFocus()
        }else{
            ContextManager.removeFavReqs[addonIndex-Addons.size()].requestFocus()
        }
        return true
    }
    return false
}

fun nonAddonKE(
    event: KeyEvent
): Boolean {
    return event.key == Key.DirectionLeft
}