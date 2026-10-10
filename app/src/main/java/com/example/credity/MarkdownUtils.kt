package com.example.credity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parses markdown text containing **bold** delimiters into an AnnotatedString with bold styling.
 */
fun parseMarkdownToAnnotatedString(
    text: String,
    normalColor: Color = Color.White,
    boldColor: Color = Color.White
): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) { // Odd index parts were enclosed in **...**
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(parts[i])
                }
            } else {
                withStyle(style = SpanStyle(color = normalColor)) {
                    append(parts[i])
                }
            }
        }
    }
}
