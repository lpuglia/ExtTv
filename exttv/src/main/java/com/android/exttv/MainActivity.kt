package com.android.exttv

import CatalogBrowser
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
//            this.type = type
        }

        this.startActivity(intent)
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

        setContent {
            MaterialTheme {
                CatalogBrowser()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Remap back-space to the back key
        if (event.keyCode == KeyEvent.KEYCODE_DEL) {
            dispatchKeyEvent(KeyEvent(event.action, KeyEvent.KEYCODE_BACK))
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}