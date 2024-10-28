package com.android.exttv.model.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.exttv.model.data.CardItem
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
            if (Sections.isNotEmpty) return
            if (!Python.isStarted()) Python.start(AndroidPlatform(context))
            exttv = Python.getInstance().getModule("exttv") // this initialize the workspace
        }
    }

    fun selectAddon(pluginName: String) {
        Status.selectedIndex = Addons.getAllAddonNames().indexOf(pluginName)
        Sections.focusedIndex = -1
        Sections.focusedCardIndex = -1
        selectSection(CardItem("plugin://${Addons.getIdByName(pluginName)}/", "Menu"))
    }

    fun selectFavourite(favouriteName: String) {
        Status.selectedIndex = Addons.size + Favourites.indexOf(favouriteName)
        Sections.focusedIndex = -1
        Sections.focusedCardIndex = -1
        selectSection(CardItem("favourite://${favouriteName}", favouriteName))
    }

    fun getSection(uri: String): List<CardItem> {
        if (uri.startsWith("plugin://")) {
            return exttv?.callAttr("run", uri)?.toJava(List::class.java) as List<CardItem>
        }else if(uri.startsWith("favourite://")){
            return Favourites.getFavourite(uri.replace("favourite://", ""))
        }else{
            return emptyList()
        }
    }

    fun selectSection(card: CardItem, sectionIndex: Int = -1, cardIndex: Int = 0) {
        Status.loadingState = LoadingStatus.SELECTING_SECTION
        val runnable = Runnable {
            val newSection = Sections.Section(card.label, getSection(card.uri))

            if(card.isFolder){
                // if newSection is empty, this will be taken care of in removeAndAdd
                Sections.removeAndAdd(sectionIndex+1, card.uri, newSection)
                Sections.updateSelectedSection(sectionIndex, cardIndex)
            }else{
//                Sections.updateSelectedSection(sectionIndex, cardIndex)
            }

            try {
                Handler(Looper.getMainLooper()).post {
                    if(sectionIndex==-1 && newSection.cardList.isEmpty()){
                        Sections.clearSections()
                    }else if(newSection.cardList.isNotEmpty()){
                        // if selected card is adding a new section, then focus on the new section
                        Sections.focusedIndex += 1
                        Sections.focusedCardIndex = 0
                    }
                    // this must be outside to guarantee that spinner is hidden
                    Status.loadingState = LoadingStatus.DONE;
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
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
