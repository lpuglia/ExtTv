package com.android.exttv.model

import android.content.Context
import android.content.SharedPreferences
import com.android.exttv.model.SectionManager.CardItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.android.exttv.util.TvContract as tvContract

typealias Favourites = List<CardItem>

object FavouriteManager {
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // Initialize with SharedPreferences
    fun init(context: Context) {
        prefs = context.getSharedPreferences("favourite_prefs", Context.MODE_PRIVATE)
    }

    // Get all lists with their names and contents
    private fun getAllFavourites(): Map<String, Favourites> {
        val jsonString = prefs.getString("all_favourites", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Favourites>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    // Get all favourite cards from all favourites
    fun getAllFavouriteCards(): Map<String, List<CardItem>> {
        val toReturn = mutableMapOf<String, List<CardItem>>()
        for (listName in getAllFavouriteNames()) {
            getFavourite(listName).let { toReturn[listName] = it }
        }
        return toReturn
    }

    // Save all lists and their names in a single entry
    private fun saveAllData(allLists: Map<String, Favourites>) {
        val jsonString = gson.toJson(allLists)
        prefs.edit().putString("all_favourites", jsonString).apply()
        StatusManager.update()
        Thread {
            tvContract.createOrUpdateChannel(getAllFavouriteCards())
        }.start()
    }

    // Create a new list given the card/cards
    fun createFavourite(listName: String, favList: Favourites) {
        val allLists = getAllFavourites().toMutableMap()
        allLists[listName] = favList
        saveAllData(allLists)
    }

    // Add a new card to a specific list or create a new list if it doesn't exist
    fun addCardOrCreateFavourite(listName: String, card: CardItem) {
        val allLists = getAllFavourites().toMutableMap()
        val favList = allLists.getOrPut(listName) { mutableListOf() } as MutableList<CardItem>
        favList.add(card)
        allLists[listName] = favList
        saveAllData(allLists)
    }

    // Remove a card from a specific list
    fun removeCardFromFavourite(listName: String, card: CardItem) {
        val allLists = getAllFavourites().toMutableMap()
        val favList = allLists[listName]?.toMutableList() ?: return
        favList.removeAll { it.uri == card.uri }
        if (favList.isEmpty()) {
            allLists.remove(listName)
        } else {
            allLists[listName] = favList
        }
        saveAllData(allLists)
    }

    // Move a card to a new position in the list
    fun moveCardInFavourite(listName: String, favUri: String, newPosition: Int) {
        val allLists = getAllFavourites().toMutableMap()
        val favList = allLists[listName]?.toMutableList() ?: return
        val fav = favList.find { it.uri == favUri } ?: return

        favList.remove(fav)
        when {
            newPosition < 0 -> favList.add(0, fav)
            newPosition >= favList.size -> favList.add(fav)
            else -> favList.add(newPosition, fav)
        }

        allLists[listName] = favList
        saveAllData(allLists)
    }

    // Retrieve a list of favourites
    fun getFavourite(listName: String): List<CardItem> {
        val cache = mutableMapOf<String, List<CardItem>>()
        val toReturn = mutableListOf<CardItem>()
        val favourites = getAllFavourites()[listName]

        for (fav in favourites ?: emptyList()) {
            if(fav.uriParent !in cache) {
                cache[fav.uriParent] = PythonManager.getSection(fav.uriParent)
            }
            var favCard = cache[fav.uriParent]?.find { it.uri == fav.uri }
            // if uri was not found in the list, maybe the uri has been changed slightly
            // attempt a second match, this is not perfect and may not work
            // can't do much if addon developer doesn't provide a stable uri
            if (favCard == null) {
                favCard = cache[fav.uriParent]?.find { it.label == fav.label && it.isFolder == fav.isFolder }
            }
            if (favCard != null) {
                toReturn.add(favCard)
            }
        }
        return toReturn
    }

    // Delete a list entirely
    fun deleteFavourite(index: Int) {
        val allLists = getAllFavourites().toMutableMap()
        allLists.remove(allLists.keys.elementAt(index))
        saveAllData(allLists)
    }

    // Retrieve all list names
    fun getAllFavouriteNames(): Set<String> {
        return getAllFavourites().keys
    }

    operator fun get(index: Int): String {
        return getAllFavourites().keys.elementAt(index)
    }

    fun indexOf(favouriteName: String): Int = getAllFavourites().keys.indexOf(favouriteName)

    val size: Int
        get() =  getAllFavourites().size
}
