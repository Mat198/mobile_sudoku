package com.mat198.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mat198.sudoku.game.Game

/**
 * The grid. Deliberately plain: only the selected cell is marked. No highlighting of
 * related cells, matching digits, or conflicts.
 */
@Composable
fun Board(
    game: Game,
    version: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val styles = remember {
        BoardStyles(
            given = TextStyle(color = Ink.given, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            pen = TextStyle(color = Ink.pen, fontSize = 26.sp, fontFamily = FontFamily.Serif),
            note = TextStyle(color = Ink.pencil, fontSize = 11.sp, fontFamily = FontFamily.SansSerif),
        )
    }

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val cellSize = size.width / 9f
                    val c = (pos.x / cellSize).toInt().coerceIn(0, 8)
                    val r = (pos.y / cellSize).toInt().coerceIn(0, 8)
                    onSelect(r * 9 + c)
                }
            }
    ) {
        // Reading `version` ties redraws to edits of the game's (mutable) arrays.
        check(version >= 0)
        val cell = size.width / 9f

        if (selected >= 0) {
            drawRect(
                Ink.selection,
                topLeft = Offset((selected % 9) * cell, (selected / 9) * cell),
                size = Size(cell, cell),
            )
        }

        for (i in 0 until 81) {
            val x = (i % 9) * cell
            val y = (i / 9) * cell
            val v = game.values[i]
            when {
                v != 0 -> drawCentered(measurer, "$v", if (game.isGiven(i)) styles.given else styles.pen, x, y, cell, cell)
                game.notes[i] != 0 -> {
                    val sub = cell / 3f
                    for (d in 1..9) {
                        if (game.notes[i] and (1 shl d) == 0) continue
                        val nx = x + ((d - 1) % 3) * sub
                        val ny = y + ((d - 1) / 3) * sub
                        drawCentered(measurer, "$d", styles.note, nx, ny, sub, sub)
                    }
                }
            }
        }

        val thin = 1.dp.toPx()
        val thick = 3.dp.toPx()
        for (k in 0..9) {
            if (k % 3 == 0) continue
            val p = k * cell
            drawLine(Ink.gridThin, Offset(p, 0f), Offset(p, size.height), thin)
            drawLine(Ink.gridThin, Offset(0f, p), Offset(size.width, p), thin)
        }
        for (k in 0..3) {
            val p = (k * 3 * cell).coerceIn(thick / 2, size.width - thick / 2)
            drawLine(Ink.gridThick, Offset(p, 0f), Offset(p, size.height), thick)
            drawLine(Ink.gridThick, Offset(0f, p), Offset(size.width, p), thick)
        }
    }
}

private class BoardStyles(val given: TextStyle, val pen: TextStyle, val note: TextStyle)

private fun DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
) {
    val layout = measurer.measure(text, style)
    drawText(
        layout,
        topLeft = Offset(x + (w - layout.size.width) / 2f, y + (h - layout.size.height) / 2f),
    )
}
