package view;

import entity.troops.Troop;

@FunctionalInterface
public interface PlacementRule {
    boolean isValid(Troop.Team team, int row, int column);
}
