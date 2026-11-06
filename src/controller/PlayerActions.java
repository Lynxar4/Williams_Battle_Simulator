package controller;

import entity.troops.Troop;
import entity.troops.TroopType;

// Everything the player can do on screen.
public interface PlayerActions {
    void tileClicked(Troop.Team team, TroopType type, int groupSize, int row, int column);
    void moveTroopRequested(Troop troop);
    void cancelMove();
    void removeTroopRequested(Troop troop);
    void startBattleRequested();
    void nextRoundRequested();
}

