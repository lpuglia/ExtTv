package com.android.exttv

import CatalogBrowser
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.MaterialTheme

class MainActivity : ComponentActivity() {

    companion object {
        private var instance: MainActivity? = null

        @JvmStatic // Optional annotation for Java interop
        fun getInstance(): MainActivity? {
            return instance
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
            runOnUiThread {
                Toast.makeText(this, "No application found to open the magnet.\nSupported applications: Amnis, Splayer, Stremio, ...", Toast.LENGTH_LONG).show()
            }
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

        setContent {
            MaterialTheme {
                CatalogBrowser(this)
            }
        }
    }

    override fun onResume() { //only called at standby thanks to onUserLeaveHint
        super.onResume()
    }

}