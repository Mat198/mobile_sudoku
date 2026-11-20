package com.mat198.sudoku.game

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mat198.sudoku.engine.Analysis
import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.Generator
import com.mat198.sudoku.engine.Puzzle
import com.mat198.sudoku.engine.Solver
import com.mat198.sudoku.engine.Verdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { MENU, DIFFICULTY, GAME, CUSTOM_LIST, EDITOR }

enum class CheckResult { NONE, WRONG, SOLVED }

enum class ThemeMode(val label: String) { SYSTEM("System"), PAPER("Paper"), NIGHT("Night") }

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    private val seeds: List<String> by lazy {
        app.assets.open("puzzles17.txt").bufferedReader().readLines().filter { it.length >= 81 }
    }

    var screen by mutableStateOf(Screen.MENU)
        private set
    var game by mutableStateOf<Game?>(null)
        private set
    var hasSavedGame by mutableStateOf(prefs.contains(KEY_GAME))
        private set
    var generating by mutableStateOf(false)
        private set
    var theme by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM)
    )
        private set

    /** Bumped on every edit so the board redraws (the game's arrays are mutated in place). */
    var version by mutableIntStateOf(0)
        private set
    var selected by mutableIntStateOf(-1)
        private set
    var noteMode by mutableStateOf(false)
        private set
    var checkResult by mutableStateOf(CheckResult.NONE)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set

    private var resumed = true
    private var clockJob: Job? = null
    private var runningSince = 0L

    // --- navigation ----------------------------------------------------------

    fun showDifficulty() {
        screen = Screen.DIFFICULTY
    }

    fun showCustomGames() {
        screen = Screen.CUSTOM_LIST
    }

    fun backToMenu() {
        stopClock()
        save()
        screen = Screen.MENU
    }

    fun backToCustomList() {
        screen = Screen.CUSTOM_LIST
    }

    fun cycleTheme() {
        theme = ThemeMode.entries[(theme.ordinal + 1) % ThemeMode.entries.size]
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    // --- starting games ------------------------------------------------------

    fun continueGame() {
        val json = prefs.getString(KEY_GAME, null) ?: return
        val loaded = runCatching { Game.fromJson(json) }.getOrNull()
        if (loaded == null) {
            prefs.edit().remove(KEY_GAME).apply()
            hasSavedGame = false
            return
        }
        open(loaded)
    }

    fun newGame(difficulty: Difficulty) {
        if (generating) return
        generating = true
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator.generate(difficulty, seeds) }
            generating = false
            open(Game(difficulty, puzzle))
            save()
        }
    }

    fun playCustom(puzzle: CustomPuzzle) {
        open(Game(puzzle.name, Puzzle(puzzle.givens.copyOf(), puzzle.solution.copyOf())))
        save()
    }

    private fun open(g: Game) {
        game = g
        selected = -1
        noteMode = false
        checkResult = CheckResult.NONE
        elapsedMs = g.elapsedMs
        version++
        screen = Screen.GAME
        startClock()
    }

    // --- playing -------------------------------------------------------------

    fun select(cell: Int) {
        selected = cell
    }

    fun toggleMode() {
        noteMode = !noteMode
    }

    private val playing: Game?
        get() = game?.takeIf { checkResult != CheckResult.SOLVED }

    fun input(digit: Int) {
        val g = playing ?: return
        val cell = selected
        if (cell < 0) return
        val changed = if (noteMode) g.toggleNote(cell, digit) else g.writeValue(cell, digit)
        if (changed) edited()
    }

    fun erase() {
        val g = playing ?: return
        if (selected >= 0 && g.erase(selected)) edited()
    }

    fun clearAll() {
        val g = playing ?: return
        if (g.clearAll()) edited()
    }

    fun undo() {
        val g = playing ?: return
        val action = g.undo() ?: return
        action.singleOrNull()?.let { selected = it.cell }
        edited()
    }

    fun redo() {
        val g = playing ?: return
        val action = g.redo() ?: return
        action.singleOrNull()?.let { selected = it.cell }
        edited()
    }

    fun checkSolution() {
        val g = game ?: return
        if (!g.isFilled) return
        if (g.isCorrect()) {
            stopClock()
            checkResult = CheckResult.SOLVED
            prefs.edit().remove(KEY_GAME).apply()
            hasSavedGame = false
        } else {
            checkResult = CheckResult.WRONG
        }
    }

    fun finishSolved() {
        game = null
        screen = Screen.MENU
    }

    private fun edited() {
        checkResult = CheckResult.NONE
        version++
        save()
    }

    // --- custom games ----------------------------------------------------------

    var customGames by mutableStateOf(loadCustomGames())
        private set

    /** The grid being typed in by the player in the editor. */
    val editorGrid = IntArray(81)
    var editorVersion by mutableIntStateOf(0)
        private set
    var editorSelected by mutableIntStateOf(-1)
        private set
    var editorBusy by mutableStateOf(false)
        private set

    /** Result of pressing "Done" in the editor; drives the error / naming dialogs. */
    var editorAnalysis by mutableStateOf<Analysis?>(null)
        private set

    val editorClues: Int get() = editorGrid.count { it != 0 }

    fun openEditor() {
        editorGrid.fill(0)
        editorSelected = -1
        editorAnalysis = null
        editorVersion++
        screen = Screen.EDITOR
    }

    fun editorSelect(cell: Int) {
        editorSelected = cell
    }

    fun editorInput(digit: Int) {
        if (editorSelected < 0) return
        editorGrid[editorSelected] = if (editorGrid[editorSelected] == digit) 0 else digit
        editorVersion++
    }

    fun editorErase() {
        if (editorSelected < 0 || editorGrid[editorSelected] == 0) return
        editorGrid[editorSelected] = 0
        editorVersion++
    }

    fun editorClear() {
        editorGrid.fill(0)
        editorVersion++
    }

    fun editorDone() {
        if (editorBusy) return
        editorBusy = true
        val grid = editorGrid.copyOf()
        viewModelScope.launch {
            editorAnalysis = withContext(Dispatchers.Default) { Solver.analyze(grid) }
            editorBusy = false
        }
    }

    fun dismissAnalysis() {
        editorAnalysis = null
    }

    fun defaultCustomName(): String {
        val taken = customGames.map { it.name }.toSet()
        return generateSequence(customGames.size + 1) { it + 1 }.map { "Custom $it" }.first { it !in taken }
    }

    /** Saves the verified editor grid under [name] and starts playing it. */
    fun saveCustomAndPlay(name: String) {
        val analysis = editorAnalysis?.takeIf { it.playable } ?: return
        val solution = analysis.solution ?: return
        val puzzle = CustomPuzzle(
            id = System.currentTimeMillis(),
            name = name.trim().ifEmpty { defaultCustomName() },
            givens = editorGrid.copyOf(),
            solution = solution,
        )
        customGames = customGames + puzzle
        storeCustomGames()
        editorAnalysis = null
        playCustom(puzzle)
    }

    fun deleteCustom(puzzle: CustomPuzzle) {
        customGames = customGames.filter { it.id != puzzle.id }
        storeCustomGames()
    }

    private fun loadCustomGames(): List<CustomPuzzle> =
        prefs.getString(KEY_CUSTOM, null)
            ?.let { runCatching { CustomPuzzle.listFromJson(it) }.getOrNull() }
            .orEmpty()

    private fun storeCustomGames() {
        prefs.edit().putString(KEY_CUSTOM, CustomPuzzle.listToJson(customGames)).apply()
    }

    // --- clock -----------------------------------------------------------------

    fun onResume() {
        resumed = true
        if (screen == Screen.GAME) startClock()
    }

    fun onPause() {
        resumed = false
        stopClock()
        save()
    }

    private fun startClock() {
        val g = game ?: return
        if (!resumed || clockJob != null || checkResult == CheckResult.SOLVED) return
        runningSince = SystemClock.elapsedRealtime()
        val base = g.elapsedMs
        clockJob = viewModelScope.launch {
            while (isActive) {
                elapsedMs = base + (SystemClock.elapsedRealtime() - runningSince)
                delay(250)
            }
        }
    }

    private fun stopClock() {
        val job = clockJob ?: return
        job.cancel()
        clockJob = null
        game?.let {
            it.elapsedMs += SystemClock.elapsedRealtime() - runningSince
            elapsedMs = it.elapsedMs
        }
    }

    private fun save() {
        val g = game ?: return
        if (checkResult == CheckResult.SOLVED) return
        // Capture the running clock without stopping it.
        val saved = g.elapsedMs
        if (clockJob != null) g.elapsedMs = saved + (SystemClock.elapsedRealtime() - runningSince)
        prefs.edit().putString(KEY_GAME, g.toJson()).apply()
        g.elapsedMs = saved
        hasSavedGame = true
    }

    companion object {
        private const val KEY_GAME = "game"
        private const val KEY_THEME = "theme"
        private const val KEY_CUSTOM = "custom"

        fun verdictMessage(v: Verdict): String = when (v) {
            Verdict.TOO_FEW_CLUES -> "Enter at least 17 digits."
            Verdict.CONFLICT -> "A digit repeats in a row, column or box."
            Verdict.NO_SOLUTION -> "This puzzle has no solution."
            Verdict.MULTIPLE_SOLUTIONS ->
                "This puzzle has more than one solution. Add more digits to make the solution unique."
            Verdict.UNIQUE -> "This puzzle has exactly one solution."
        }
    }
}
