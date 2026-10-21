package controller.states;

import controller.Round;

public class AllocateState implements GameState {

    @Override
    public void update(Round round) {
        round.spawnTileEffects();
        round.setCurrentState(new PlaceState());
    }

    @Override
    public void render(Round round) {
        // I don't think this state needs to render anything

    }
}