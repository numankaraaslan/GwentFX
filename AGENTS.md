# AGENTS.md

Instructions for AI coding agents working on this repository.

GwentFX is a fan remake of the Gwent card game from The Witcher 3: one player against the computer, written in
Java with JavaFX. [README.md](README.md) explains what the game is and how to run it, [RULES.md](RULES.md) how the
original rules were interpreted, and [AI.md](AI.md) how the computer opponent decides.

## Build and run

- `mvn javafx:run` starts the game.
- `mvn -DskipTests package` builds the runnable jar `target/GwentFX.jar`.
- There are no automated tests.
- A game started with `mvn javafx:run` keeps loading classes from `target/classes` while it runs, so rebuilding into
  `target/` (above all `mvn clean`) while it's open crashes it. For compile checks, build into another folder:
  `mvn -q -DskipTests -Dgwent.buildDir=<another folder> compile`.

## Java conventions

- No `var`: always write the type.
- No method references (`::`) and no lambdas: use anonymous classes. A single-line stream operation is the only
  exception.
- No fully qualified class names: use imports.
- One statement per line: don't break a statement across lines.
- No inline styles (`setStyle`): styling goes in the CSS files under
  `src/main/resources/com/numankaraaslan/gwent/css/`.
- Match the formatting of the file you edit.

## Rules not to break

- The `engine`, `model` and `ai` packages never use JavaFX.
- Only the engine changes `GameState`: `GameEngine` runs the game flow (dealing, redraws, rounds) and `Resolver`
  applies moves, abilities and round ends. `Rules` only reads the state.
- The engine runs on its own thread. The UI only draws the `GameSnapshot`s that come with each `GameEvent`, and
  never changes `GameState`.
- The computer opponent only uses what a human player could know. It simulates moves on copies of the game with
  both decks shuffled, and it never reads the opponent's hand. The one exception is cards that Emperor of
  Nilfgaard reveals.
- Every screen is laid out on a fixed 1920×1080 canvas that is scaled to the window.

## Content

- Never add artwork, music, sound, fonts or code from the original game. Cards are drawn with JavaFX shapes.
- Keep the disclaimer in the README.

## Docs

- When a rule changes, update RULES.md.
- When the computer opponent's behaviour or numbers change, update AI.md.
