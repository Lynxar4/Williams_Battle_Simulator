package controller.states;

import controller.Round;
import entity.InvalidPlacementException;
import entity.Team;
import entity.troops.Attacker;
import entity.troops.Troop;
import entity.troops.TroopType;
import view.GameView;

import java.util.HashMap;
import java.util.Map;

public class PlaceState implements GameState {
    /* ---- PRIVATE VARIABLES ---- */
    // Amount actually paid per group, so removing it refunds exactly once.
    private final Map<Troop,Integer> purchaseCosts = new HashMap<>();
    private Troop movingTroop;

    @Override
    public void onEnter(Round round){
        GameView view = round.getView();
        view.setPlacementEnabled(true);
        view.setPlacementRule((team, row, column) -> isValidTile(round, team, row, column));
        view.showMessage("Click an empty tile to buy a group. Right-click a group to move or remove it.", false);
        updateControls(round);

    }

    @Override
    public void render(Round round) {
        round.getView().refreshBoard();
    }

    @Override
    public void onTileClicked(Round round, Troop.Team team, TroopType type, int groupSize, int row, int column) {
        GameView view = round.getView();
        try {
            if (movingTroop != null) {
                moveTroop(round, row, column);
                view.showMessage("Group moved. Right-click a group to move or remove it.", false);
            } else {
                int cost = buyTroop(round, team, type, groupSize, row, column);
                view.showMessage("Bought " + type.getDisplayName() + " group of " + groupSize
                        + " for $" + cost + ". Right-click to move or remove it.", false);
            }
        } catch (InvalidPlacementException e) {
            view.showMessage(e.getMessage(), true);
        }
        updateControls(round);
    }

    @Override
    public void onMoveTroopRequested(Round round, Troop troop) {
        movingTroop = troop;
        round.getView().highlightMovingTroop(troop);
        round.getView().showMessage("Click an empty tile in this team's half to move the group. Esc cancels.", false);
    }

    @Override
    public void onCancelMove(Round round) {
        if (movingTroop != null) {
            clearMove(round);
            round.getView().showMessage("Move cancelled. Click an empty tile to buy, or right-click a group to edit.", false);
        }
    }

    @Override
    public void onRemoveTroopRequested(Round round, Troop troop) {
        try {
            validatePlacedTroop(round, troop);
            int refund = purchaseCosts.remove(troop);
            Team team = round.getOwnTeam(troop);
            team.getArmy().remove(troop);
            team.addBudget(refund);
            clearMove(round);
            round.getView().showMessage("Group removed. Refunded $" + refund + ".", false);
        } catch (InvalidPlacementException e) {
            round.getView().showMessage(e.getMessage(), true);
        }
        updateControls(round);
    }

    @Override
    public void onStartBattleRequested(Round round) {
        clearMove(round);
        if (!canStartBattle(round)) {
            round.getView().showMessage(
                    "Priests cannot attack. Each team needs an Infantry, Archer or Cavalry group to start.", true);
            return;
        }
        round.setCurrentState(new BattleState());
    }

    /* ---- PLACEMENT RULES ---- */
    private int buyTroop(Round round, Troop.Team owner, TroopType type, int groupSize, int row, int column)
            throws InvalidPlacementException {
        if (owner == null || type == null) {
            throw new InvalidPlacementException("Select a team and troop type first.");
        }
        validatePlacement(round, owner, row, column);

        int cost;
        try {
            cost = type.getTotalCost(groupSize);
        } catch (IllegalArgumentException e) {
            throw new InvalidPlacementException(e.getMessage());
        }
        Team team = round.getTeam(owner);
        if (cost > team.getBudget()) {
            throw new InvalidPlacementException("Not enough gold: this group costs $" + cost
                    + ", but the team has $" + team.getBudget() + ".");
        }

        Troop troop = type.createTroop(row, column, groupSize, owner);
        team.spendBudget(cost);
        team.addTroop(troop);
        purchaseCosts.put(troop, cost);
        return cost;
    }

    private void moveTroop(Round round, int row, int column) throws InvalidPlacementException {
        validatePlacedTroop(round, movingTroop);
        if (!movingTroop.isAt(row, column)) {
            validatePlacement(round, movingTroop.getTeam(), row, column);
            movingTroop.setPosition(row, column);
        }
        clearMove(round); // only reached if the move worked
    }

    private void validatePlacedTroop(Round round, Troop troop) throws InvalidPlacementException {
        if (troop == null || troop.isDestroyed() || !purchaseCosts.containsKey(troop)
                || !round.getOwnTeam(troop).getArmy().contains(troop)) {
            throw new InvalidPlacementException("Choose a placed group from this battle.");
        }
    }

    private void validatePlacement(Round round, Troop.Team owner, int row, int column)
            throws InvalidPlacementException {
        if (!round.isWithinBoard(row, column)) {
            throw new InvalidPlacementException("Choose a tile inside the battlefield.");
        }
        if (round.ownerOfHalf(column) != owner) {
            throw new InvalidPlacementException("Place Team A on the left half and Team B on the right half.");
        }
        if (!round.isCellFree(row, column)) {
            throw new InvalidPlacementException("That tile is occupied. Choose an empty tile.");
        }
    }

    // Same rules as a real placement, just answered with true/false for the hover colour.
    private boolean isValidTile(Round round, Troop.Team team, int row, int column) {
        if (movingTroop != null && movingTroop.isAt(row, column)) {
            return true; // dropping a group back where it was is fine
        }
        try {
            validatePlacement(round, team, row, column);
            return true;
        } catch (InvalidPlacementException e) {
            return false;
        }
    }

    private boolean canStartBattle(Round round) {
        return hasLivingAttacker(round.getTeamA()) && hasLivingAttacker(round.getTeamB());
    }

    private boolean hasLivingAttacker(Team team) {
        return team.getArmy().stream()
                .anyMatch(troop -> troop instanceof Attacker && !troop.isDestroyed());
    }

    /* ---- HELPERS ---- */
    private void clearMove(Round round) {
        movingTroop = null;
        round.getView().highlightMovingTroop(null);
    }

    private void updateControls(Round round) {
        GameView view = round.getView();
        view.showGold(round.getTeamA().getBudget(), round.getTeamB().getBudget());
        view.setStartBattleAllowed(canStartBattle(round));
    }

}