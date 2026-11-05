package controller.states;

import controller.Round;
import controller.BattleSummary;

public class RoundOverState implements GameState {
    public enum Outcome {
        TEAM_A_WINS, TEAM_B_WINS, DRAW
    }

    private final Outcome outcome;
    private final String reason;
    private final BattleSummary summary;

    public RoundOverState(Outcome outcome, String reason, BattleSummary summary) {
        this.outcome = outcome;
        this.reason = reason;
        this.summary = summary;
    }

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

    @Override
    public void update(Round round) {
        // The finished round stays frozen until the session restarts.
    }

    @Override
    public void render(Round round) {
        // Session displays the result without adding Swing dependencies to this state.
    }

}
