package com.android.exttv.view

import CatalogBrowser
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.MaterialTheme
import com.android.exttv.model.AddonManager
import com.android.exttv.model.FavouriteManager
import com.android.exttv.model.PythonManager
import com.android.exttv.model.StatusManager

class MainActivity : ComponentActivity() {

    companion object {
        private var instance: MainActivity? = null
        private var doubleBackToExitPressedOnce = false
        private val backPressInterval: Long = 2000 // 2 seconds

        @JvmStatic // Optional annotation for Java interop
        fun getInstance(): MainActivity? {
            return instance
        }
    }


    // Method to show a Toast, callable from Python
    fun showToast(message: String?, duration: Int) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, duration).show()
        }
    }

    fun fireMagnetIntent(magnetUri: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(magnetUri)
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        try {
            this.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast("No application found to open the magnet.\nSupported applications: Amnis, Splayer, Stremio, ...", Toast.LENGTH_LONG)
        }
    }

    fun executeStartActivity(command: String) {
        Log.d("python", "Executing command: $command")
        val parts = command.removeSurrounding("StartAndroidActivity(", ")")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }

        // Check that we have exactly 4 parameters
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid command format")
        }

        var (_, action, type, url) = parts
        when(action) {
            "android.intent.action.VIEW" -> {
                action = Intent.ACTION_VIEW
            }
            else -> {
                throw IllegalArgumentException("Invalid action")
            }
        }

        val intent = Intent(action).apply {
            data = Uri.parse(url)
        }

        this.startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (doubleBackToExitPressedOnce) {
                // Navigate to home screen
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
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        AddonManager.init(this)
        PythonManager.init(this)
        FavouriteManager.init(this)
        StatusManager.init(this)

        val data = intent.data
        if(data != null) {
            intent.data?.let {
                val uriString = data.toString()
                if (uriString.startsWith("exttv://")) {
                    PythonManager.selectSection(uriString.replace("exttv://",""), "prova", -1, 0)
                }
            }
        }else {
            setContent {
                MaterialTheme {
                    CatalogBrowser()
                }
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