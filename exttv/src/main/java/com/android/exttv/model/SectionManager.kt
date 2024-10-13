package com.android.exttv.model

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@SuppressLint("MutableCollectionMutableState")
object SectionManager {
    data class Section(
        val title: String,
        val cardList: List<CardItem>,
    )
    data class CardItem(
        val uri: String,
        val label : String,
        val label2 : String,
        val plot : String,
        val thumbnailUrl: String,
        val posterUrl: String,
        val fanartUrl: String,
        val isFolder: Boolean,
    ) {
        val pluginName: String
            get() = uri.split("://")[1].split("/")[0]
    }

    var focusedIndex by mutableIntStateOf(-1)
    var focusedCardIndex by mutableIntStateOf(-1)
    private var sections by mutableStateOf(LinkedHashMap<String, Section>())

    private val selectedIndices: MutableList<Int?> = mutableListOf()

    fun removeAndAdd(index: Int, key: String, newSection: Section): Boolean {
        // Convert the map keys to a list to easily access by index
        val keys = sections.keys.toList()

        // Compare newSection with the section at index-1 if index is greater than 0
        if (index > 0 && sections[keys[index - 1]]?.cardList == newSection.cardList) {
            return false// Ignore the addition if the newSection is equal to the last added section
        }

        // Ensure the index is within the bounds
        if (index in keys.indices) {

            // Remove all entries after the given index
            for (i in keys.size - 1 downTo index + 1) {
                this.remove(keys[i])
                selectedIndices.removeAt(i)
            }

            // Replace the entry at the given index or add new if index is out of current bounds
            if (index < keys.size) {
                val keyAtIndex = keys[index]
                this[keyAtIndex] = newSection
                selectedIndices[index] = null // Reset selected index for the new section
            } else {
                // If the index is out of bounds (greater than current size), add the new section
                this[key] = newSection
                selectedIndices.add(null)
            }
        } else {
            // If index is out of bounds, just add the new section
            this[key] = newSection
            selectedIndices.add(null)
        }
        return true
    }

    fun replaceCard(sectionIndex: Int, cardToReplace: CardItem, newSection: Section): Boolean {
        // Ensure the sectionIndex is within bounds
        val sectionKeys = sections.keys.toList()
        if (sectionIndex !in sectionKeys.indices) return false

        // Get the section by index
        val sectionKey = sectionKeys[sectionIndex]
        val section = this[sectionKey] ?: return false

        // Check if the card to replace exists in the section
        val cardIndex = section.cardList.indexOfFirst { it.uri == cardToReplace.uri }
        if (cardIndex == -1) return false // Card not found

        // Replace the card list with the newSection's card list
        val updatedCardList = section.cardList.toMutableList().apply {
            removeAt(cardIndex)
            addAll(cardIndex, newSection.cardList) // Insert new cards at the same index
        }

        // Update the section with the modified card list
        val updatedSection = section.copy(cardList = updatedCardList)
        this[sectionKey] = updatedSection
        return true
    }

    fun getSectionsInOrder(): List<Section> {
        return sections.values.toList()
    }

    fun getFocusedCard(): CardItem {
        return sections.entries.toList()[focusedIndex].value.cardList[focusedCardIndex]
    }

    fun updateSelectedSection(sectionIndex: Int, selectedIndex: Int?) {
        if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex] = selectedIndex
        }
    }

    fun getSelectedSection(sectionIndex: Int): Int? {
        return if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex]
        } else {
            null
        }
    }

    fun clearSections() {
        sections = LinkedHashMap()
        selectedIndices.clear()
        StatusManager.bgImage = ""
        StatusManager.loadingState = LoadingStatus.DONE
    }

    val isEmpty: Boolean get() = sections.isEmpty()
    val isNotEmpty: Boolean get() = sections.isNotEmpty()
    val size: Int get() = sections.size

    operator fun get(index: Int): Section {
        return sections.values.toList()[index]
    }

    operator fun get(key: String): Section? {
        return sections[key]
    }

    operator fun set(key: String, section: Section) {
        val newSections = LinkedHashMap(sections)
        newSections[key] = section
        sections = newSections
    }

    fun remove(key: String) {
        val newSections = LinkedHashMap(sections)
        newSections.remove(key)
        sections = newSections  // Assign new instance to trigger recomposition
    }

}