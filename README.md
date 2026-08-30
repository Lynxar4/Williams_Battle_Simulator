# Williams Battle Simulator

A simple Java Swing battle simulator built for our group project. Two teams
face off across a battlefield with gold, troops, and a castle on each side.
Kept intentionally simple — built-in Java (AWT/Swing) only, no external
libraries.

## Repo setup

- Original/shared repo: `xenosM/Williams_Battle_Simulator` (`upstream`)
- Rafic forked it to `rafic-najjar/Williams_Battle_Simulator` (`origin`) to
  develop on and push changes without needing direct write access to the
  shared repo. Work gets merged back via pull request from the fork.
- Rafic's working branch: `jpanel-scaffold` (kept in sync with `main`)

## Running

```
cd src
javac App.java controller/*.java controller/states/*.java entity/*.java view/*.java
java App
```

## Known issues

- **Build currently broken** (as of `147c3b6`, "fixed minor bugs"): `Round.update()`
  calls `currentState.update()`, but `GameState`'s `update()`/`render()` methods
  are commented out in the interface. Needs to be uncommented (and implemented
  by the state classes) before the project compiles.

## Progress log

- **2026-08-28** — Added `.gitignore` for a Java project (compiled output,
  packaged archives, logs, IDE files).
- **2026-08-28** — Added the basic Swing window scaffold: `App.java` opens
  a `JFrame` and adds an empty `BattlefieldPanel` (a blank `JPanel`) as the
  starting point for the game window. No simulation logic yet — just the
  window itself.
- **2026-08-30** — Pulled in teammates' work from `upstream/main`: `Castle`
  and `Troop` entity classes, and `BattlefieldPanel` now draws a grid with
  both teams' castles and a placeholder troop.
- **2026-08-30** — Renamed Rafic's working branch from `add-gitignore` to
  `jpanel-scaffold` (more descriptive of what it actually contains now).
- **2026-08-30** — Pulled another round of teammates' work from
  `upstream/main`: code reorganized into `entity/`, `view/`, and
  `controller/` packages; added `Team`, and a `controller/` package with
  `Round`, `Session`, and a `GameState` state machine (`AllocateState`,
  `PlaceState`, `BattleState`, `RoundOverState`) for round/session logic.
  See Known issues — this pull currently doesn't compile.
