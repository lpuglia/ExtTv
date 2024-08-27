package com.android.exttv.manager

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.android.exttv.manager.SectionManager
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.FavouriteManager as Favourites
import com.android.exttv.manager.SectionManager as Sections
import com.android.exttv.manager.StatusManager as Status

object PythonManager {
    private var exttv: PyObject? = null

    fun init(context: Activity) {
        if (exttv != null && Sections.isNotEmpty) return
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        Thread {
            exttv = Python.getInstance().getModule("exttv") // this initialize the workspace
        }.apply { start(); join() }
    }

    fun setPluginName(pluginName: String) {
        Thread {
            Python.getInstance().getModule("utils").callAttr("set_plugin_name", pluginName)
        }.apply { start(); join() }
    }

    fun selectFavourite(favouriteName: String) {
        Favourites.getFavourite(favouriteName).let {
            Status.loadingState = LoadingStatus.SELECTING_ADDON
            Sections.clearSections()
            Favourites.selectFavourite(favouriteName)
            Status.loadingState = LoadingStatus.SECTION_LOADED
        }
    }

    fun selectAddon(pluginName: String) {
        Status.loadingState = LoadingStatus.SELECTING_ADDON
        Sections.clearSections()
        setPluginName(pluginName)
        Addons.selectAddon(pluginName)
        selectSection("plugin://$pluginName/", "Menu")
    }

    fun selectSection(uri: String, title: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        Status.loadingState = LoadingStatus.SELECTING_SECTION
        val runnable = Runnable {
            // returned value from exttv.py
            val newSection = Sections.Section(title, exttv?.callAttr("run", uri)?.toJava(List::class.java) as List<SectionManager.CardItem>)

            if(newSection.cardList.isNotEmpty() && Sections.removeAndAdd(sectionIndex+1, uri, newSection)) {
                Sections.updateSelectedSection(sectionIndex, cardIndex)
            }

            try {
                Handler(Looper.getMainLooper()).post {
                    if(sectionIndex==-1 && newSection.cardList.isEmpty()){
                        Sections.clearSections()
                    }else{
                        Status.loadingState = LoadingStatus.SECTION_LOADED
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
    }
}
