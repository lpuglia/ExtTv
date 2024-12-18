package com.android.exttv.view

import PlayerView
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.MaterialTheme
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.data.ExtTvMediaSource
import com.android.exttv.model.manager.MediaSourceManager
import com.android.exttv.model.manager.PlayerManager
import com.android.exttv.model.manager.PythonManager
import com.android.exttv.model.manager.StatusManager
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (doubleBackToExitPressedOnce) {
                finish()
                return true
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, backPressInterval)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusManager.init(this, applicationContext)
//        scheduleSyncJob(applicationContext)
        PlayerManager.init(applicationContext)
    }

    override fun onRestart() {
        super.onRestart()
        StatusManager.init(this, applicationContext)
//        scheduleSyncJob(applicationContext)
        PlayerManager.init(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        // Stop the player to prepare it with the new MediaSource
        PlayerManager.player.stop()
        PlayerManager.player.clearMediaItems()
        PlayerManager.isLoading = true
        setContent {
            MaterialTheme {
                PlayerView()
            }
        }

        Thread {
            var card: CardItem
            val data = intent.data
            data?.let {
                val serializedCard = URLDecoder.decode(data.toString(), StandardCharsets.UTF_8.toString()).replace("exttv_player://app?","")
                println(serializedCard)
                card = Json.decodeFromString(CardItem.serializer(), serializedCard)
                println(card)

                if(card.isFolder){
                    PlayerManager.cardList = PythonManager.getSection(card.uri)
                    if(PlayerManager.cardList.isNotEmpty() && !PlayerManager.cardList[0].isFolder){
                        card = PlayerManager.cardList[0]
                        PlayerManager.currentCard = card
                        StatusManager.lastSelectedCard = card
                    } else {
                        throw Exception("Folder is empty")
                    }
                } else {
                    PlayerManager.cardList = PythonManager.getSection(card.uriParent)
                        .filter { !it.isFolder } // Filters out cards with isFolder == true
                        .toMutableList()

                    PlayerManager.currentCard = card
                    PlayerManager.isProgressBarVisible = true
                }

                if (card.mediaSource.isEmpty()) { // if mediaSource hasn't been filled, ask Python to fill it
                    StatusManager.lastSelectedCard = card
                    PythonManager.runPluginUri(card.uri)
                    PlayerManager.isLoading = false
                } else {
                    val mediaSource = Json.decodeFromString<ExtTvMediaSource>(
                        URLDecoder.decode(
                            card.mediaSource,
                            StandardCharsets.UTF_8.toString()
                        )
                    )
                    Handler(Looper.getMainLooper()).post {
                        PlayerManager.setMediaSource(
                            MediaSourceManager.preparePlayer(
                                mediaSource
                            )
                        )
                        PlayerManager.isLoading = false
                    }
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        PlayerManager.isLoading = true
        PlayerManager.isProgressBarVisible = false
        PlayerManager.isVisibleCardList = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent for the activity
    }

    override fun onStop() {
        super.onStop()
    }

}