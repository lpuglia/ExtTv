package com.android.exttv.model.manager

import com.android.exttv.model.data.*
import com.android.exttv.model.data.Setting.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun parsePoFile(poFile: File): Map<String, String> {
    // Regular expression to match msgctxt and msgid
    val poContent = poFile.readText()
    val pattern = Pattern.compile("""msgctxt\s+"(.*?)"\s*msgid\s+"(.*?)"\s*msgstr\s*""")
    val matcher = pattern.matcher(poContent)

    val result = mutableMapOf<String, String>()

    while (matcher.find()) {
        val msgctxt = matcher.group(1).drop(1)
        val msgid = matcher.group(2)
        result[msgctxt] = msgid
    }

    return result
}

// Function to parse XML file and create Settings object
fun readSettingMeta(pluginName: String, settingsMap: MutableMap<String, String> = mutableMapOf()): Settings {
    val settings = mutableListOf<Category>()

    // Load and parse the XML document
    val settingMetaFile = AddonManager.addonsPath.resolve("$pluginName/resources/settings.xml")
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.parse(settingMetaFile)

    val poFile = AddonManager.addonsPath.resolve("$pluginName/resources/language/resource.language.en_gb/strings.po")
    val poMap = mutableMapOf<String, String>()
    poMap.putAll(parsePoFile(poFile))

    document.documentElement.normalize()

    val categoryNodes = document.getElementsByTagName("category")
    for (i in 0 until categoryNodes.length) {
        val categoryNode = categoryNodes.item(i)
        if (categoryNode.nodeType == Node.ELEMENT_NODE) {
            val categoryElement = categoryNode as Element
            val label = categoryElement.getAttribute("label")
            val settingsList = mutableListOf<Setting>()

            val settingNodes = categoryElement.getElementsByTagName("setting")
            for (j in 0 until settingNodes.length) {
                val settingNode = settingNodes.item(j)
                if (settingNode.nodeType == Node.ELEMENT_NODE) {
                    val settingElement = settingNode as Element
                    val type = settingElement.getAttribute("type")
                    val labelAttr = poMap.getOrDefault(settingElement.getAttribute("label"), settingElement.getAttribute("label"))
                    val id = settingElement.getAttribute("id").takeIf { it.isNotEmpty() }.toString()
                    val default = settingElement.getAttribute("default").takeIf { it.isNotEmpty() }
                    val visible = settingElement.getAttribute("visible").takeIf { it.isNotEmpty() }?: ""
                    val enable = settingElement.getAttribute("enable").takeIf { it.isNotEmpty() }?.toBoolean() != false
                    val option = settingElement.getAttribute("option").takeIf { it.isNotEmpty() }
                    val range = settingElement.getAttribute("range").takeIf { it.isNotEmpty() }
                    val values = settingElement.getAttribute("values").takeIf { it.isNotEmpty() }
                    val lvalues = settingElement.getAttribute("lvalues").takeIf { it.isNotEmpty() }
                    val action = settingElement.getAttribute("action").takeIf { it.isNotEmpty() }

                    val setting = when (type) {
                        "bool" -> BoolSetting(
                            id = id,
                            label = labelAttr,
                            default = default?.toBoolean(),
                            visible = visible.toString(),
                            currentValue = settingsMap.getOrDefault(settingsMap[id], default).toBoolean(),
                            enable = enable
                        )
                        "text" -> TextSetting(
                            id = id,
                            label = labelAttr,
                            default = default ?: "",
                            visible = visible.toString(),
                            currentValue = settingsMap.getOrDefault(settingsMap[id], default?: ""),
                            enable = enable
                        )
                        "slider" -> SliderSetting(
                            id = id,
                            label = labelAttr,
                            default = default?.toInt() ?: 0,
                            range = range ?: "0,1,1",
                            visible = visible.toString(),
                            currentValue = settingsMap.getOrDefault(settingsMap[id], default?: "0").toInt(),
                            enable = enable,
                            option = option ?: ""
                        )
                        "select" -> SelectSetting(
                            id = id,
                            label = labelAttr,
                            default = default ?: "",
                            values = values?.split("|") ?: lvalues?.split("|")?.map { poMap[it] } ?: emptyList(),
                            visible = visible.toString(),
                            currentValue = settingsMap.getOrDefault(settingsMap[id], default?: ""),
                            enable = enable
                        )
                        "action" -> ActionSetting(
                            id = id,
                            label = labelAttr,
                            action = action ?: "",
                            visible = visible.toString(),
                            default = settingsMap.getOrDefault(settingsMap[id], default),
                            enable = enable
                        )
                        "folder" -> FolderSetting(
                            id = id,
                            label = labelAttr,
                            visible = visible.toString(),
                            default = settingsMap.getOrDefault(settingsMap[id], default),
                            enable = enable
                        )
                        "lsep" -> LsepSetting(label = labelAttr)
                        else -> VoidSetting(
                            id = id,
                            label = label,
                            visible = visible,
                            enable = enable,
                            default = default ?: ""
                        )
                    }

                    setting?.let { settingsList.add(it) }
                }
            }
            settings.add(Category(label, settingsList))
        }
    }

    return Settings(settings)
}

