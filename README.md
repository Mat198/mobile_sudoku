# mobile_sudoku

Sudoku para jogar no celular sem propagandas chatas e sem ajuda desnecessária que deixa o jogo mais fácil.

A hardcore, ad-free, offline Sudoku for Android: pen and paper only, no hints.

## Features

- **Start menu** with *Continue* (resumes the saved game, including the clock and undo history) and *New game*.
- **Five difficulty levels**, defined by the number of given digits:

  | Level      | Clues |
  |------------|------:|
  | Easy       | 40 |
  | Medium     | 34 |
  | Hard       | 28 |
  | Expert     | 23 |
  | Challenger | 17 (the minimum possible) |

  Every puzzle starts from one of 2000 known 17-clue puzzles (Gordon Royle's collection),
  disguised by a random symmetry transform (digit relabelling, row/column/band/stack shuffles,
  transposition). Easier levels reveal extra cells from the solution. Every puzzle has exactly
  one solution.
- **Paper & eraser**: write numbers (pen) or notes (pencil). Nothing is checked as you play,
  no conflict highlighting, no same-digit highlighting, no digit counters, no auto-notes.
- **Check solution** appears only when all 81 cells are filled. It only tells you
  "solved" or "something is wrong somewhere" — never where.
- **Clock**, **Undo** (reverse), **Erase**, and a **Pen / Notes switch**.
- Portrait layout tuned for the Redmi Note 13 (1080×2400). Screen stays on during a game.

## Install on the phone

Every push builds the APK with GitHub Actions:

1. On the phone, open the repo's **Releases** page and download `sudoku.apk` from the latest
   `latest-…` release.
2. Open it and allow "Install unknown apps" for your browser/file manager when asked
   (on MIUI/HyperOS you may also need to confirm in the security prompt).

The APK is always signed with the same key (`app/debug.keystore`), so new builds install over
the old one and keep your saved game.

## Build locally

Requires the Android SDK (API 35) and JDK 17:

```
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest   # puzzle engine tests
```

## Code

- `engine/Sudoku.kt` — bitmask solver, solution counter and puzzle generator.
- `game/Game.kt` — the board state, undo history and save format.
- `game/GameViewModel.kt` — screens, clock, persistence.
- `ui/` — Jetpack Compose screens and the board canvas.
