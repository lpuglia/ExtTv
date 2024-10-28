package com.android.exttv.view

import CatalogBrowser
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.MaterialTheme
import com.android.exttv.model.manager.AddonManager
import com.android.exttv.model.manager.FavouriteManager
import com.android.exttv.model.manager.PythonManager
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.model.data.CardItem
import com.android.exttv.service.scheduleSyncJob
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    companion object {
        private lateinit var instance: MainActivity
        @JvmStatic // Optional annotation for Java interop
        fun getInstance(): MainActivity {
            return instance
        }
    }

    private var doubleBackToExitPressedOnce = false
    private val backPressInterval: Long = 2000 // 2 seconds

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (doubleBackToExitPressedOnce) {
                finishAffinity() // This will close the app and go back to the home screen
                return true
            }

            // First back press
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            // Reset the flag after the specified interval
            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, backPressInterval)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        AddonManager.init(applicationContext)
        PythonManager.init(applicationContext)
        FavouriteManager.init(applicationContext)
        StatusManager.init(applicationContext)

        val data = intent.data
        if(data != null) {
            intent.data?.let {
                val uriString = data.toString()
                if (uriString.startsWith("exttv://")) {
                    val intentCardItem = Json.decodeFromString(CardItem.serializer(), uriString.replace("exttv://",""))
                    PythonManager.selectSection(intentCardItem)
                }
            }
        }else {
            scheduleSyncJob(applicationContext)
            setContent {
                MaterialTheme {
                    CatalogBrowser()
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        println("onRestart")
        setContent {
            MaterialTheme {
                CatalogBrowser()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DEL) {
            dispatchKeyEvent(KeyEvent(event.action, KeyEvent.KEYCODE_BACK))
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}