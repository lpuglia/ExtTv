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
    private lateinit var card: CardItem

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
        PlayerManager.init(applicationContext)
        StatusManager.init(this, applicationContext)
    }

    override fun onRestart() {
        super.onRestart()
        PlayerManager.init(applicationContext)
        StatusManager.init(this, applicationContext)
    }

    override fun onResume() {
        PlayerManager.isLoading = true
        super.onResume()
        val data = intent.data
        data?.let {
            val serializedCard = URLDecoder.decode(data.toString(), StandardCharsets.UTF_8.toString()).replace("exttv_player://app?","")
            card = Json.decodeFromString(CardItem.serializer(), serializedCard)
            println(card)
            val mediaSource = Json.decodeFromString<ExtTvMediaSource>(URLDecoder.decode(card.mediaSource, StandardCharsets.UTF_8.toString()))
            setContent {
                MaterialTheme {
                    PlayerView()
                }
            }
            Handler(Looper.getMainLooper()).post {
                PlayerManager.setMediaSource(MediaSourceManager.preparePlayer(mediaSource))
                PlayerManager.currentCard = card
                PlayerManager.cardList = PythonManager.getSection(card.uriParent).toMutableList()
                PlayerManager.isLoading = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        PlayerManager.player.release()
        PlayerManager.isLoading = false
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