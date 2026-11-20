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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.MIN_CLUES
import com.mat198.sudoku.engine.Verdict
import com.mat198.sudoku.game.CheckResult
import com.mat198.sudoku.game.CustomPuzzle
import com.mat198.sudoku.game.GameViewModel
import com.mat198.sudoku.game.Screen

@Composable
fun SudokuApp(vm: GameViewModel, dark: Boolean) {
    SudokuTheme(dark) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LocalInk.current.paper)
                .safeDrawingPadding()
        ) {
            when (vm.screen) {
                Screen.MENU -> MenuScreen(vm)
                Screen.DIFFICULTY -> DifficultyScreen(vm)
                Screen.GAME -> GameScreen(vm)
                Screen.CUSTOM_LIST -> CustomListScreen(vm)
                Screen.EDITOR -> EditorScreen(vm)
            }
        }
    }
}

// --- menu ----------------------------------------------------------------------

@Composable
private fun MenuScreen(vm: GameViewModel) {
    val ink = LocalInk.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Sudoku", fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Text("pen & paper", color = ink.pencil, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(64.dp))
        MenuButton("Continue", enabled = vm.hasSavedGame) { vm.continueGame() }
        Spacer(Modifier.height(16.dp))
        MenuButton("New game") { vm.showDifficulty() }
        Spacer(Modifier.height(16.dp))
        MenuButton("Custom games") { vm.showCustomGames() }
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = vm::cycleTheme) {
            Text("Theme: ${vm.theme.label}", fontSize = 16.sp)
        }
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
        ReplaceWarning(vm)
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
private fun ReplaceWarning(vm: GameViewModel) {
    if (vm.hasSavedGame) {
        Text(
            "Starting a new game replaces your current one.",
            color = LocalInk.current.pencil,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
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

// --- game ----------------------------------------------------------------------

@Composable
private fun GameScreen(vm: GameViewModel) {
    val game = vm.game ?: return
    val ink = LocalInk.current
    // Reading the version here refreshes undo/redo/clear availability after each edit.
    val version = vm.version
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler { vm.backToMenu() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: back to menu, title, clock.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.backToMenu() }) { Text("‹ Menu", fontSize = 16.sp) }
            Text(
                game.title,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatTime(vm.elapsedMs),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        // Shrinks below full width only if the screen is too short to fit everything.
        Board(
            values = game.values,
            notes = game.notes,
            isGiven = game::isGiven,
            version = version,
            selected = vm.selected,
            onSelect = vm::select,
            modifier = Modifier.weight(1f, fill = false),
        )

        // Status line: only ever says "solved" or "not yet" — never where a mistake is.
        Box(
            Modifier
                .height(52.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                vm.checkResult == CheckResult.WRONG ->
                    Text("Not quite. Something is wrong somewhere.", color = ink.error, fontSize = 16.sp)
                game.isFilled && vm.checkResult == CheckResult.NONE ->
                    Button(onClick = vm::checkSolution) { Text("Check solution", fontSize = 16.sp) }
            }
        }

        // Tools: reverse, redo, erase, clear everything.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("↶ Undo", enabled = game.canUndo, onClick = vm::undo)
            ToolButton("↷ Redo", enabled = game.canRedo, onClick = vm::redo)
            ToolButton("⌫ Erase", onClick = vm::erase)
            ToolButton("Clear", enabled = game.hasEntries, onClick = { confirmClear = true })
        }

        Spacer(Modifier.height(8.dp))
        ModeSwitch(noteMode = vm.noteMode, onToggle = vm::toggleMode)
        Spacer(Modifier.height(8.dp))
        NumberPad(noteMode = vm.noteMode, onDigit = vm::input)
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear everything?",
            text = "All your numbers and notes will be erased. The clock keeps running. You can undo this.",
            confirm = "Clear",
            onConfirm = {
                confirmClear = false
                vm.clearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }

    if (vm.checkResult == CheckResult.SOLVED) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Solved!") },
            text = { Text("${game.title} solved in ${formatTime(vm.elapsedMs)}.") },
            confirmButton = { TextButton(onClick = vm::finishSolved) { Text("Menu") } },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ToolButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(text, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun ModeSwitch(noteMode: Boolean, onToggle: () -> Unit) {
    val ink = LocalInk.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Pen",
            fontSize = 16.sp,
            fontWeight = if (!noteMode) FontWeight.Bold else FontWeight.Normal,
            color = if (!noteMode) ink.pen else ink.pencil,
        )
        Spacer(Modifier.width(10.dp))
        Switch(checked = noteMode, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(10.dp))
        Text(
            "Notes",
            fontSize = 16.sp,
            fontWeight = if (noteMode) FontWeight.Bold else FontWeight.Normal,
            color = if (noteMode) ink.given else ink.pencil,
        )
    }
}

@Composable
private fun NumberPad(noteMode: Boolean, onDigit: (Int) -> Unit) {
    val ink = LocalInk.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val d = row * 3 + col + 1
                    OutlinedButton(
                        onClick = { onDigit(d) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Text(
                            "$d",
                            fontSize = if (noteMode) 18.sp else 26.sp,
                            color = if (noteMode) ink.pencil else ink.pen,
                            fontFamily = FontFamily.Serif,
                        )
                    }
                }
            }
        }
    }
}

