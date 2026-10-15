package com.mat198.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.game.CheckResult
import com.mat198.sudoku.game.GameViewModel
import com.mat198.sudoku.game.Screen

@Composable
fun SudokuApp(vm: GameViewModel) {
    SudokuTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink.paper)
                .safeDrawingPadding()
        ) {
            when (vm.screen) {
                Screen.MENU -> MenuScreen(vm)
                Screen.DIFFICULTY -> DifficultyScreen(vm)
                Screen.GAME -> GameScreen(vm)
            }
        }
    }
}

@Composable
private fun MenuScreen(vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Sudoku", fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Text("pen & paper", color = Ink.pencil, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(64.dp))
        MenuButton("Continue", enabled = vm.hasSavedGame) { vm.continueGame() }
        Spacer(Modifier.height(16.dp))
        MenuButton("New game") { vm.showDifficulty() }
    }
}

@Composable
private fun DifficultyScreen(vm: GameViewModel) {
    BackHandler { vm.backToMenu() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Difficulty", fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        if (vm.hasSavedGame) {
            Text("Starting a new game replaces your current one.", color = Ink.pencil, fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))
        if (vm.generating) {
            CircularProgressIndicator()
        } else {
            for (d in Difficulty.entries) {
                MenuButton("${d.label}  ·  ${d.clues} clues") { vm.newGame(d) }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { vm.backToMenu() }) { Text("Back") }
        }
    }
}

@Composable
private fun MenuButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(text, fontSize = 18.sp)
    }
}

@Composable
private fun GameScreen(vm: GameViewModel) {
    val game = vm.game ?: return
    BackHandler { vm.backToMenu() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: back to menu, difficulty, clock.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.backToMenu() }) { Text("‹ Menu", fontSize = 16.sp) }
            Text(
                game.difficulty.label,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                formatTime(vm.elapsedMs),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Board(
            game = game,
            version = vm.version,
            selected = vm.selected,
            onSelect = vm::select,
            modifier = Modifier.fillMaxWidth(),
        )

        // Status line: only ever says "solved" or "not yet" — never where a mistake is.
        Box(Modifier.height(56.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                vm.checkResult == CheckResult.WRONG ->
                    Text("Not quite. Something is wrong somewhere.", color = Ink.error, fontSize = 16.sp)
                game.isFilled && vm.checkResult == CheckResult.NONE ->
                    Button(onClick = vm::checkSolution) { Text("Check solution", fontSize = 16.sp) }
            }
        }

        // Tools: reverse, erase, pen/pencil switch.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = vm::undo, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Text("↶ Undo", fontSize = 16.sp)
            }
            OutlinedButton(onClick = vm::erase, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Text("⌫ Erase", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pen",
                    fontWeight = if (!vm.noteMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!vm.noteMode) Ink.pen else Ink.pencil,
                )
                Spacer(Modifier.width(6.dp))
                Switch(checked = vm.noteMode, onCheckedChange = { vm.toggleMode() })
                Spacer(Modifier.width(6.dp))
                Text(
                    "Notes",
                    fontWeight = if (vm.noteMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (vm.noteMode) Ink.given else Ink.pencil,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        NumberPad(noteMode = vm.noteMode, onDigit = vm::input)
    }

    if (vm.checkResult == CheckResult.SOLVED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Solved!") },
            text = { Text("${game.difficulty.label} puzzle solved in ${formatTime(vm.elapsedMs)}.") },
            confirmButton = { TextButton(onClick = vm::finishSolved) { Text("Menu") } },
        )
    }
}

@Composable
private fun NumberPad(noteMode: Boolean, onDigit: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val d = row * 3 + col + 1
                    OutlinedButton(
                        onClick = { onDigit(d) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Text(
                            "$d",
                            fontSize = if (noteMode) 18.sp else 26.sp,
                            color = if (noteMode) Ink.pencil else Ink.pen,
                            fontFamily = FontFamily.Serif,
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
