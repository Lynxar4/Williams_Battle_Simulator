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
- Current working branch: `add-gitignore`

## Running

```
cd src
javac App.java BattlefieldPanel.java
java App
```

## Progress log

- **2026-08-28** — Added `.gitignore` for a Java project (compiled output,
  packaged archives, logs, IDE files).
- **2026-08-28** — Added the basic Swing window scaffold: `App.java` opens
  a `JFrame` and adds an empty `BattlefieldPanel` (a blank `JPanel`) as the
  starting point for the game window. No simulation logic yet — just the
  window itself.
