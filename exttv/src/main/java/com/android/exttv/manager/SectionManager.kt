package com.android.exttv.manager

import com.android.exttv.model.Section

class SectionManager() {
    private val sections = LinkedHashMap<String, Section>()
    private val selectedIndices: MutableList<Int?> = mutableListOf()

    fun removeAndAdd(index: Int, key: String, newSection: Section): Boolean {
        // Convert the map keys to a list to easily access by index
        val keys = sections.keys.toList()

        // Compare newSection with the section at index-1 if index is greater than 0
        if (index > 0 && sections[keys[index - 1]]?.movieList == newSection.movieList) {
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

    fun updateSelectedIndex(sectionIndex: Int, selectedIndex: Int?) {
        if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex] = selectedIndex
        }
    }

    fun getSelectedIndexForSection(sectionIndex: Int): Int? {
        return if (sectionIndex in selectedIndices.indices) {
            selectedIndices[sectionIndex]
        } else {
            null
        }
    }
}