package com.android.exttv.model

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.android.exttv.model.SectionManager
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.android.exttv.model.AddonManager as Addons
import com.android.exttv.model.FavouriteManager as Favourites
import com.android.exttv.model.SectionManager as Sections
import com.android.exttv.model.StatusManager as Status

object PythonManager {
    private var exttv: PyObject? = null

    fun init(context: Activity) {
        if (exttv != null && Sections.isNotEmpty) return
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        Thread {
            exttv = Python.getInstance().getModule("exttv") // this initialize the workspace
        }.apply { start(); join() }
    }

    fun selectAddon(pluginName: String) {
        Status.selectedIndex = Addons.getAllAddonNames().indexOf(pluginName)
        Status.loadingState = LoadingStatus.SELECTING_ADDON
        selectSection("plugin://${Addons.getIdByName(pluginName)}/", "Menu")
    }

    fun selectFavourite(favouriteName: String) {
        Status.selectedIndex = Addons.size + Favourites.indexOf(favouriteName)
        Status.loadingState = LoadingStatus.SELECTING_SECTION
        Sections.removeAndAdd(0, "", Sections.Section(favouriteName, Favourites.getFavourite(favouriteName)))
        Status.loadingState = LoadingStatus.SECTION_LOADED
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

    fun unfoldCard(card: Sections.CardItem, visitedUris: MutableSet<String> = mutableSetOf()): List<Sections.CardItem> {
        if (card.uri in visitedUris) return emptyList()
        visitedUris.add(card.uri)

        val childCards = exttv?.callAttr("run", card.uri)?.toJava(List::class.java) as List<Sections.CardItem>
        val allCards = mutableListOf<Sections.CardItem>()

        for (child in childCards) {
            if (child.isFolder) allCards.addAll(unfoldCard(child, visitedUris))
            else allCards.add(child)
        }
        return allCards
    }
}
