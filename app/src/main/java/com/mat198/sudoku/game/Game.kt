package com.mat198.sudoku.game

import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.Puzzle
import org.json.JSONArray
import org.json.JSONObject

/** Previous contents of a cell, recorded before every change so it can be reversed. */
data class Move(val cell: Int, val value: Int, val notes: Int)

/**
 * The paper: given digits, what the player wrote (values and pencil notes) and the clock.
 * Nothing here ever tells the player whether an entry is right; only [isCorrect] does,
 * and it is only offered once every cell is filled.
 */
class Game(
    val difficulty: Difficulty,
    val givens: IntArray,
    val solution: IntArray,
    val values: IntArray = givens.copyOf(),
    /** Bit d (1..9) set means the note d is pencilled in that cell. */
    val notes: IntArray = IntArray(81),
    var elapsedMs: Long = 0L,
    val history: ArrayDeque<Move> = ArrayDeque(),
) {
    constructor(difficulty: Difficulty, puzzle: Puzzle) :
        this(difficulty, puzzle.givens, puzzle.solution)

    fun isGiven(cell: Int) = givens[cell] != 0

    val isFilled: Boolean get() = values.all { it != 0 }

    fun isCorrect(): Boolean = values.contentEquals(solution)

    fun writeValue(cell: Int, digit: Int): Boolean {
        if (isGiven(cell) || values[cell] == digit) return false
        record(cell)
        values[cell] = digit
        return true
    }

    fun toggleNote(cell: Int, digit: Int): Boolean {
        if (isGiven(cell) || values[cell] != 0) return false
        record(cell)
        notes[cell] = notes[cell] xor (1 shl digit)
        return true
    }

    fun erase(cell: Int): Boolean {
        if (isGiven(cell) || (values[cell] == 0 && notes[cell] == 0)) return false
        record(cell)
        values[cell] = 0
        notes[cell] = 0
        return true
    }

    fun undo(): Int? {
        val move = history.removeLastOrNull() ?: return null
        values[move.cell] = move.value
        notes[move.cell] = move.notes
        return move.cell
    }

    private fun record(cell: Int) {
        history.addLast(Move(cell, values[cell], notes[cell]))
        while (history.size > MAX_HISTORY) history.removeFirst()
    }

    fun toJson(): String = JSONObject().apply {
        put("difficulty", difficulty.name)
        put("givens", digits(givens))
        put("solution", digits(solution))
        put("values", digits(values))
        put("notes", JSONArray().also { a -> notes.forEach { a.put(it) } })
        put("elapsedMs", elapsedMs)
        put("history", JSONArray().also { a ->
            history.forEach { a.put(JSONArray().put(it.cell).put(it.value).put(it.notes)) }
        })
    }.toString()

    companion object {
        private const val MAX_HISTORY = 1000

        private fun digits(a: IntArray) = a.joinToString("")
        private fun parseDigits(s: String) = IntArray(81) { s[it] - '0' }

        fun fromJson(json: String): Game {
            val o = JSONObject(json)
            val notesJson = o.getJSONArray("notes")
            val historyJson = o.getJSONArray("history")
            val history = ArrayDeque<Move>()
            for (i in 0 until historyJson.length()) {
                val m = historyJson.getJSONArray(i)
                history.addLast(Move(m.getInt(0), m.getInt(1), m.getInt(2)))
            }
            return Game(
                difficulty = Difficulty.valueOf(o.getString("difficulty")),
                givens = parseDigits(o.getString("givens")),
                solution = parseDigits(o.getString("solution")),
                values = parseDigits(o.getString("values")),
                notes = IntArray(81) { notesJson.getInt(it) },
                elapsedMs = o.getLong("elapsedMs"),
                history = history,
            )
        }
    }
}
