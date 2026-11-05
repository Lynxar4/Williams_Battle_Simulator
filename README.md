# Williams Battle Simulator

A simple Java Swing battle simulator built for our group project. Two teams
face off across a battlefield with gold, troops, and a castle on each side.
Kept intentionally simple — built-in Java (AWT/Swing) only, no external
libraries.

## Running

From the project root in PowerShell:

```powershell
javac -d bin (Get-ChildItem src -Recurse -Filter *.java).FullName
java -cp bin App
```

Run from the project root, not `src/` — sprite paths (`assets/...`) are relative to
the working directory, so running from `src/` leaves them blank.

## Notes

- Battlefield grid: 30x30 tiles. Tiles use the available width and height, with
  a minimum size of 30 pixels in the normal view. Scroll to reach other parts of
  the board in a smaller window, or select Fit whole board for an overview.
  Each team's castle sits midway down its outer edge.
- Each team's `$` budget is displayed on the battlefield.
- Each team starts with $500 and an empty army.
- Choose a team, troop type, and group size in the buying bar. The total cost
  updates as you enter the size. Click an empty tile to buy and place the group.
- Team A places on the left half; Team B places on the right half. Castles,
  tile effects, and other groups occupy tiles and cannot be placed over.
- Prices per soldier: Infantry $10, Archer $15, Cavalry $25, Priest $15.
  One purchase creates one group, represented by one icon. Its badge always
  shows the number of surviving soldiers. Wider tiles also show the type (I/A/C/P).
- Hover over a group to see its type, soldier count, and health.
- Before battle, right-click a group to move it or remove it for a full refund.
  When moving, click an empty tile in the same team's half; press Esc to cancel.
- Figures fit inside their own tile. Sprite scaling ignores transparent margins
  and preserves the image proportions. Team summaries stay visible when scrolling.
- Each troop class uses its own team sprite. Healing highlights the healed group
  and the priest's book. Tile effects report their latest event beneath the board.
- Army health updates in each team panel. Healthy means above 70% of a group's
  starting HP; wounded is 30-70%; critical is below 30%; defeated is zero HP.
  These categories count groups. Overall health is remaining HP / starting HP
  across the army, including defeated groups. An empty army shows a dash.
- Invalid purchases show a message and do not spend gold. Once each team has
  at least one attacking group (Infantry, Archer or Cavalry), click Start Battle.
  Priests cannot attack. Buying, moving and removing groups then close.
- A team loses when its castle is destroyed or every group in its army is dead.
  Battle stops immediately and shows the winner and reason. If both teams are
  defeated when checked, the result is a draw.
- The game-over card compares each team's starting soldiers, survivors, losses
  and remaining castle HP. Counts include priests and exclude groups removed
  before battle. The summary is captured at game over and stays fixed.
- Click Restart on the game-over prompt to return to buying in the same window,
  with fresh castles, empty armies, $500 per team, and a new battlefield.

## Army health processing

The display copies each group's health, maximum health and surviving soldier
count into an immutable `GroupHealthSnapshot` on Swing's event thread. Combat
and movement continue sequentially on that thread.

`ArmyHealthMonitor` uses a `SwingWorker` to collect those snapshots with
`parallelStream()`. `ArmyHealthCollector` counts health categories and sums HP
and soldiers in one pass. Each stream partition gets a separate accumulator;
the combiner adds their counts and totals, and the finisher returns an immutable
`ArmyHealthSummary`. No worker changes troops, Swing components or shared counters.
The collector is unordered, but is not marked concurrent or identity-finish.

Swing applies the results on its event thread. Only one health worker runs at a
time; newer requests replace a waiting snapshot. Older results are ignored after
another refresh or a restart, so they cannot overwrite the latest army.

Parallel streams use Java's common fork/join pool. For these small armies,
sequential aggregation may be faster; this demonstrates safe parallel collection
rather than promising a speed increase. The timing experiment below compares the
same snapshots and verifies equal results. It excludes snapshot creation, worker
scheduling and drawing, so it is not a measure of frame rate or total update time.

## Checks

After compiling the simulation, run these checks using the standard JDK:

```powershell
javac -cp bin -d bin (Get-ChildItem tests -Filter *.java).FullName
java -cp bin TroopBuyingTest
java -cp bin TeamIntegrationTest
java -cp bin GameOverTest
java -cp bin TileEffectVisitorTest
java -cp bin HillRangeTest
java -cp bin SpawnStrategyTest
java -cp bin TileEffectSummaryTest
java -cp bin HillDetourTest
java -cp bin PlacementEditingTest
java -cp bin ArmyHealthTest
java -cp bin ArmyHealthBenchmark
```
