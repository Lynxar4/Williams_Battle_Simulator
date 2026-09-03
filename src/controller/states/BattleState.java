package controller.states;

import controller.Round;
import entity.Troop;
import java.awt.Graphics;

public class BattleState implements GameState{

    // Move every troop on both teams by 1 step. Called once per tick.
    @Override
    public void update(Round round)
    {
        for (Troop troop : round.getTeamA().getArmy())
        {
            troop.move();
        }
        for (Troop troop : round.getTeamB().getArmy())
        {
            troop.move();
        }
    }

    @Override
    public void render(Round round, Graphics g)
    {
        // no rendering yet since BattlefieldPanel currently handles all drawing.
    }
}