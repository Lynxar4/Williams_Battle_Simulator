package controller.states;

import controller.Round;
import entity.Damageable;
import entity.Positioned;
import entity.Team;
import entity.troops.Attacker;
import entity.troops.Healable;
import entity.troops.Troop;
import entity.troops.support.Healer;
import controller.GameView;

import java.util.List;
import java.util.stream.Stream;

public class BattleState implements GameState {
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

    /*
        Moves the troop 1 cell per step instead of one big jump. Steps match the troop's speed member.
        If there is no target or target is in range the troop doesn't move
    */
    private void moveTroop(Round round, Troop troop) {
        for (int step = 0; step < troop.getSpeed(); ++step) {
            Positioned target = findTarget(round, troop);
            if (target == null || isInRange(round, troop, target)) {
                return;
            }
            if (!stepToward(round, troop, target)) {
                return;
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

    private boolean stepToward(Round round, Troop troop, Positioned target) {
        int rowDiff = target.getRow() - troop.getRow();
        int colDiff = target.getColumn() - troop.getColumn();
        if (Math.abs(rowDiff) + Math.abs(colDiff) <= 1) {
            return false; // This means the troops are adjacent, Attack logic will be added here later
        }

        int colStep = Integer.signum(colDiff); // signum returns the sign of a number. 1 for positive, 0 for 0 and -1 for negative. We use this because we only need the troop to move 1 cell.
        int rowStep = Integer.signum(rowDiff);

        // try moving sideways
        if (colStep != 0 && tryMove(round, troop, troop.getRow(), troop.getColumn() + colStep)) {
            return true;
        }

        // if sideways didn't work try moving up and down
        if (rowStep != 0 && tryMove(round, troop, troop.getRow() + rowStep, troop.getColumn())) {
            return true;
        }
        return false;
    }

    private boolean tryMove(Round round, Troop troop, int row, int column) {
        if (round.isBlocked(row, column)) {
            return false;
        }
        troop.setPosition(row, column);
        return true;
    }

    private int distance(Positioned a, Positioned b) {
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

        if (defeatedA && defeatedB) {
            round.setCurrentState(new RoundOverState(RoundOverState.Outcome.DRAW,
                    "Both teams have lost their castle or their entire army."));
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

        RoundOverState.Outcome outcome = defeatedA
                ? RoundOverState.Outcome.TEAM_B_WINS
                : RoundOverState.Outcome.TEAM_A_WINS;
        round.setCurrentState(new RoundOverState(outcome, reason));
        return true;
    }

    private boolean isArmyDefeated(Team team) {
        return team.getArmy().stream().allMatch(Troop::isDestroyed);
    }

    /*  ----------- */
}
