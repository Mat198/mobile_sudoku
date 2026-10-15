package com.mat198.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
