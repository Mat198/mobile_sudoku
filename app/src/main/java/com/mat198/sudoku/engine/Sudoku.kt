package com.mat198.sudoku.engine

import kotlin.random.Random

/** Number of given digits for each difficulty. Challenger uses the theoretical minimum (17). */
enum class Difficulty(val label: String, val clues: Int) {
    EASY("Easy", 40),
    MEDIUM("Medium", 34),
    HARD("Hard", 28),
    EXPERT("Expert", 23),
    CHALLENGER("Challenger", 17),
}

/** A puzzle with exactly one solution. Cells are indexed row * 9 + col; 0 means empty. */
class Puzzle(val givens: IntArray, val solution: IntArray)

object Solver {
    /** Counts solutions of [grid], stopping once [limit] is reached. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val state = State.from(grid) ?: return 0
        var count = 0
        fun search(): Boolean {
            val cell = state.bestEmptyCell()
            if (cell < 0) {
                count++
                return count >= limit
            }
            var candidates = state.candidates(cell)
            while (candidates != 0) {
                val bit = candidates and -candidates
                candidates = candidates xor bit
                state.place(cell, Integer.numberOfTrailingZeros(bit))
                if (search()) return true
                state.clear(cell)
            }
            return false
        }
        search()
        return count
    }

    /** Returns the first solution found, or null if the grid is unsolvable. */
    fun solve(grid: IntArray): IntArray? {
        val state = State.from(grid) ?: return null
        fun search(): Boolean {
            val cell = state.bestEmptyCell()
            if (cell < 0) return true
            var candidates = state.candidates(cell)
            while (candidates != 0) {
                val bit = candidates and -candidates
                candidates = candidates xor bit
                state.place(cell, Integer.numberOfTrailingZeros(bit))
                if (search()) return true
                state.clear(cell)
            }
            return false
        }
        return if (search()) state.cells.copyOf() else null
    }

    private class State(val cells: IntArray) {
        // Bit d (1..9) set means digit d is already used in that unit.
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)

        fun candidates(cell: Int): Int {
            val r = cell / 9
            val c = cell % 9
            return (rows[r] or cols[c] or boxes[box(r, c)]).inv() and ALL
        }

        fun place(cell: Int, d: Int) {
            val r = cell / 9
            val c = cell % 9
            val bit = 1 shl d
            cells[cell] = d
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[box(r, c)] = boxes[box(r, c)] or bit
        }

        fun clear(cell: Int) {
            val r = cell / 9
            val c = cell % 9
            val mask = (1 shl cells[cell]).inv()
            cells[cell] = 0
            rows[r] = rows[r] and mask
            cols[c] = cols[c] and mask
            boxes[box(r, c)] = boxes[box(r, c)] and mask
        }

        /** Empty cell with the fewest candidates, or -1 when the grid is full. */
        fun bestEmptyCell(): Int {
            var best = -1
            var bestCount = 10
            for (i in 0 until 81) {
                if (cells[i] != 0) continue
                val n = Integer.bitCount(candidates(i))
                if (n < bestCount) {
                    best = i
                    bestCount = n
                    if (n <= 1) break
                }
            }
            return best
        }

        companion object {
            const val ALL = 0x3FE // bits 1..9

            fun box(r: Int, c: Int) = (r / 3) * 3 + c / 3

            fun from(grid: IntArray): State? {
                val s = State(IntArray(81))
                for (i in 0 until 81) {
                    val d = grid[i]
                    if (d == 0) continue
                    if (s.candidates(i) and (1 shl d) == 0) return null
                    s.place(i, d)
                }
                return s
            }
        }
    }
}

object Generator {
    /**
     * Builds a puzzle from one of the known 17-clue puzzles: the seed is disguised with a
     * random validity-preserving transformation (digit relabelling, row/column swaps within
     * bands/stacks, band/stack swaps, transposition) and then, for easier levels, extra cells
     * of the solution are revealed. Adding clues never breaks uniqueness.
     */
    fun generate(difficulty: Difficulty, seeds: List<String>, random: Random = Random.Default): Puzzle {
        val seed = parse(seeds[random.nextInt(seeds.size)])
        val givens = transform(seed, random)
        val solution = requireNotNull(Solver.solve(givens)) { "Seed puzzle has no solution" }

        val empty = (0 until 81).filter { givens[it] == 0 }.shuffled(random)
        val toReveal = (difficulty.clues - givens.count { it != 0 }).coerceAtLeast(0)
        for (cell in empty.take(toReveal)) givens[cell] = solution[cell]

        return Puzzle(givens, solution)
    }

    /** Parses an 81-character puzzle line; '.' or '0' mean empty. */
    fun parse(line: String): IntArray {
        val s = line.trim()
        require(s.length >= 81) { "Puzzle line too short" }
        return IntArray(81) { i ->
            val ch = s[i]
            if (ch in '1'..'9') ch - '0' else 0
        }
    }

    fun transform(grid: IntArray, random: Random): IntArray {
        val digits = (1..9).shuffled(random)
        val rowOrder = lineOrder(random)
        val colOrder = lineOrder(random)
        val transpose = random.nextBoolean()
        return IntArray(81) { i ->
            var r = rowOrder[i / 9]
            var c = colOrder[i % 9]
            if (transpose) r = c.also { c = r }
            val d = grid[r * 9 + c]
            if (d == 0) 0 else digits[d - 1]
        }
    }

    /** A permutation of 0..8 that keeps lines within groups of three (bands or stacks). */
    private fun lineOrder(random: Random): List<Int> =
        (0 until 3).shuffled(random).flatMap { group ->
            (0 until 3).shuffled(random).map { group * 3 + it }
        }
}
