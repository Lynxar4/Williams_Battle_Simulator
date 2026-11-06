package controller;

import entity.troops.Troop;

// what is still out on the battlefield, counted up.
// only effects still on the board are counted, because a collected coin and a
// sprung trap are removed as soon as they fire. gold a team has already banked
// shows in its budget, so there is no point repeating it here.
public record TileEffectSummary(
        int hillsHeldA,
        int hillsHeldB,
        int hillsUnclaimed,
        int goldOnField,
        int coinsOnField,
        int trapsArmedA,
        int trapsArmedB) {

    public static TileEffectSummary empty() {
        return new TileEffectSummary(0, 0, 0, 0, 0, 0, 0);
    }

    /* ---- GETTER METHODS ---- */
    public int hillsHeldBy(Troop.Team team) {
        return team == Troop.Team.teamA ? hillsHeldA : hillsHeldB;
    }

    public int trapsArmedBy(Troop.Team team) {
        return team == Troop.Team.teamA ? trapsArmedA : trapsArmedB;
    }

    public int hillsTotal() {
        return hillsHeldA + hillsHeldB + hillsUnclaimed;
    }
}
