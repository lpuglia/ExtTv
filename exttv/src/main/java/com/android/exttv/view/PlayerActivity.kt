package com.android.exttv.view

import PlayerView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.data.ExtTvMediaSource
import kotlinx.serialization.json.Json
import org.conscrypt.Conscrypt
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.Security

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

        Security.insertProviderAt(Conscrypt.newProvider(), 1) //without this I get handshake error

        // disable strict mode because ScraperManager.postfinished may need to scrape a proxy when onDemand is called
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val data = intent.data
        data?.let {
            val serializedCard = URLDecoder.decode(data.toString(), StandardCharsets.UTF_8.toString()).replace("exttv_player://app?","")
            card = Json.decodeFromString(CardItem.serializer(), serializedCard)
            val mediaSource = Json.decodeFromString<ExtTvMediaSource>(URLDecoder.decode(card.mediaSource, StandardCharsets.UTF_8.toString()))
            setContent {
                PlayerView(mediaSource)
            }
        }
    }
}