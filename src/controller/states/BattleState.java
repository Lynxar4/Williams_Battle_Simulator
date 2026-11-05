package controller.states;

import controller.Round;
import entity.Castle;
import entity.Damageable;
import entity.Positioned;
import entity.Team;
import entity.troops.Attacker;
import entity.troops.Healable;
import entity.troops.Troop;
import entity.troops.support.Healer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class BattleState implements GameState {

    @Override
    public void update(Round round) {
        if (!round.checkGameOver()) {
            runSequence(round);
        }
    }

    @Override
    public void render(Round round) {
    }

    private void runSequence(Round round) {
        for (Troop troop : buildTurnOrder(round)) {
            if (troop.isDestroyed()) {
                continue;   // killed earlier in this same sequence, so it skips its turn
            }
            moveTroop(round, troop);
            round.applyTileEffects(troop); // whatever it landed on gets its turn
            if (round.checkGameOver()) {
                return;
            }
            if (troop.isDestroyed()) {
                continue; // a trap can kill it before it gets to act
            }
            actIfInRange(round, troop);
            if (round.checkGameOver()) {
                return;
            }
        }
    }

    private void actIfInRange(Round round, Troop troop) {
        Positioned target = troop.getTarget();

        //Does nothing if there is no target or target is out of range
        if (target == null || !isInRange(round, troop, target)) {
            return;
        }
        if (troop instanceof Attacker attacker) {
            attacker.attack(round.damageBonusAt(attacker)); // high ground counts
        }
        if (troop instanceof Healer healer) {
            healer.heal();
        }
    }

    /*  TURN BUILDING LOGIC*/
    private List<Troop> buildTurnOrder(Round round) {
        //Randomly chooses a team to go first in case of a tie during comparison
        Troop.Team firstTeam = (Math.random() < 0.5) ? Troop.Team.teamA : Troop.Team.teamB;
        return Stream.concat(round.getTeamA().getArmy().stream(),
                        round.getTeamB().getArmy().stream())
                .filter(troop -> !troop.isDestroyed())
                .sorted((a, b) -> compareTurn(a, b, firstTeam))
                .toList();
    }

    private int compareTurn(Troop a, Troop b, Troop.Team firstTeam) {
        int statA = a.getSpeed() + a.getAttackSpeed();
        int statB = b.getSpeed() + b.getAttackSpeed();

        //Different Total: Higher goes first
        if (statA != statB) {
            return statB - statA;
        }

        //Same Total, Same Team: keep as is
        if (a.getTeam() == b.getTeam()) {
            return 0;
        }

        // Same total, different teams: the troop who is in the first team goes first
        return (a.getTeam() == firstTeam) ? -1 : 1;
    }
    /*  ---------   */

    /*  MOVEMENT LOGIC*/
    // A simple helper to represent a coordinate in the grid for pathfinding
    record PathNode(int row, int col) implements Positioned {
        @Override
        public boolean isAt(int row, int column) {
            return this.row == row && this.col == column;
        }

        @Override
        public int getRow() { return row; }

        @Override
        public int getColumn() { return col; }
    }

    // The BFS Pathfinding Method
    // Spreads out from the troop one step at a time until it finds a free cell the troop can attack the target from.
    private List<Positioned> findPath(Round round, Troop troop, Positioned target) {
        Positioned startNode = new PathNode(troop.getRow(), troop.getColumn());

        Queue<Positioned> queue = new LinkedList<>(); // keeps track of the cells we need to explore next
        Map<Positioned, Positioned> parentMap = new HashMap<>(); // remembers which cell each cell was reached from
        Set<Positioned> visited = new HashSet<>(); // holds cells already seen so we never check one twice

        queue.add(startNode);
        visited.add(startNode);

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            Positioned current = queue.poll();

            // Get the path to a cell the troop can hit a target from
            if (distance(current, target) <= troop.getRange()) {
                return buildPath(parentMap, startNode, current);
            }

            for (int[] dir : directions) {
                int nextRow = current.getRow() + dir[0];
                int nextCol = current.getColumn() + dir[1];
                Positioned next = new PathNode(nextRow, nextCol);

                // isBlocked already rejects cells that are out of the map
                if (!visited.contains(next) && !round.isBlocked(nextRow, nextCol)) {
                    visited.add(next);
                    parentMap.put(next, current);
                    queue.add(next);
                }
            }
        }
        return null; // nothing reachable
    }

    // follows the parents back from the goal to the start
    private List<Positioned> buildPath(Map<Positioned, Positioned> parentMap, Positioned start, Positioned end) {
        List<Positioned> path = new ArrayList<>();
        Positioned current = end;
        while (!current.equals(start)) {
            path.add(0, current); // Put the current cell at the front of the list
            current = parentMap.get(current); // Move to the "Parent" (the cell we came from)
        }
        return path;
    }

    private void moveTroop(Round round, Troop troop) {
        Positioned target = findTarget(round, troop);
        if (target == null || isInRange(round, troop, target)) {
            return;
        }

        List<Positioned> path = findPath(round, troop, target);
        if (path == null) {
            return; // no route to the target so the troop stays still
        }

        // walk up to speed steps along the path and stop early once in range
        for (int i = 0; i < Math.min(troop.getSpeed(), path.size()); i++) {
            troop.setPosition(path.get(i).getRow(), path.get(i).getColumn());
            if (isInRange(round, troop, target)) {
                break;
            }
        }
    }

    // high ground lets ranged troops shoot further, so range is read from the
    // board rather than from the troop alone
    private boolean isInRange(Round round, Troop troop, Positioned target) {
        return distance(troop, target) <= troop.getRange() + round.rangeBonusAt(troop);
    }

    // Updates the closest living enemy or enemy castle for attacker or closest living ally for supporters
    private Positioned findTarget(Round round, Troop troop) {
        if (troop instanceof Healer) {
            return findInjuredAlly(round, (Healer) troop);
        }
        return findEnemyTarget(round, (Attacker) troop);
    }

    private Positioned findInjuredAlly(Round round, Healer troop) {
        Healable closestTarget = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Troop ally : round.getOwnTeam(troop).getArmy()) {
            if (ally instanceof Healable healableAlly) { // Creates a healableAlly variable if ally is healable
                int distance = distance(troop, ally);
                if (!ally.isDestroyed() && ally.getGroupHealth() < ally.getMaxHealth() && distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = healableAlly;
                }
            }
        }
        troop.setTarget(closestTarget);
        return closestTarget;
    }

    private Positioned findEnemyTarget(Round round, Attacker troop) {
        Damageable closestTarget = round.getEnemyCastle(troop);
        int closestDistance = distance(troop, closestTarget);
        for (Troop enemy : round.getEnemyTeam(troop).getArmy()) {
            if (!enemy.isDestroyed() && distance(troop, enemy) < closestDistance) {
                closestDistance = distance(troop, enemy);
                closestTarget = enemy;
            }
        }
        troop.setTarget(closestTarget);
        return closestTarget;
    }

    private int distance(Positioned a, Positioned b) {
        if (b instanceof Castle castle) {
            int closestRow = Math.max(castle.getRow(),
                    Math.min(a.getRow(), castle.getRow() + castle.getSize() - 1));

            int closestColumn = Math.max(castle.getColumn(),
                    Math.min(a.getColumn(), castle.getColumn() + castle.getSize() - 1));

            return Math.abs(a.getRow() - closestRow)
                    + Math.abs(a.getColumn() - closestColumn);
        }

        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getColumn() - b.getColumn());
    }

    /*  ----------- */
}