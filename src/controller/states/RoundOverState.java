package controller.states;

import controller.Round;
import controller.GameView;
import controller.BattleSummary;

public class RoundOverState implements GameState {
    /* ---- ENUMS ---- */
    public enum Outcome {
        TEAM_A_WINS, TEAM_B_WINS, DRAW
    }

    /* ---- PRIVATE VARIABLES ---- */
    private final Outcome outcome;
    private final String reason;
    private final BattleSummary summary;

    /* ---- CONSTRUCTORS ---- */
    public RoundOverState(Outcome outcome, String reason, BattleSummary summary) {
        this.outcome = outcome;
        this.reason = reason;
        this.summary = summary;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void onEnter(Round round) {
        GameView view = round.getView();
        view.refreshBoard();                     // draw the final position once
        view.showRoundResult(getTitle(), reason, summary);
    }

    @Override
    public void onNextRoundRequested(Round round) {
        round.getView().hideRoundResult();   // the state that showed it, hides it
        round.getTeamA().clearArmy();
        round.getTeamB().clearArmy();
        round.setCurrentState(new AllocateState());
    }

    /* ---- GETTER METHODS ---- */
    public Outcome getOutcome() {
        return outcome;
    }

    public String getTitle() {
        return switch (outcome) {
            case TEAM_A_WINS -> "Team A wins!";
            case TEAM_B_WINS -> "Team B wins!";
            case DRAW -> "Draw";
        };
    }

    public String getReason() {
        return reason;
    }

    public BattleSummary getSummary() {
        return summary;
    }
}
