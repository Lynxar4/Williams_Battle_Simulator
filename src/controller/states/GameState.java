package controller.states;

import controller.Round;
import entity.troops.Troop;
import entity.troops.TroopType;

public interface GameState {
    default void onEnter(Round round) {}

    default void update(Round round) {}

    default void render(Round round) {}

    default void onTileClicked(Round round, Troop.Team team, TroopType type,
                               int groupSize, int row, int column) {}
    default void onMoveTroopRequested(Round round, Troop troop) {}
    default void onCancelMove(Round round) {}
    default void onRemoveTroopRequested(Round round, Troop troop) {}
    default void onStartBattleRequested(Round round) {}
    default void onNextRoundRequested(Round round) {}
}