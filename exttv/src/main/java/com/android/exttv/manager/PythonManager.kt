package com.android.exttv.manager

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.exttv.manager.SectionManager
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.SectionManager as Sections
import com.android.exttv.manager.StatusManager as Status

object PythonManager {
    private var exttv: PyObject? = null

    fun init(context: Activity) {
        if (exttv != null && Status.sectionList.isNotEmpty()) return
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        Thread {
            exttv = Python.getInstance().getModule("exttv") // this initialize the workspace
        }.apply { start(); join() }
    }

    fun addAddon(url: String, isOfficial : Boolean = true) {
        Status.loadingState = LoadingStatus.INSTALLING_ADDON
        var pluginName = ""
        Thread {
            val utils = Python.getInstance().getModule("utils")
            pluginName = utils.callAttr(if(isOfficial)"get_from_repository" else "get_from_git", url, true).toString()
            Addons.add(pluginName)
        }.apply { start(); join() }
        selectAddon(pluginName)
    }

    fun selectAddon(pluginName: String) {
        Status.loadingState = LoadingStatus.SELECTING_ADDON
        Sections.clearSections()
        Status.titleMap.clear()
        Thread {
            Python.getInstance().getModule("utils").callAttr("set_plugin_name", pluginName)
        }.apply { start(); join() }
        Addons.selectAddon(pluginName)
        Status.titleMap["plugin://$pluginName/"] = "Menu"
        selectSection("plugin://$pluginName/")
    }

    fun selectSection(argv2: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        Status.loadingState = LoadingStatus.SELECTING_SECTION
        val runnable = Runnable {
            val title : String = Status.titleMap.getOrDefault(argv2, "")
            // returned value from exttv.py
            val newSection = Section(title, exttv?.callAttr("run", argv2)?.toJava(List::class.java) as List<SectionManager.CardView>)
            Status.titleMap.putAll(newSection.movieList.associate { it.id to it.label })

            if(newSection.movieList.isNotEmpty() && Sections.removeAndAdd(sectionIndex+1, argv2, newSection)) {
                Sections.updateSelectedSection(sectionIndex, cardIndex)
            }

            try {
                Handler(Looper.getMainLooper()).post {
                    if(sectionIndex==-1 && newSection.movieList.isEmpty()){
                        Sections.clearSections()
                    }else{
                        Status.loadingState = LoadingStatus.SECTION_LOADED
                        Status.sectionList = Sections.getSectionsInOrder()
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
    }
}
