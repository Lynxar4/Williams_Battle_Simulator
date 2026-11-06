package controller.states;

import controller.Round;

public class AllocateState implements GameState {
    private final int INITIAL_BUDGET = 500;
    @Override
    public void onEnter(Round round) {
        round.resetCastles();
        round.spawnTileEffects();   // already clears last round's effects first
        allocateInitialBudget(round);
        round.getView().showBoard(round.getRowCount(), round.getColumnCount(),
                round.getTeamA(), round.getTeamB(),
                round.getTeamACastle(), round.getTeamBCastle(),
                round.getTileEffects());

        round.setCurrentState(new PlaceState());
    }
    private void allocateInitialBudget(Round round){
        round.getTeamA().addBudget(INITIAL_BUDGET);
        round.getTeamB().addBudget(INITIAL_BUDGET);
    }
}