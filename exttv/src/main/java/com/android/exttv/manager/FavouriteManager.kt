package com.android.exttv.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.android.exttv.manager.SectionManager.CardItem

object FavouriteManager {
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    var selectedIndex by mutableIntStateOf(-1)

    // Initialize with SharedPreferences
    fun init(context: Context) {
        prefs = context.getSharedPreferences("favourite_prefs", Context.MODE_PRIVATE)
        val allLists: Map<String, List<CardItem>> = getAllFavourites()
        for ((listName, cardList) in allLists) {
            Log.d("Favourites", "List Name: $listName")
            cardList.forEach { card ->
                Log.d("Favourites", "Card: $card")
            }
        }
    }

    // Create a new list given the card/cards
    fun createList(listName: String, cards: List<CardItem>) {
        saveCardList(listName, cards)
        addListName(listName)
    }

    // Add a new card to a specific list or create a new list if it doesn't exist
    fun addCardToListOrCreate(listName: String, card: CardItem) {
        val cardList = getFavourite(listName).toMutableList()
        cardList.add(card)
        saveCardList(listName, cardList)
        addListName(listName) // Ensure the list name is added
    }

    // Remove a card from a specific list
    fun removeCardFromList(listName: String, card: CardItem) {
        val cardList = getFavourite(listName).toMutableList()
        cardList.removeAll { it.id == card.id }
        saveCardList(listName, cardList)
    }

    // Move a card to a new position in the list
    fun moveCardInList(listName: String, cardId: String, newPosition: Int) {
        val cardList = getFavourite(listName).toMutableList()
        val card = cardList.find { it.id == cardId } ?: return

        // Remove the card from its current position
        cardList.remove(card)

        // Insert the card at the new position
        if (newPosition < 0) {
            cardList.add(0, card) // Move to the beginning if position is negative
        } else if (newPosition >= cardList.size) {
            cardList.add(card) // Move to the end if position is beyond the list size
        } else {
            cardList.add(newPosition, card) // Move to the specific position
        }

        saveCardList(listName, cardList)
    }

    // Retrieve a list of CardItem
    fun getFavourite(listName: String): List<CardItem> {
        val jsonString = prefs.getString(listName, null) ?: return emptyList()
        val type = object : TypeToken<List<CardItem>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    // Save a list of CardItem
    private fun saveCardList(listName: String, cardList: List<CardItem>) {
        val jsonString = gson.toJson(cardList)
        prefs.edit().putString(listName, jsonString).apply()
    }

    // Delete a list entirely
    fun deleteFavourite(listName: String) {
        prefs.edit().remove(listName).apply()
        removeFavourite(listName)
    }

    // Clear all stored lists
    fun clearAllLists() {
        val listNames = getAllFavouritesNames()
        listNames.forEach { listName ->
            prefs.edit().remove(listName).apply()
        }
        clearAllListNames()
    }

    // Add a list name to the set of all list names
    private fun addListName(listName: String) {
        val listNames = getAllFavouritesNames().toMutableSet()
        listNames.add(listName)
        prefs.edit().putStringSet("list_names", listNames).apply()
        StatusManager.focusedContextIndex = -1
    }

    // Remove a list name from the set of all list names
    private fun removeFavourite(listName: String) {
        val listNames = getAllFavouritesNames().toMutableSet()
        listNames.remove(listName)
        prefs.edit().putStringSet("list_names", listNames).apply()
//        StatusManager.selectedIndex = -1
        StatusManager.focusedContextIndex = -1
    }

    // Retrieve all list names
    fun getAllFavouritesNames(): Set<String> {
        return prefs.getStringSet("list_names", emptySet()) ?: emptySet()
    }

    // Clear all list names
    private fun clearAllListNames() {
        prefs.edit().remove("list_names").apply()
    }

    // Get all lists with their names and contents
    fun getAllFavourites(): Map<String, List<CardItem>> {
        val listNames = getAllFavouritesNames()
        val allLists = mutableMapOf<String, List<CardItem>>()
        listNames.forEach { listName ->
            allLists[listName] = getFavourite(listName)
        }
        return allLists
    }

    fun size(): Int {
        return getAllFavouritesNames().size
    }
}