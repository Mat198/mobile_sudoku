package com.mat198.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class GeneratorTest {
    private val seeds = File("src/main/assets/puzzles17.txt").readLines().filter { it.length >= 81 }

    @Test
    fun seedsHaveSeventeenCluesAndUniqueSolutions() {
        // Checking all 2000 is slow-ish; a fixed sample keeps the test quick.
        for (line in seeds.shuffled(Random(1)).take(200)) {
            val grid = Generator.parse(line)
            assertEquals(line, 17, grid.count { it != 0 })
            assertEquals(line, 1, Solver.countSolutions(grid))
        }
    }

    @Test
    fun generatedPuzzlesMatchDifficultyAndAreUnique() {
        val random = Random(42)
        for (difficulty in Difficulty.entries) {
            repeat(20) {
                val p = Generator.generate(difficulty, seeds, random)
                assertEquals(difficulty.clues, p.givens.count { it != 0 })
                assertEquals(1, Solver.countSolutions(p.givens))
                assertArrayEquals(p.solution, Solver.solve(p.givens))
                for (i in 0 until 81) {
                    if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
                }
                assertValidSolution(p.solution)
            }
        }
    }

    private fun assertValidSolution(s: IntArray) {
        assertNotNull(s)
        val all = (1..9).toSet()
        for (k in 0 until 9) {
            assertEquals(all, (0 until 9).map { s[k * 9 + it] }.toSet())
            assertEquals(all, (0 until 9).map { s[it * 9 + k] }.toSet())
            val br = (k / 3) * 3
            val bc = (k % 3) * 3
            assertEquals(all, (0 until 9).map { s[(br + it / 3) * 9 + bc + it % 3] }.toSet())
        }
    }
}

class AnalyzeTest {
    private val seed = Generator.parse(
        "000000010400000000020000000000050407008000300001090000300400200050100000000806000"
    )

    @Test
    fun uniqueSeventeenCluePuzzleIsAccepted() {
        val a = Solver.analyze(seed)
        assertEquals(Verdict.UNIQUE, a.verdict)
        assertArrayEquals(Solver.solve(seed), a.solution)
    }

    @Test
    fun tooFewClues() {
        val g = seed.copyOf().also { it[it.indexOfFirst { d -> d != 0 }] = 0 }
        assertEquals(Verdict.TOO_FEW_CLUES, Solver.analyze(g).verdict)
    }

    @Test
    fun repeatedDigitIsAConflict() {
        // Row 0 already has a 1 at column 7.
        val g = seed.copyOf().also { it[0] = 1 }
        assertEquals(Verdict.CONFLICT, Solver.analyze(g).verdict)
    }

    @Test
    fun noSolution() {
        // Fill row 0 with 1..8 and put a 9 in column 8 lower down: the last cell of row 0 has no digit.
        val g = IntArray(81)
        for (c in 0 until 8) g[c] = c + 1
        g[4 * 9 + 8] = 9
        // Add unrelated clues to pass the 17-clue minimum without creating conflicts.
        val extra = listOf(9 * 3 + 0 to 4, 9 * 3 + 1 to 5, 9 * 3 + 2 to 6, 9 * 6 + 0 to 7, 9 * 6 + 1 to 8,
            9 * 6 + 2 to 9, 9 * 7 + 3 to 1, 9 * 7 + 4 to 2)
        for ((cell, d) in extra) g[cell] = d
        assertEquals(17, g.count { it != 0 })
        assertEquals(false, Solver.hasConflict(g))
        assertEquals(Verdict.NO_SOLUTION, Solver.analyze(g).verdict)
    }

    @Test
    fun multipleSolutionsAreRejected() {
        val solution = Solver.solve(seed)!!
        // Keep the first 30 cells of the solution: far from unique.
        val g = IntArray(81) { if (it < 30) solution[it] else 0 }
        val a = Solver.analyze(g)
        assertEquals(Verdict.MULTIPLE_SOLUTIONS, a.verdict)
        assertEquals(false, a.playable)
        assertTrue(a.ambiguous.isNotEmpty())
        assertTrue(a.ambiguous.all { g[it] == 0 })
    }

    @Test
    fun ambiguousCellsAreExactlyTheOnesThatVary() {
        // The design from the chat: full centre box, clues in the corners and diagonals.
        val g = Generator.parse(
            "100000002060000070002000300000123000000456000000789000001000900050000080400000003"
        )
        val a = Solver.analyze(g)
        assertEquals(Verdict.MULTIPLE_SOLUTIONS, a.verdict)
        // Brute-force check on a few cells: a cell is ambiguous iff some solution differs there.
        for (cell in a.ambiguous.take(5)) {
            val alt = (1..9).any { d ->
                d != a.solution!![cell] && Solver.solve(g.copyOf().also { it[cell] = d }) != null
            }
            assertTrue(alt)
        }
        for (cell in (0 until 81).filter { g[it] == 0 && it !in a.ambiguous }) {
            val alt = (1..9).any { d ->
                d != a.solution!![cell] && Solver.solve(g.copyOf().also { it[cell] = d }) != null
            }
            assertEquals("cell $cell", false, alt)
        }
    }

    @Test
    fun uniquePuzzleHasNoAmbiguousCells() {
        assertTrue(Solver.analyze(seed).ambiguous.isEmpty())
    }
}
