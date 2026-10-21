package controller.states;

import controller.Round;

public class PlaceState implements GameState {
    @Override
    public void update(Round round) {
        // TEMPORARY: skips straight to battle.
        // Real placement will wait for player input before handing over.
        round.setCurrentState(new BattleState());
    }

    @Override
    public void render(Round round) {
        // draw placement-phase UI
    }
}