package com.android.exttv.model.data

data class Settings(
    val categories: List<Category>
)

data class Category(
    val label: String,
    val settings: List<Setting>
)

sealed class Setting {
    abstract val id: String
    abstract val label: String
    abstract val visible: String
    abstract val enable: Boolean

    data class VoidSetting(
        override val id: String,
        override val label: String,
        override val visible: String,
        override val enable: Boolean,
        val default: String,
    ) : Setting()

    // BoolSetting
    data class BoolSetting(
        override val id: String,
        override val label: String,
        val default: Boolean?,
        var currentValue: Boolean?, // Tracks the current value
        override val visible: String,
        override val enable: Boolean
    ) : Setting()

    // TextSetting
    data class TextSetting(
        override val id: String,
        override val label: String,
        val default: String,
        var currentValue: String, // Tracks the current value
        override val visible: String, // can be an expression
        override val enable: Boolean
    ) : Setting()

    // SliderSetting
    data class SliderSetting(
        override val id: String,
        override val label: String,
        val default: Int,
        var currentValue: Int, // Tracks the current value
        val range: String,
        override val visible: String,
        override val enable: Boolean,
        val option: String
    ) : Setting()

    // SelectSetting
    data class SelectSetting(
        override val id: String,
        override val label: String,
        val default: String,
        var currentValue: String, // Tracks the current value
        val values: List<Any?>,
        override val visible: String,
        override val enable: Boolean
    ) : Setting()

    // ActionSetting
    data class ActionSetting(
        override val id: String,
        override val label: String,
        val default: String?,
        val action: String,
        override val visible: String,
        override val enable: Boolean
    ) : Setting()

    // FolderSetting
    data class FolderSetting(
        override val id: String,
        override val label: String,
        val default: String?,
        override val visible: String,
        override val enable: Boolean
    ) : Setting()

    data class LsepSetting(
        override val label: String
    ) : Setting() {
        override val id: String = ""
        override val visible: String = "true"
        override val enable: Boolean = true
    }
}
