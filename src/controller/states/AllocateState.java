package controller.states;

import controller.Round;

public class AllocateState implements GameState {

    @Override
    public void onEnter(Round round) {
        round.resetCastles();
        round.spawnTileEffects();   // already clears last round's effects first

        round.getView().showBoard(round.getRowCount(), round.getColumnCount(),
                round.getTeamA(), round.getTeamB(),
                round.getTeamACastle(), round.getTeamBCastle(),
                round.getTileEffects());

        round.setCurrentState(new PlaceState());
    }
}