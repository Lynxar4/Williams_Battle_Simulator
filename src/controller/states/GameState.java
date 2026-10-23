package controller.states;

import controller.Round;

public interface GameState {
    public void update(Round round);

    public void render(Round round);
}