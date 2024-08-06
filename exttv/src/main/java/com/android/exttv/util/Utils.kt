package com.android.exttv.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun parseText(input: String): AnnotatedString {
    val stripped = input.replace("\n", "; ").trim()//.replace(Regex(";\\s*;+\\s*"), ";")
    val annotatedString = AnnotatedString.Builder()
    val regex = "\\[(B|I|LIGHT|UPPERCASE|LOWERCASE|CAPITALIZE)](.*?)\\[/\\1]".toRegex()

    var currentIndex = 0
    for (match in regex.findAll(stripped)) {
        val (tag, content) = match.destructured
        val startIndex = match.range.first

        if (currentIndex < startIndex) {
            annotatedString.append(AnnotatedString(stripped.substring(currentIndex, startIndex)))
        }

        when (tag) {
            "B" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    annotatedString.append(parseText(content))
                }
            }
            "I" -> {
                annotatedString.withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    annotatedString.append(parseText(content))
                }
            }
            "LIGHT" -> {
                annotatedString.withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                    annotatedString.append(parseText(content))
                }
            }
            "UPPERCASE" -> {
                annotatedString.append(AnnotatedString(content.uppercase()))
            }
            "LOWERCASE" -> {
                annotatedString.append(AnnotatedString(content.lowercase()))
            }
            "CAPITALIZE" -> {
                annotatedString.append(AnnotatedString(content.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }))
            }
        }

        currentIndex = match.range.last + 1
    }

    if (currentIndex < stripped.length) {
        annotatedString.append(AnnotatedString(stripped.substring(currentIndex)))
    }

    return annotatedString.toAnnotatedString()
}

fun cleanText(input: String): String {
    // Remove color tags

    // Remove carriage returns
    var cleanedText = input.replace(Regex("\\[CR\\]"), "")

    // Remove tabulators
    cleanedText = cleanedText.replace(Regex("\\[TABS](\\d+)\\[/TABS]")) { match ->
        val tabs = match.groupValues[1].toIntOrNull() ?: 0
        "\t".repeat(tabs)
    }
    cleanedText = cleanedText.replace(Regex("\\[COLOR\\s+[^\\]]*](.*?)\\[/COLOR]"), "$1")

    return cleanedText
}
