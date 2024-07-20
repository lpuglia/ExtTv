package com.android.exttv

import CatalogBrowser
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.MaterialTheme
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {

    companion object {
        private var instance: MainActivity? = null

        @JvmStatic // Optional annotation for Java interop
        fun getInstance(): MainActivity? {
            return instance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
//        if (!Python.isStarted()) {
//            Python.start(AndroidPlatform(this))
//        }
//        lateinit var kodi: PyObject
//        val runnable = Runnable {
//            val py = Python.getInstance()
//            py.getModule("xbmcgui")
//            kodi = py.getModule("kodi")
//        }
//        val thread = Thread(runnable)
//        thread.start()
//        thread.join()
//
//        val runnable2 = Runnable {
//            kodi.callAttr("test_dialog").toJava(List::class.java)
//        }
//        val thread2 = Thread(runnable2)
//        thread2.start()
//        thread2.join()

        setContent { // In here, we can call composables!
            MaterialTheme {
                CatalogBrowser(this)
            }
        }
    }

}
