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
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.service.scheduleSyncJob

class MainActivity : ComponentActivity() {

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
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        StatusManager.init(this, applicationContext)
        scheduleSyncJob(applicationContext)

        setContent {
            MaterialTheme {
                CatalogBrowser()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        StatusManager.init(this, applicationContext)
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