package controller.states;

import controller.BattleSummary;
import controller.GameView;
import controller.Round;
import entity.Castle;
import entity.Damageable;
import entity.Positioned;
import entity.Team;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.troops.Attacker;
import entity.troops.Healable;
import entity.troops.Troop;
import entity.troops.support.Healer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BattleState implements GameState {
    // a hill nobody has claimed yet. a claimed one is no use: the second
    // team to arrive gets nothing.
    private static final Predicate<TileEffect> UNCLAIMED_HILL = effect -> effect instanceof Hill hill && !hill.isClaimed();
    // how far out of its way a troop will go for one
    private static final int MAX_HILL_DETOUR = 5;

    @Override
    public void onEnter(Round round) {
        GameView view = round.getView();
        view.setPlacementEnabled(false);
        view.showMessage("Battle in progress. Buying and placement are closed.", false);
    }

    @Override
    public void update(Round round) {
        if (endRoundIfOver(round)) {
            return; // someone already lost before anyone moved
        }
        runSequence(round);
    }

    @Override
    public void render(Round round) {
        round.getView().refreshBoard();
    }

    private void runSequence(Round round) {
        for (Troop troop : buildTurnOrder(round)) {
            if (troop.isDestroyed()) {
                continue;   // killed earlier in this same sequence, so it skips its turn
            }
            moveTroop(round, troop);
            round.applyTileEffects(troop); // whatever it landed on gets its turn
            if (endRoundIfOver(round)) {
                return;
            }
            if (troop.isDestroyed()) {
                continue; // a trap can kill it before it gets to act
            }
            actIfInRange(round, troop);
            if (endRoundIfOver(round)) {
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
    /*  ---------   */

    /*  TURN BUILDING LOGIC*/
    private List<Troop> buildTurnOrder(Round round) {
        //Randomly chooses a team to go first in case of a tie during comparison
        Troop.Team firstTeam = (Math.random() < 0.5) ? Troop.Team.teamA : Troop.Team.teamB;
        return Stream.concat(round.getTeamA().getArmy().stream(), round.getTeamB().getArmy().stream()).filter(troop -> !troop.isDestroyed()).sorted((a, b) -> compareTurn(a, b, firstTeam)).toList();
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

    // The BFS Pathfinding Method
    // Spreads out from the troop one step at a time until it finds a free cell where the target is in range
    private List<Positioned> findPath(Round round, Troop troop, Positioned target, int range) {
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
            if (distance(current, target) <= range) {
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

    // Follows the parents back from the goal to the start
    private List<Positioned> buildPath(Map<Positioned, Positioned> parentMap, Positioned start, Positioned end) {
        List<Positioned> path = new ArrayList<>();
        Positioned current = end;
        while (!current.equals(start)) {
            path.add(0, current); // put the current cell at the front of the list
            current = parentMap.get(current); // move to the "Parent" (the cell we came from)
        }
        return path;
    }

    // Takes the first step of the BFS path. Returns false if there is no route
    private boolean stepAlongPath(Round round, Troop troop, Positioned target, int range) {
        List<Positioned> path = findPath(round, troop, target, range);
        if (path == null || path.isEmpty()) {
            return false;
        }
        troop.setPosition(path.get(0).getRow(), path.get(0).getColumn());
        return true;
    }

    /* HIGH GROUND */

    // Take up to speed steps per tick. Each step re-picks the target and moves one cell
    private void moveTroop(Round round, Troop troop) {
        for (int step = 0; step < troop.getSpeed(); ++step) {
            Positioned target = findTarget(round, troop);
            if (target == null || isInRange(round, troop, target)) {
                return; // stop moving if the target is in range or there's no target
            }

            // a free hill close by is worth stepping aside for, because
            // holding it pays for the rest of the fight. if the detour is
            // blocked the troop carries on at its target as before.
            Optional<TileEffect> hill = hillWorthTaking(round, troop, target);
            boolean moved = hill.map(free -> stepAlongPath(round, troop, free, 0)).orElse(false) || stepAlongPath(round, troop, target, troop.getRange());

            if (!moved) {
                return;
            }
        }
    }

    private Optional<TileEffect> hillWorthTaking(Round round, Troop troop, Positioned target) {
        // priests gain nothing from height, and a troop already holding a
        // hill should stay on it
        if (!(troop instanceof Attacker) || round.damageBonusAt(troop) > 0) {
            return Optional.empty();
        }

        int toTarget = distance(troop, target);

        return round.effectsMatching(UNCLAIMED_HILL).stream().filter(hill -> distance(troop, hill) <= MAX_HILL_DETOUR).filter(hill -> distance(troop, hill) < toTarget).min(Comparator.comparingInt(hill -> distance(troop, hill)));
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
            int closestRow = Math.max(castle.getRow(), Math.min(a.getRow(), castle.getRow() + castle.getSize() - 1));

            int closestColumn = Math.max(castle.getColumn(), Math.min(a.getColumn(), castle.getColumn() + castle.getSize() - 1));

            return Math.abs(a.getRow() - closestRow) + Math.abs(a.getColumn() - closestColumn);
        }

        return Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getColumn() - b.getColumn());
    }

    private boolean endRoundIfOver(Round round) {
        boolean castleA = round.getTeamACastle().isDestroyed();
        boolean castleB = round.getTeamBCastle().isDestroyed();
        boolean armyA = isArmyDefeated(round.getTeamA());
        boolean armyB = isArmyDefeated(round.getTeamB());
        boolean defeatedA = castleA || armyA;
        boolean defeatedB = castleB || armyB;

        if (!defeatedA && !defeatedB) {
            return false;
        }
        BattleSummary summary = BattleSummary.capture(round);
        if (defeatedA && defeatedB) {
            round.setCurrentState(new RoundOverState(RoundOverState.Outcome.DRAW, "Both teams have lost their castle or their entire army.",summary));
            return true;
        }

        String team = defeatedA ? "Team A" : "Team B";
        boolean castle = defeatedA ? castleA : castleB;
        boolean army = defeatedA ? armyA : armyB;
        String reason;
        if (castle && army) {
            reason = team + " lost its castle and its entire army.";
        } else if (castle) {
            reason = team + "'s castle was destroyed.";
        } else {
            reason = team + "'s entire army was defeated.";
        }

        RoundOverState.Outcome outcome = defeatedA ? RoundOverState.Outcome.TEAM_B_WINS : RoundOverState.Outcome.TEAM_A_WINS;
        round.setCurrentState(new RoundOverState(outcome, reason,summary));
        return true;
    }

    private boolean isArmyDefeated(Team team) {
        return team.getArmy().stream().allMatch(Troop::isDestroyed);
    }

    /*  MOVEMENT LOGIC*/
    // A simple helper to represent a coordinate in the grid for pathfinding
    record PathNode(int row, int col) implements Positioned {
        @Override
        public boolean isAt(int row, int column) {
            return this.row == row && this.col == column;
        }

        @Override
        public int getRow() {
            return row;
        }

        @Override
        public int getColumn() {
            return col;
        }
    }

    /*  ----------- */
}