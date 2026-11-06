package controller;

import entity.Team;
import entity.interactables.TileEffectEvent;
import entity.interactables.TileEffectListener;
import entity.troops.Troop;

// pays the troop's team whatever the event says it is owed.
// never asks what kind of effect fired: a coin reports 25, a hill and a trap
// report zero, and the arithmetic is the same either way.

public class BudgetListener implements TileEffectListener {
    /* ---- PRIVATE VARIABLES ---- */
    private final Team teamA;
    private final Team teamB;

    /* ---- CONSTRUCTORS ---- */
    public BudgetListener(Team teamA, Team teamB) {
        this.teamA = teamA;
        this.teamB = teamB;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void onTileEffectTriggered(TileEffectEvent event) {
        if (event.budgetChange() == 0) {
            return;
        }

        teamFor(event.troop()).addBudget(event.budgetChange());
    }

    /* ---- PRIVATE METHODS ---- */
    private Team teamFor(Troop troop) {
        return (troop.getTeam() == Troop.Team.teamA) ? teamA : teamB;
    }
}
