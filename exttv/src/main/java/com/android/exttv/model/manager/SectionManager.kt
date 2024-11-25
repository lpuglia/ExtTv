package com.android.exttv.model.manager

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.exttv.model.data.CardItem

@SuppressLint("MutableCollectionMutableState")
object SectionManager {
    data class Section(
        val title: String,
        val cardList: List<CardItem>,
    )

    var focusedIndex by mutableIntStateOf(-1)
    var focusedCardIndex by mutableIntStateOf(-1)
    var refocus by mutableStateOf(false)
    private var sections by mutableStateOf(LinkedHashMap<String, Section>())
    var focusedCardPlayerIndex by mutableIntStateOf(0)

    private val selectedIndices: MutableList<Int?> = mutableListOf()

    fun focusCard(sectionIndex: Int, cardIndex: Int) {
        println("FocusCard: $sectionIndex, $cardIndex")
        focusedIndex = sectionIndex
        focusedCardIndex = cardIndex
        refocus = true
    }

    fun refocusCard() {
        refocus = true
    }

    fun removeAndAdd(sectionIndex: Int, card: CardItem, cardList: List<CardItem>) {
        val newSection = Section(card.label, cardList)
        // Convert the map keys to a list to easily access by index
        val keys = sections.keys.toList()

        // Compare newSection with the section at index-1 if index is greater than 0
        // if (index > 0 && sections[keys[index - 1]]?.cardList == newSection.cardList) {
        //        return false// Ignore the addition if the newSection is equal to the last added section
        // }

        // Check if the newSection is empty
        if (cardList.isEmpty()) {
            // If empty, remove all entries after the given index and return
            for (i in keys.size - 1 downTo sectionIndex) {
                remove(keys[i])
                selectedIndices.removeAt(i)
            }
            return
        }

        // Ensure the index is within the bounds
        if (sectionIndex in keys.indices) {

            // Remove all entries after the given index
            for (i in keys.size - 1 downTo sectionIndex + 1) {
                remove(keys[i])
                selectedIndices.removeAt(i)
            }

            // Replace the entry at the given index or add new if index is out of bounds
            if (sectionIndex < keys.size) {
                val keyAtIndex = keys[sectionIndex]
                this[keyAtIndex] = newSection
                if (sectionIndex > 0) {
                    selectedIndices[sectionIndex - 1] = sections.values.toList()[sectionIndex - 1].cardList.indexOf(card)
                }
                selectedIndices[sectionIndex] = null // Reset selected index for the new section
            }
        }
        // Add new section if index is out of bounds or size constraints apply
        if (sectionIndex >= keys.size) {
            this[card.uri] = newSection
            if (sectionIndex > 0) {
                selectedIndices[sectionIndex - 1] = sections.values.toList()[sectionIndex - 1].cardList.indexOf(card)
            }
            selectedIndices.add(null)
        }
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