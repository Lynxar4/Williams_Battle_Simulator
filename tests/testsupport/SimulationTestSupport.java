package testsupport;

import controller.*;
import controller.states.BattleState;
import controller.states.PlaceState;
import controller.states.RoundOverState;
import entity.Castle;
import entity.InvalidPlacementException;
import entity.Team;
import entity.interactables.TileEffect;
import entity.troops.Troop;
import entity.troops.TroopType;
import java.util.List;
import view.BattlefieldPanel;

/* Drives the real PlayerActions API and records its GameView output, without a window. */
public final class SimulationTestSupport {
    private SimulationTestSupport() { }

    public static Round newRound(Team teamA, Team teamB) {
        return new Round(teamA, teamB, new RecordingView());
    }

    public static RecordingView view(Round round) {
        return (RecordingView) round.getView();
    }

    public static boolean isPlacingTroops(Round round) {
        return round.getCurrentState() instanceof PlaceState;
    }

    public static boolean isGameOver(Round round) {
        return round.getCurrentState() instanceof RoundOverState;
    }

    public static Troop buyTroop(Round round, Troop.Team owner, TroopType type, int size, int row, int column)
            throws InvalidPlacementException {
        RecordingView view = view(round);
        view.error = false;
        Team team = round.getTeam(owner);
        int before = team.getArmy().size();
        round.tileClicked(owner, type, size, row, column);
        if (view.error || team.getArmy().size() != before + 1) {
            throw new InvalidPlacementException(view.message);
        }
        return team.getArmy().get(before);
    }

    public static void startBattle(Round round) throws InvalidPlacementException {
        RecordingView view = view(round);
        view.error = false;
        round.startBattleRequested();
        if (view.error || !(round.getCurrentState() instanceof BattleState)) {
            throw new InvalidPlacementException(view.message);
        }
    }

    public static void movePlacedTroop(Round round, Troop troop, int row, int column)
            throws InvalidPlacementException {
        RecordingView view = view(round);
        view.error = false;
        view.highlighted = null;
        round.moveTroopRequested(troop);
        if (view.highlighted != troop) throw new InvalidPlacementException("Move was ignored.");
        round.tileClicked(troop.getTeam(), TroopType.INFANTRY, 1, row, column);
        if (view.error) {
            String message = view.message;
            round.cancelMove();
            throw new InvalidPlacementException(message);
        }
        if (!troop.isAt(row, column)) throw new AssertionError("Move did not reach the requested tile");
    }

    public static int removePlacedTroop(Round round, Troop troop) throws InvalidPlacementException {
        RecordingView view = view(round);
        view.error = false;
        Team team = troop == null ? round.getTeamA() : round.getOwnTeam(troop);
        int before = team.getArmy().size();
        int gold = team.getBudget();
        round.removeTroopRequested(troop);
        if (view.error || team.getArmy().size() != before - 1) {
            throw new InvalidPlacementException(view.message);
        }
        return team.getBudget() - gold;
    }

    public static void connectBoard(Round round, BattlefieldPanel board) {
        RecordingView view = view(round);
        view.board = board;
        board.showBoard(round.getRowCount(), round.getColumnCount(), round.getTeamA(), round.getTeamB(),
                round.getTeamACastle(), round.getTeamBCastle(), round.getTileEffects());
        board.setPlacementEnabled(view.placementEnabled);
        board.setPlacementRule(view.rule);
    }

    public static final class RecordingView implements GameView {
        public boolean error, placementEnabled, startAllowed, resultVisible;
        public String message = "", latestEvent = "";
        public Troop highlighted;
        public BattleSummary summary;
        public PlayerActions actions;
        public PlacementRule rule = (team, row, column) -> false;
        private BattlefieldPanel board;

        @Override public void showBoard(int rows, int columns, Team a, Team b, Castle ca, Castle cb,
                                        List<TileEffect> effects) {
            if (board != null) board.showBoard(rows, columns, a, b, ca, cb, effects);
        }
        @Override public void refreshBoard() { if (board != null) board.refresh(); }
        @Override public void setPlacementEnabled(boolean enabled) {
            placementEnabled = enabled;
            if (board != null) board.setPlacementEnabled(enabled);
        }
        @Override public void showGold(int a, int b) { }
        @Override public void setStartBattleAllowed(boolean allowed) { startAllowed = allowed; }
        @Override public void setPlacementRule(PlacementRule rule) {
            this.rule = rule;
            if (board != null) board.setPlacementRule(rule);
        }
        @Override public void highlightMovingTroop(Troop troop) {
            highlighted = troop;
            if (board != null) board.setMovingTroop(troop);
        }
        @Override public void showMessage(String text, boolean isError) { message = text; error = isError; }
        @Override public void showLatestEvent(String text) {
            latestEvent = text;
            if (board != null) board.showLatestEvent(text);
        }
        @Override public void showRoundResult(String title, String reason, BattleSummary summary) {
            resultVisible = true;
            this.summary = summary;
        }
        @Override public void hideRoundResult() { resultVisible = false; }
        @Override public void setPlayerActions(PlayerActions actions) { this.actions = actions; }
    }
}
