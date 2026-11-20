package com.mat198.sudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Paper and ink colours for the board and screens. */
@Immutable
data class Palette(
    val paper: Color,
    val given: Color,
    val pen: Color,
    val pencil: Color,
    val gridThin: Color,
    val gridThick: Color,
    val selection: Color,
    val error: Color,
)

val PaperPalette = Palette(
    paper = Color(0xFFF7F3E8),
    given = Color(0xFF1B1B1B),
    pen = Color(0xFF1F4FA8),
    pencil = Color(0xFF6B6B6B),
    gridThin = Color(0xFFB9B2A0),
    gridThick = Color(0xFF2B2B2B),
    selection = Color(0x332F6FDB),
    error = Color(0xFFA32020),
)

/** Low-glare night palette: dim warm greys, no pure white, softened blue ink. */
val NightPalette = Palette(
    paper = Color(0xFF141311),
    given = Color(0xFFD9D3C6),
    pen = Color(0xFF7FA6E0),
    pencil = Color(0xFF8C877D),
    gridThin = Color(0xFF34322D),
    gridThick = Color(0xFFA9A396),
    selection = Color(0x403F6FCF),
    error = Color(0xFFE07A7A),
)

val LocalInk = staticCompositionLocalOf { PaperPalette }

private val paperColors = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = PaperPalette.paper,
    secondary = PaperPalette.pen,
    onSecondary = Color.White,
    background = PaperPalette.paper,
    onBackground = PaperPalette.given,
    surface = PaperPalette.paper,
    onSurface = PaperPalette.given,
    surfaceVariant = Color(0xFFE9E3D2),
    onSurfaceVariant = Color(0xFF3A3A3A),
    surfaceContainerHigh = Color(0xFFEFE9DA),
    outline = Color(0xFF8A836F),
)

private val nightColors = darkColorScheme(
    primary = Color(0xFFBDB6A8),
    onPrimary = NightPalette.paper,
    secondary = NightPalette.pen,
    onSecondary = NightPalette.paper,
    background = NightPalette.paper,
    onBackground = NightPalette.given,
    surface = NightPalette.paper,
    onSurface = NightPalette.given,
    surfaceVariant = Color(0xFF26241F),
    onSurfaceVariant = Color(0xFFB5AFA2),
    surfaceContainerHigh = Color(0xFF211F1B),
    outline = Color(0xFF5A564E),
)

@Composable
fun SudokuTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInk provides if (dark) NightPalette else PaperPalette) {
        MaterialTheme(colorScheme = if (dark) nightColors else paperColors, content = content)
    }
}