// --- custom games ----------------------------------------------------------------

@Composable
private fun CustomListScreen(vm: GameViewModel) {
    val ink = LocalInk.current
    var toDelete by remember { mutableStateOf<CustomPuzzle?>(null) }
    BackHandler { vm.backToMenu() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.backToMenu() }) { Text("‹ Menu", fontSize = 16.sp) }
        }
        Text("Custom games", fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(8.dp))
        ReplaceWarning(vm)
        Spacer(Modifier.height(24.dp))
        MenuButton("Create a puzzle") { vm.openEditor() }
        Spacer(Modifier.height(24.dp))

        if (vm.customGames.isEmpty()) {
            Text(
                "No saved puzzles yet. Copy one from a newspaper or book, or make your own.",
                color = ink.pencil,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(vm.customGames.asReversed(), key = { it.id }) { puzzle ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                puzzle.name,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${puzzle.clues} clues", fontSize = 14.sp, color = ink.pencil)
                        }
                        TextButton(onClick = { toDelete = puzzle }) { Text("Delete", color = ink.error) }
                        Button(onClick = { vm.playCustom(puzzle) }) { Text("Play") }
                    }
                    HorizontalDivider(color = ink.gridThin)
                }
            }
        }
    }

    toDelete?.let { puzzle ->
        ConfirmDialog(
            title = "Delete \"${puzzle.name}\"?",
            text = "The puzzle will be removed from your list.",
            confirm = "Delete",
            onConfirm = {
                vm.deleteCustom(puzzle)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun EditorScreen(vm: GameViewModel) {
    val ink = LocalInk.current
    val version = vm.editorVersion
    val clues = vm.editorClues
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler { vm.backToCustomList() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.backToCustomList() }) { Text("‹ Back", fontSize = 16.sp) }
            Text(
                "New puzzle",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$clues / $MIN_CLUES+",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = if (clues >= MIN_CLUES) ink.given else ink.pencil,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Board(
            values = vm.editorGrid,
            notes = null,
            isGiven = { true },
            version = version,
            selected = vm.editorSelected,
            onSelect = vm::editorSelect,
            modifier = Modifier.weight(1f, fill = false),
        )

        Box(
            Modifier
                .height(52.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Enter the given digits (at least $MIN_CLUES), then tap Done.",
                color = ink.pencil,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("⌫ Erase", onClick = vm::editorErase)
            ToolButton("Clear", enabled = clues > 0, onClick = { confirmClear = true })
            Button(
                onClick = vm::editorDone,
                enabled = clues >= MIN_CLUES && !vm.editorBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                if (vm.editorBusy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Done", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        NumberPad(noteMode = false, onDigit = vm::editorInput)
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear the grid?",
            text = "Every digit you entered will be erased.",
            confirm = "Clear",
            onConfirm = {
                confirmClear = false
                vm.editorClear()
            },
            onDismiss = { confirmClear = false },
        )
    }

    vm.editorAnalysis?.let { analysis ->
        if (analysis.playable) {
            NameDialog(
                initial = remember(analysis) { vm.defaultCustomName() },
                warning = if (analysis.verdict == Verdict.MULTIPLE_SOLUTIONS) {
                    GameViewModel.verdictMessage(analysis.verdict)
                } else {
                    null
                },
                onSave = vm::saveCustomAndPlay,
                onDismiss = vm::dismissAnalysis,
            )
        } else {
            AlertDialog(
                onDismissRequest = vm::dismissAnalysis,
                title = { Text("Can't play this yet") },
                text = { Text(GameViewModel.verdictMessage(analysis.verdict)) },
                confirmButton = { TextButton(onClick = vm::dismissAnalysis) { Text("OK") } },
            )
        }
    }
}

@Composable
private fun NameDialog(
    initial: String,
    warning: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Puzzle is solvable") },
        text = {
            Column {
                if (warning != null) {
                    Text(warning, color = LocalInk.current.error, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }
                Text("Give it a name to save it to your custom games.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Save & play") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep editing") } },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = LocalInk.current.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
