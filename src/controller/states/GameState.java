package controller.states;

import java.awt.Graphics;

import controller.states.Round;

interface GameState{
    public void update(Round round);
    public void render(Round round,Graphics graphics);
}