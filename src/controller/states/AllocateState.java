package controller.states;

import controller.Round;
import java.awt.Graphics;

public class AllocateState implements GameState{

    @Override
    public void update(Round round)
    {
        // Allocate troops and budget
    }

    @Override
    public void render(Round round, Graphics g)
    {
        // I don't think this state needs to render anything
    }
}