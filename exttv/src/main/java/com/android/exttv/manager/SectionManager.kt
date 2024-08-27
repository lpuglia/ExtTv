package com.android.exttv.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SectionManager {
    data class Section(
        val title: String,
        val cardList: List<CardItem>,
    )
    data class CardItem(
        val id: String,
        val label : String,
        val label2 : String,
        val plot : String,
        val thumbnailUrl: String,
        val posterUrl: String,
        val fanartUrl: String,
        val isFolder: Boolean,
    ) {
        val pluginName: String
            get() = id.split("://")[1].split("/")[0]
    }

    var focusedIndex by mutableIntStateOf(-1)
    var focusedCardIndex by mutableIntStateOf(-1)
    private val sections = LinkedHashMap<String, Section>()
    var sectionList by mutableStateOf(listOf<Section>())
    private val selectedIndices: MutableList<Int?> = mutableListOf()

    fun getFocusedCard(): CardItem {
        return sectionList[focusedIndex].cardList[focusedCardIndex]
    }

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
                sections.remove(keys[i])
                selectedIndices.removeAt(i)
            }

            // Replace the entry at the given index or add new if index is out of current bounds
            if (index < keys.size) {
                val keyAtIndex = keys[index]
                sections[keyAtIndex] = newSection
                selectedIndices[index] = null // Reset selected index for the new section
            } else {
                // If the index is out of bounds (greater than current size), add the new section
                sections[key] = newSection
                selectedIndices.add(null)
            }
        } else {
            // If index is out of bounds, just add the new section
            sections[key] = newSection
            selectedIndices.add(null)
        }
        return true
    }

    fun getSectionsInOrder(): List<Section> {
        return sections.values.toList()
    }

    fun getLastSectionKey(): String? {
        return sections.keys.lastOrNull()
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

    fun isEmpty(): Boolean {
        return sections.isEmpty()
    }

    fun clearSections() {
        sections.clear()
        selectedIndices.clear()
        StatusManager.bgImage = ""
        StatusManager.loadingState = LoadingStatus.DONE
        sectionList = getSectionsInOrder()
    }
}