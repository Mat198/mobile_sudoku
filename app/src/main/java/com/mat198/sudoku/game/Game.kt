package com.mat198.sudoku.game

import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.Puzzle
import com.mat198.sudoku.engine.Solver
import org.json.JSONArray
import org.json.JSONObject

/** One cell before and after an edit. */
data class CellChange(
    val cell: Int,
    val oldValue: Int,
    val oldNotes: Int,
    val newValue: Int,
    val newNotes: Int,
)

/** Everything one tap changed (a single cell, or the whole board for "clear"). */
typealias Action = List<CellChange>

/**
 * The paper: given digits, what the player wrote (values and pencil notes) and the clock.
 * Nothing here ever tells the player whether an entry is right; only [isCorrect] does,
 * and it is only offered once every cell is filled.
 */
class Game(
    /** Shown on the game screen: the difficulty, or a custom puzzle's name. */
    val title: String,
    /** Null for a custom puzzle. */
    val difficulty: Difficulty?,
    val givens: IntArray,
    val solution: IntArray,
    val values: IntArray = givens.copyOf(),
    /** Bit d (1..9) set means the note d is pencilled in that cell. */
    val notes: IntArray = IntArray(81),
    var elapsedMs: Long = 0L,
    private val undoStack: ArrayDeque<Action> = ArrayDeque(),
    private val redoStack: ArrayDeque<Action> = ArrayDeque(),
) {
    constructor(difficulty: Difficulty, puzzle: Puzzle) :
        this(difficulty.label, difficulty, puzzle.givens, puzzle.solution)

    constructor(name: String, puzzle: Puzzle) :
        this(name, null, puzzle.givens, puzzle.solution)

    fun isGiven(cell: Int) = givens[cell] != 0

    val isFilled: Boolean get() = values.all { it != 0 }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val hasEntries: Boolean get() = (0 until 81).any { !isGiven(it) && (values[it] != 0 || notes[it] != 0) }

    /**
     * Any grid that keeps the givens and follows the rules counts, so a custom puzzle with
     * several solutions is judged fairly. For a unique puzzle this equals the stored solution.
     */
    fun isCorrect(): Boolean =
        (0 until 81).all { !isGiven(it) || values[it] == givens[it] } && Solver.isValidSolution(values)

    fun writeValue(cell: Int, digit: Int): Boolean {
        if (isGiven(cell) || values[cell] == digit) return false
        return apply(listOf(change(cell, digit, notes[cell])))
    }

    fun toggleNote(cell: Int, digit: Int): Boolean {
        if (isGiven(cell) || values[cell] != 0) return false
        return apply(listOf(change(cell, 0, notes[cell] xor (1 shl digit))))
    }

    fun erase(cell: Int): Boolean {
        if (isGiven(cell) || (values[cell] == 0 && notes[cell] == 0)) return false
        return apply(listOf(change(cell, 0, 0)))
    }

    /** Wipes every number and note the player wrote, as one undoable step. */
    fun clearAll(): Boolean =
        apply((0 until 81).filter { !isGiven(it) && (values[it] != 0 || notes[it] != 0) }.map { change(it, 0, 0) })

    /** Returns the reversed action, or null when there is nothing to undo. */
    fun undo(): Action? {
        val action = undoStack.removeLastOrNull() ?: return null
        for (c in action) {
            values[c.cell] = c.oldValue
            notes[c.cell] = c.oldNotes
        }
        redoStack.addLast(action)
        return action
    }

    fun redo(): Action? {
        val action = redoStack.removeLastOrNull() ?: return null
        for (c in action) {
            values[c.cell] = c.newValue
            notes[c.cell] = c.newNotes
        }
        undoStack.addLast(action)
        return action
    }

    private fun change(cell: Int, value: Int, note: Int) =
        CellChange(cell, values[cell], notes[cell], value, note)

    private fun apply(action: Action): Boolean {
        if (action.isEmpty()) return false
        for (c in action) {
            values[c.cell] = c.newValue
            notes[c.cell] = c.newNotes
        }
        undoStack.addLast(action)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        return true
    }

    fun toJson(): String = JSONObject().apply {
        put("title", title)
        difficulty?.let { put("difficulty", it.name) }
        put("givens", digits(givens))
        put("solution", digits(solution))
        put("values", digits(values))
        put("notes", JSONArray().also { a -> notes.forEach { a.put(it) } })
        put("elapsedMs", elapsedMs)
        put("undo", actionsToJson(undoStack))
        put("redo", actionsToJson(redoStack))
    }.toString()

    companion object {
        private const val MAX_HISTORY = 1000

        fun digits(a: IntArray) = a.joinToString("")
        fun parseDigits(s: String) = IntArray(81) { s[it] - '0' }

        private fun actionsToJson(actions: Collection<Action>) = JSONArray().also { a ->
            for (action in actions) {
                a.put(JSONArray().also { cells ->
                    for (c in action) {
                        cells.put(JSONArray().put(c.cell).put(c.oldValue).put(c.oldNotes).put(c.newValue).put(c.newNotes))
                    }
                })
            }
        }

        private fun actionsFromJson(a: JSONArray?): ArrayDeque<Action> {
            val out = ArrayDeque<Action>()
            if (a == null) return out
            for (i in 0 until a.length()) {
                val cells = a.getJSONArray(i)
                out.addLast(List(cells.length()) { j ->
                    val c = cells.getJSONArray(j)
                    CellChange(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4))
                })
            }
            return out
        }

        fun fromJson(json: String): Game {
            val o = JSONObject(json)
            val notesJson = o.getJSONArray("notes")
            val difficulty = o.optString("difficulty").takeIf { it.isNotEmpty() }?.let { Difficulty.valueOf(it) }
            return Game(
                title = o.optString("title").ifEmpty { difficulty?.label ?: "Custom" },
                difficulty = difficulty,
                givens = parseDigits(o.getString("givens")),
                solution = parseDigits(o.getString("solution")),
                values = parseDigits(o.getString("values")),
                notes = IntArray(81) { notesJson.getInt(it) },
                elapsedMs = o.getLong("elapsedMs"),
                // Saves from version 1.0 kept a one-way "history"; it is simply dropped.
                undoStack = actionsFromJson(o.optJSONArray("undo")),
                redoStack = actionsFromJson(o.optJSONArray("redo")),
            )
        }
    }
}

/** A hand-entered puzzle kept in the custom games list. */
class CustomPuzzle(val id: Long, val name: String, val givens: IntArray, val solution: IntArray) {
    val clues: Int get() = givens.count { it != 0 }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("givens", Game.digits(givens))
        .put("solution", Game.digits(solution))

    companion object {
        fun listToJson(list: List<CustomPuzzle>): String =
            JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }.toString()

        fun listFromJson(json: String): List<CustomPuzzle> {
            val a = JSONArray(json)
            return List(a.length()) { i ->
                val o = a.getJSONObject(i)
                CustomPuzzle(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    givens = Game.parseDigits(o.getString("givens")),
                    solution = Game.parseDigits(o.getString("solution")),
                )
            }
        }
    }
}
