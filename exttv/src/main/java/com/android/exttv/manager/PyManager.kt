package com.android.exttv.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.exttv.model.CardView
import com.android.exttv.model.Section
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.util.SortedSet
import java.util.TreeSet

object PyManager {
    lateinit var installedAddons: SortedSet<String>
    private var exttv: PyObject? = null
    private val titleMap: MutableMap<String, String> = mutableMapOf()
    var sectionList by mutableStateOf(listOf<Section>())
    var isLoadingAddon by mutableStateOf(false)
    var isLoadingSection by mutableStateOf(false)
    var manager : SectionManager = SectionManager()

    private fun getInstalledAddons(context: Context): SortedSet<String> {
        val addonsPath = File(context.filesDir, "exttv_home/addons")
        return if (addonsPath.exists() && addonsPath.isDirectory) {
            TreeSet(addonsPath.list()?.toList() ?: emptyList())
        } else {
            TreeSet()
        }
    }

    fun Init(context: Activity) {
        this.installedAddons = getInstalledAddons(context)

        if (exttv != null && sectionList.isNotEmpty()) {
            return
        }

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val runnable = Runnable {
            val py = Python.getInstance()
            exttv = py.getModule("exttv") // this initialize the workspace
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()
    }

    fun addPlugin(){//owner: String, repo: String, branch: String) {
        isLoadingAddon = true
        val owner = "kodiondemand"
        val repo = "addon"
        val branch = "master"
//        val owner = "luivit"
//        val repo = "plugin.video.rivedila7"
//        val branch = "master"

        manager = SectionManager()
        titleMap.clear()

        var pluginName = ""
        val runnable = Runnable {
            val py = Python.getInstance()
            val utils = py.getModule("utils")
            pluginName = utils.callAttr("download_and_extract_plugin", owner, repo, branch, true).toString()
            installedAddons.add(pluginName)
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()
        Log.d("Python", pluginName)

        titleMap["plugin://$pluginName/"] = "Menu"
        SetSection("plugin://$pluginName/")
    }

    fun selectPlugin(pluginName: String) {
        isLoadingAddon = true
        manager = SectionManager()
        titleMap.clear()
        Log.d("Python", pluginName)
        titleMap["plugin://$pluginName/"] = "Menu"

        val runnable = Runnable {
            val py = Python.getInstance()
            val utils = py.getModule("utils")
            utils.callAttr("set_plugin_name", pluginName)
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()

        titleMap["plugin://$pluginName/"] = "Menu"
        SetSection("plugin://$pluginName/")
    }

    fun SetSection(argv2: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        isLoadingSection = true
        val runnable = Runnable {
            val title : String = titleMap.getOrDefault(argv2, "")
            val newSection = Section(title, exttv?.callAttr("run", argv2)?.toJava(List::class.java) as List<CardView>)
            if(newSection.movieList.isEmpty()){
                isLoadingSection = false
                return@Runnable
            }
            titleMap.putAll(newSection.movieList.associate { it.id to it.label })

            val lastKey = manager.getLastSectionKey()
            if(manager.removeAndAdd(sectionIndex+1, argv2, newSection)) {
                manager.updateSelectedIndex(sectionIndex, cardIndex)
            }
            try {
                Handler(Looper.getMainLooper()).post {
                    Log.d("Python", newSection.toString())
                    sectionList = manager.getSectionsInOrder()
                    isLoadingSection = false // Stop loading indicator
                    isLoadingAddon = false
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
    }
}
