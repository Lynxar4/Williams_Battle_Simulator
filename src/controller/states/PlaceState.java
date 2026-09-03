package controller.states;

import controller.Round;
import java.awt.Graphics;

public class PlaceState implements GameState{
    @Override
    public void update(Round round)
    {
        // Handle troop placement which is currently implemented in BattlefieldPanel
    }

    @Override
    public void render(Round round, Graphics g)
    {
        // draw placement-phase UI
    }
}