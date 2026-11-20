package com.mat198.sudoku.game

import com.mat198.sudoku.engine.Difficulty
import com.mat198.sudoku.engine.Generator
import com.mat198.sudoku.engine.Solver
import com.mat198.sudoku.engine.Verdict
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class GameTest {
    private val seeds = File("src/main/assets/puzzles17.txt").readLines().filter { it.length >= 81 }
    private fun newGame() = Game(Difficulty.EXPERT, Generator.generate(Difficulty.EXPERT, seeds, Random(7)))
    private fun Game.firstEmpty() = (0 until 81).first { !isGiven(it) }

    @Test
    fun undoAndRedoWalkThroughHistory() {
        val g = newGame()
        val a = g.firstEmpty()
        g.writeValue(a, 5)
        g.toggleNote((0 until 81).filter { !g.isGiven(it) }[1], 3)
        assertEquals(5, g.values[a])

        g.undo()
        g.undo()
        assertEquals(0, g.values[a])
        assertFalse(g.canUndo)
        assertTrue(g.canRedo)

        g.redo()
        assertEquals(5, g.values[a])
        assertTrue(g.canRedo)

        // A new edit drops what was left to redo.
        g.writeValue(a, 6)
        assertFalse(g.canRedo)
        assertNull(g.redo())
    }

    @Test
    fun clearAllIsOneUndoableStep() {
        val g = newGame()
        val empties = (0 until 81).filter { !g.isGiven(it) }
        g.writeValue(empties[0], 1)
        g.writeValue(empties[1], 2)
        g.toggleNote(empties[2], 4)
        val before = g.values.copyOf()
        val notesBefore = g.notes.copyOf()

        assertTrue(g.clearAll())
        assertArrayEquals(g.givens, g.values)
        assertTrue(g.notes.all { it == 0 })
        assertFalse(g.hasEntries)
        assertFalse(g.clearAll())

        g.undo()
        assertArrayEquals(before, g.values)
        assertArrayEquals(notesBefore, g.notes)
        g.redo()
        assertArrayEquals(g.givens, g.values)
    }

    @Test
    fun correctnessFollowsRulesAndKeepsGivens() {
        val g = newGame()
        for (i in 0 until 81) if (!g.isGiven(i)) g.writeValue(i, g.solution[i])
        assertTrue(g.isCorrect())
        val i = g.firstEmpty()
        g.writeValue(i, g.solution[i] % 9 + 1)
        assertFalse(g.isCorrect())
    }

    @Test
    fun jsonRoundTripKeepsHistory() {
        val g = newGame()
        val a = g.firstEmpty()
        g.writeValue(a, 7)
        g.writeValue(a, 8)
        g.undo()
        g.elapsedMs = 12_345
        val copy = Game.fromJson(g.toJson())
        assertEquals("Expert", copy.title)
        assertEquals(Difficulty.EXPERT, copy.difficulty)
        assertArrayEquals(g.values, copy.values)
        assertEquals(12_345, copy.elapsedMs)
        copy.redo()
        assertEquals(8, copy.values[a])
        copy.undo()
        copy.undo()
        assertEquals(0, copy.values[a])
    }

    @Test
    fun oldSavesStillLoad() {
        val p = Generator.generate(Difficulty.EASY, seeds, Random(3))
        val v1 = """{"difficulty":"EASY","givens":"${Game.digits(p.givens)}","solution":"${Game.digits(p.solution)}",
            "values":"${Game.digits(p.givens)}","notes":[${IntArray(81).joinToString(",")}],"elapsedMs":5,
            "history":[[0,0,0]]}"""
        val g = Game.fromJson(v1)
        assertEquals("Easy", g.title)
        assertFalse(g.canUndo)
    }

    @Test
    fun customPuzzleListRoundTrip() {
        val p = Generator.generate(Difficulty.CHALLENGER, seeds, Random(5))
        val list = listOf(CustomPuzzle(1, "Newspaper Sunday", p.givens, p.solution))
        val back = CustomPuzzle.listFromJson(CustomPuzzle.listToJson(list))
        assertEquals("Newspaper Sunday", back[0].name)
        assertEquals(17, back[0].clues)
        assertArrayEquals(p.solution, back[0].solution)
        assertEquals(Verdict.UNIQUE, Solver.analyze(back[0].givens).verdict)
    }
}
