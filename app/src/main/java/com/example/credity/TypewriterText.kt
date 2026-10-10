package com.example.credity

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/**
 * Animated Typewriter Text component for AI text generation.
 * Gradually reveals characters with markdown bold parsing for a high-tech AI streaming look.
 */
@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    normalColor: Color = Color.White,
    boldColor: Color = Color.White,
    delayMillisPerChar: Long = 10L
) {
    var visibleCharCount by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        visibleCharCount = 0
        while (visibleCharCount < text.length) {
            delay(delayMillisPerChar)
            visibleCharCount = (visibleCharCount + 2).coerceAtMost(text.length)
        }
    }

    val visibleSubstring = text.take(visibleCharCount)
    Text(
        text = parseMarkdownToAnnotatedString(visibleSubstring, normalColor, boldColor),
        style = style,
        modifier = modifier
    )
}