fun dumpSettingValues(settings: Settings, pluginName: String) {
    val outputFile = AddonManager.addonsPath.resolve("../userdata/addon_data/${pluginName}/settings.xml")

    // Create a new XML Document
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.newDocument()

    // Create the root element
    val rootElement = document.createElement("settings")
    rootElement.setAttribute("version", "2")
    document.appendChild(rootElement)

    // Traverse categories and settings
    for (category in settings.categories) {
        for (setting in category.settings) {
            val settingElement = document.createElement("setting")
            settingElement.setAttribute("id", setting.id)

            when (setting) {
                is BoolSetting -> {
                    // Check if the current value is the same as the default value
                    val isDefault = setting.currentValue == setting.default
                    setting.default?.let {
                        if(!it) {
                            settingElement.setAttribute("default", "true")
                        }
                    }
                    setting.currentValue?.let { settingElement.textContent = it.toString() }
                }
                is TextSetting -> {
                    // Check if the current value is the same as the default value
                    if(setting.visible == "true"){
                        val isDefault = setting.currentValue == setting.default
                        settingElement.setAttribute("default", isDefault.toString())
                    }
                    settingElement.textContent = setting.currentValue
                }
                is SliderSetting -> {
                    // Check if the current value is the same as the default value
                    val isDefault = setting.currentValue == setting.default
                    settingElement.textContent = setting.currentValue.toString()
                }
                is SelectSetting -> {
                    // Check if the current value is the same as the default value
                    val isDefault = setting.currentValue == setting.default
                    settingElement.setAttribute("default", isDefault.toString())
                    settingElement.textContent = setting.currentValue
                }
                is ActionSetting -> {
                    settingElement.setAttribute("default", "true")
                    settingElement.textContent = setting.default
                }
                is FolderSetting -> {
                    settingElement.setAttribute("default", "true")
                    settingElement.textContent = setting.default
                }
                is LsepSetting -> {
                    // Skipping separators since they have no ID or values
                    continue
                }
                is VoidSetting -> {
                    settingElement.textContent = setting.default
                }
            }

            // Append the setting element to the root
            rootElement.appendChild(settingElement)
        }
    }

    // Transform the document into a string
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")

    val source = DOMSource(document)
    val result = StreamResult(outputFile)
    transformer.transform(source, result)
}

fun readSettingValues(pluginName: String): Settings {
    val settingsMap = mutableMapOf<String, String>()

    val settingValueFile = AddonManager.addonsPath.resolve("../userdata/addon_data/${pluginName}/settings.xml")
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document: Document = documentBuilder.parse(settingValueFile)
    val nodeList = document.getElementsByTagName("setting")

    for (i in 0 until nodeList.length) {
        val node = nodeList.item(i)
        if (node.hasAttributes()) {
            val id = node.attributes.getNamedItem("id")?.nodeValue
            val value = node.textContent?.trim() ?: ""
            if (id != null) {
                settingsMap[id] = value
            }
        }
    }
    val settings = readSettingMeta(pluginName, settingsMap)
    return settings
}