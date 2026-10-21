# Williams Battle Simulator

A simple Java Swing battle simulator built for our group project. Two teams
face off across a battlefield with gold, troops, and a castle on each side.
Kept intentionally simple — built-in Java (AWT/Swing) only, no external
libraries.

## Running

```
javac -d bin src/App.java src/controller/*.java src/controller/states/*.java src/entity/*.java src/view/*.java
java -cp bin App
```

Run from the project root, not `src/` — sprite paths (`assets/...`) are relative to
the working directory, so running from `src/` leaves them blank.

## Notes

- Battlefield grid: 10x16 tiles.
- Each team's `$` budget is displayed on the battlefield.
- Click-to-place: clicking a tile moves a troop there, with a hover
  highlight on the tile under the cursor before you click.
