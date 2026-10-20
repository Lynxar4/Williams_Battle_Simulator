package controller.states;

import controller.Round;
import entity.Positioned;
import entity.Team;
import entity.troops.Troop;

public class BattleState implements GameState {

    @Override
    public void update(Round round) {
        moveArmy(round, round.getTeamA());
        moveArmy(round, round.getTeamB());
    }

    @Override
    public void render(Round round) {
    }

    private void moveArmy(Round round, Team team) {
        for (Troop troop : team.getArmy())
        {
            if (!troop.isDestroyed())
            {
                moveTroop(round, troop);
            }
        }
    }

    // Moves the troop 1 cell per step instead of one big jump. Steps match the troop's speed member.
    private void moveTroop(Round round, Troop troop)
    {
        for (int step = 0; step < troop.getSpeed(); ++step) {
            Positioned target = findTarget(round, troop);
            if (!stepToward(round, troop, target))
            {
                return;
            }
        }
    }

    // Updates the closest living enemy or the enemy castle
    private Positioned findTarget(Round round, Troop troop) {
        Positioned closestTarget = round.getEnemyCastle(troop);
        int closestDistance = distance(troop, closestTarget);
        for (Troop enemy : round.getEnemyTeam(troop).getArmy()) {
            if (!enemy.isDestroyed() && distance(troop, enemy) < closestDistance) {
                closestDistance = distance(troop, enemy);
                closestTarget = enemy;
            }
        }
        return closestTarget;
    }

    private boolean stepToward(Round round, Troop troop, Positioned target) {
        int rowDiff = target.getRow() - troop.getRow();
        int colDiff = target.getColumn() - troop.getColumn();
        if (Math.abs(rowDiff) + Math.abs(colDiff) <= 1)
        {
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
}