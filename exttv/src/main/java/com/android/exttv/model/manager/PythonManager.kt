package com.android.exttv.model.manager

import android.content.Context
import com.android.exttv.model.data.CardItem
import com.android.exttv.model.manager.SectionManager.focusCard
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.android.exttv.model.manager.AddonManager as Addons
import com.android.exttv.model.manager.FavouriteManager as Favourites
import com.android.exttv.model.manager.SectionManager as Sections
import com.android.exttv.model.manager.StatusManager as Status

object PythonManager {
    private lateinit var exttv: PyObject

    fun init(context: Context) {
        if (!::exttv.isInitialized) {
            if (!Python.isStarted()) Python.start(AndroidPlatform(context))
            val python = Python.getInstance()
            // Fix for: KeyError: '_strptime'
            python.getModule("datetime").get("datetime")?.callAttr("strptime", "00:00", "%H:%M")
            exttv = python.getModule("exttv") // this initialize the workspace
        }
    }

    fun selectAddon(pluginName: String) {
        Status.selectedAddonIndex = Addons.getAllAddonNames().indexOf(pluginName)
        selectSection(CardItem("plugin://${Addons.getIdByName(pluginName)}/", "Menu", isFolder = true))
    }

    fun selectFavourite(favouriteName: String) {
        Status.selectedAddonIndex = Addons.size + Favourites.indexOf(favouriteName)
        selectSection(CardItem("favourite://${favouriteName}", favouriteName, isFolder = true))
    }

    fun getSection(uri: String): List<CardItem> {
        return when {
            uri.startsWith("plugin://") -> {
                runPluginUri(uri)
            }
            uri.startsWith("favourite://") -> {
                Favourites.getFavourite(uri.replace("favourite://", ""))
            }
            else -> emptyList()
        }
    }

    // add Mutex to prevent multiple threads from accessing Python engine at the same time
    private val lock = Any()
    fun runPluginUri(uri: String): List<CardItem> {
        return synchronized(lock) {
            exttv?.callAttr("run", uri)?.toJava(List::class.java) as List<CardItem>
        }
    }

    fun selectSection(card: CardItem, sectionIndex: Int = -1, cardIndex: Int = 0) {
        Status.loadingState = LoadingStatus.SELECTING_SECTION
        Status.lastSelectedCard = card
        Thread {
            if(card.isFolder){
                Sections.removeAndAdd(sectionIndex+1, card, getSection(card.uri))
            }else{
                runPluginUri(card.uri)
            }
            focusCard(sectionIndex+1, 0)
            Status.loadingState = LoadingStatus.DONE;
            PlayerManager.isLoading = false
        }.start()
    }

    fun unfoldCard(card: CardItem, visitedUris: MutableSet<String> = mutableSetOf()): List<CardItem> {
        if (card.uri in visitedUris) return emptyList()
        visitedUris.add(card.uri)

        val childCards = exttv?.callAttr("run", card.uri)?.toJava(List::class.java) as List<CardItem>
        val allCards = mutableListOf<CardItem>()

        for (child in childCards) {
            if (child.isFolder) allCards.addAll(unfoldCard(child, visitedUris))
            else allCards.add(child)
        }
        return allCards
    }
}
