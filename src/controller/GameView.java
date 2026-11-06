package controller;

import entity.Castle;
import entity.Team;
import entity.interactables.TileEffect;
import entity.troops.Troop;
import java.util.List;


/*
 * Everything the controller (Round and its states) can ask the screen to do.
 * The controller only talks to this, never to Swing panels directly.
 */
public interface GameView {

    void showBoard(int rows, int columns, Team teamA, Team teamB,
                   Castle castleA, Castle castleB, List<TileEffect> tileEffects);

    void refreshBoard();
    void setPlacementEnabled(boolean isEnabled);
    void showGold(int teamAGold, int teamBGold);
    void setStartBattleAllowed(boolean allowed);
    void setPlacementRule(PlacementRule rule);
    void highlightMovingTroop(Troop troop); // null clears the highlight
    void showMessage(String text, boolean isError);
    void showLatestEvent(String text);
    void showRoundResult(String title, String reason);
    void hideRoundResult();
    void setPlayerActions(PlayerActions actions);
}