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
import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.Generator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { MENU, DIFFICULTY, GAME }

enum class CheckResult { NONE, WRONG, SOLVED }

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

    fun showDifficulty() {
        screen = Screen.DIFFICULTY
    }

    fun backToMenu() {
        stopClock()
        save()
        screen = Screen.MENU
    }

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

    fun select(cell: Int) {
        selected = cell
    }

    fun toggleMode() {
        noteMode = !noteMode
    }

    fun input(digit: Int) {
        val g = game ?: return
        val cell = selected
        if (cell < 0 || checkResult == CheckResult.SOLVED) return
        val changed = if (noteMode) g.toggleNote(cell, digit) else g.writeValue(cell, digit)
        if (changed) edited()
    }

    fun erase() {
        val g = game ?: return
        if (selected < 0 || checkResult == CheckResult.SOLVED) return
        if (g.erase(selected)) edited()
    }

    fun undo() {
        val g = game ?: return
        if (checkResult == CheckResult.SOLVED) return
        val cell = g.undo() ?: return
        selected = cell
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

    // --- clock -------------------------------------------------------------

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
    }
}
