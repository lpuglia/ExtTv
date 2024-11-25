package com.android.exttv.ui

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
import com.android.exttv.model.manager.SectionManager
import com.android.exttv.model.manager.AddonManager as Addons
import com.android.exttv.model.manager.StatusManager as Status

var uninstallReqs = mutableListOf<FocusRequester>()
var updateReqs = mutableListOf<FocusRequester>()
var settingReqs = mutableListOf<FocusRequester>()
var removeFavReqs = mutableListOf<FocusRequester>()

@Composable
fun FavouriteButtons(
    favouriteIndex: Int
){
    Row(
        Modifier.height(50.dp).width(if(Status.focusedContextIndex==Addons.size+favouriteIndex) 60.dp else 0.dp)
    ) {
        if(favouriteIndex==0) removeFavReqs.clear()
        removeFavReqs.add(FocusRequester())

        Button(
            onClick = {
                Status.showRemoveDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp)
                .focusRequester(removeFavReqs.last())
                .onKeyEvent { event -> removeFavButtonKE(event, favouriteIndex) },
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
            Status.focusedContextIndex = -1
        }
    }
    return false
}



@Composable
fun ContextButtons(
    addonIndex: Int,
) {
    Row(
        Modifier.width(if (Status.focusedContextIndex == addonIndex) 180.dp else 0.dp,)
    ) {
        if(addonIndex==0) {
            settingReqs.clear()
            updateReqs.clear()
            uninstallReqs.clear()
        }
        uninstallReqs.add(FocusRequester())
        updateReqs.add(FocusRequester())
        settingReqs.add(FocusRequester())
        Button(
            onClick = {
                Status.showUninstallDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp).height(50.dp)
                .focusRequester(uninstallReqs.last())
                .onKeyEvent { event -> uninstallButtonKE(event, addonIndex) },
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
//        println("tFocusRequester assigned: ${System.identityHashCode(tFocusRequester)}")
        Button(
            onClick = {
                Status.showUpdateDialog = true
            },
            modifier = Modifier
                .padding(end = 10.dp)
                .width(50.dp).height(50.dp)
                .focusRequester(updateReqs.last())
                .onKeyEvent { event -> updateButtonKE(event, addonIndex) },
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
                .focusRequester(settingReqs.last())
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
            Status.focusedContextIndex = -1
        } Key.DirectionRight -> {
            updateReqs[addonIndex].requestFocus()
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
            Status.focusedContextIndex = -1
        } Key.DirectionLeft -> {
            uninstallReqs[addonIndex].requestFocus()
            return true
        } Key.DirectionRight -> {
            settingReqs[addonIndex].requestFocus()
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
            Status.focusedContextIndex = -1
        } Key.DirectionLeft -> {
            updateReqs[addonIndex].requestFocus()
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
        if(itemIndex < Addons.size){
            settingReqs[itemIndex].requestFocus()
        }else{
            removeFavReqs[itemIndex-Addons.size].requestFocus()
        }
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