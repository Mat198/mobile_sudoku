package com.mat198.sudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paper and ink. */
object Ink {
    val paper = Color(0xFFF7F3E8)
    val given = Color(0xFF1B1B1B)
    val pen = Color(0xFF1F4FA8)
    val pencil = Color(0xFF6B6B6B)
    val gridThin = Color(0xFFB9B2A0)
    val gridThick = Color(0xFF2B2B2B)
    val selection = Color(0x332F6FDB)
    val error = Color(0xFFA32020)
}

private val colors = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Ink.paper,
    secondary = Ink.pen,
    onSecondary = Color.White,
    background = Ink.paper,
    onBackground = Ink.given,
    surface = Ink.paper,
    onSurface = Ink.given,
    surfaceVariant = Color(0xFFE9E3D2),
    onSurfaceVariant = Color(0xFF3A3A3A),
    outline = Color(0xFF8A836F),
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
