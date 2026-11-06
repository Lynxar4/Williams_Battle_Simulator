package controller.states;

import controller.Round;
import controller.GameView;

public class RoundOverState implements GameState {
    public enum Outcome {
        TEAM_A_WINS, TEAM_B_WINS, DRAW
    }

    private final Outcome outcome;
    private final String reason;

    public RoundOverState(Outcome outcome, String reason) {
        this.outcome = outcome;
        this.reason = reason;
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

    @Override
    public void onEnter(Round round) {
        GameView view = round.getView();
        view.refreshBoard();                     // draw the final position once
        view.showRoundResult(getTitle(), reason);
    }

    @Override
    public void onNextRoundRequested(Round round) {
        round.getView().hideRoundResult();   // the state that showed it, hides it
        round.getTeamA().clearArmy();
        round.getTeamB().clearArmy();
        round.setCurrentState(new AllocateState());
    }
}
