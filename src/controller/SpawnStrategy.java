package controller;

import entity.troops.Troop;
import java.util.Random;

// decides where a tile effect is allowed to appear.
// spawning used to be three near identical loops that each picked a cell the
// same way. the loop is now one method and the choice of cell is passed in,
// so a new rule is a lambda rather than another copy of the loop.
@FunctionalInterface
public interface SpawnStrategy {

    // suggests one cell. the caller still checks whether it is free, so this
    // is allowed to suggest a taken cell and be asked again.
    Cell pick(Random random, int rowCount, int columnCount);

    /* ---- READY MADE STRATEGIES ---- */

    // anywhere on the board
    static SpawnStrategy anywhere() {
        return (random, rows, columns) -> new Cell(random.nextInt(rows), random.nextInt(columns));
    }

    // only inside one team's half, which is the same rule placement will use
    // when the player gets to put traps down themselves
    static SpawnStrategy ownHalf(Troop.Team team) {
        return (random, rows, columns) -> {
            int half = columns / 2;
            int column = (team == Troop.Team.teamA)
                    ? random.nextInt(half)
                    : half + random.nextInt(columns - half);
            return new Cell(random.nextInt(rows), column);
        };
    }

    /* ---- COMBINING ---- */

    // narrows an existing strategy by keeping its picks away from the left and
    // right edges, where the castles sit. wraps whatever it is called on
    // instead of replacing it, so ownHalf(teamA).awayFromEdges(2) still only
    // picks on team A's side.
    default SpawnStrategy awayFromEdges(int margin) {
        return (random, rows, columns) -> {
            Cell cell = pick(random, rows, columns);
            int clamped = Math.min(Math.max(cell.column(), margin), columns - 1 - margin);
            return new Cell(cell.row(), clamped);
        };
    }
}
