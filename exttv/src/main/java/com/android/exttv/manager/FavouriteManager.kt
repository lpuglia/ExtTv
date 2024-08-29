package com.android.exttv.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.android.exttv.manager.SectionManager.CardItem

typealias Favourite = List<CardItem>

object FavouriteManager {
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private val lock = Any() // Lock object for synchronization

    // Initialize with SharedPreferences
    fun init(context: Context) {
        synchronized(lock) {
            prefs = context.getSharedPreferences("favourite_prefs", Context.MODE_PRIVATE)
            val allLists: Map<String, Favourite> = getAllFavourites()
            for ((listName, cardList) in allLists) {
                Log.d("Favourites", "List Name: $listName")
                cardList.forEach { card ->
                    Log.d("Favourites", "Card: $card")
                }
            }
        }
    }

    // Get all lists with their names and contents
    private fun getAllFavourites(): Map<String, Favourite> {
        synchronized(lock) {
            val jsonString = prefs.getString("all_favourites", null) ?: return emptyMap()
            val type = object : TypeToken<Map<String, Favourite>>() {}.type
            return gson.fromJson(jsonString, type)
        }
    }

    // Save all lists and their names in a single entry
    private fun saveAllData(allLists: Map<String, Favourite>) {
        synchronized(lock) {
            val jsonString = gson.toJson(allLists)
            prefs.edit().putString("all_favourites", jsonString).apply()
            StatusManager.update()
        }
    }

    // Create a new list given the card/cards
    fun createFavourite(listName: String, cards: Favourite) {
        synchronized(lock) {
            val allLists = getAllFavourites().toMutableMap()
            allLists[listName] = cards
            saveAllData(allLists)
        }
    }

    // Add a new card to a specific list or create a new list if it doesn't exist
    fun addCardOrCreateFavourite(listName: String, card: CardItem) {
        synchronized(lock) {
            val allLists = getAllFavourites().toMutableMap()
            val cardList = allLists.getOrPut(listName) { mutableListOf() } as MutableList<CardItem>
            cardList.add(card)
            allLists[listName] = cardList
            saveAllData(allLists)
        }
    }

    // Remove a card from a specific list
    fun removeCardFromFavourite(listName: String, card: CardItem) {
        synchronized(lock) {
            val allLists = getAllFavourites().toMutableMap()
            val cardList = allLists[listName]?.toMutableList() ?: return
            cardList.removeAll { it.uri == card.uri }
            if (cardList.isEmpty()) {
                allLists.remove(listName)
            } else {
                allLists[listName] = cardList
            }
            saveAllData(allLists)
        }
    }

    // Move a card to a new position in the list
    fun moveCardInFavourite(listName: String, cardId: String, newPosition: Int) {
        synchronized(lock) {
            val allLists = getAllFavourites().toMutableMap()
            val cardList = allLists[listName]?.toMutableList() ?: return
            val card = cardList.find { it.uri == cardId } ?: return

            cardList.remove(card)
            when {
                newPosition < 0 -> cardList.add(0, card)
                newPosition >= cardList.size -> cardList.add(card)
                else -> cardList.add(newPosition, card)
            }

            allLists[listName] = cardList
            saveAllData(allLists)
        }
    }

    // Retrieve a list of CardItem
    fun getFavourite(listName: String): Favourite {
        synchronized(lock) {
            return getAllFavourites()[listName] ?: emptyList()
        }
    }

    // Delete a list entirely
    fun deleteFavourite(index: Int) {
        synchronized(lock) {
            val allLists = getAllFavourites().toMutableMap()
            allLists.remove(allLists.keys.elementAt(index))
            saveAllData(allLists)
        }
    }

    // Retrieve all list names
    fun getAllFavouriteNames(): Set<String> {
        synchronized(lock) {
            return getAllFavourites().keys
        }
    }

    operator fun get(index: Int): String {
        return getAllFavourites().keys.elementAt(index)
    }

    fun indexOf(favouriteName: String): Int = getAllFavourites().keys.indexOf(favouriteName)

    val size: Int
        get() =  getAllFavourites().size
}
