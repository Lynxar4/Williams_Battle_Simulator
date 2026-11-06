package controller;

import entity.Castle;
import entity.Team;
import entity.troops.Troop;

/* A fixed copy of the results, captured when the battle ends. */
public record BattleSummary(ArmyResult teamA, ArmyResult teamB) {
    /* ---- RECORDS ---- */
    public record ArmyResult(int started, int survived, int castleHealth) {
        public int lost() {
            return started - survived;
        }
    }

    /* ---- FACTORY METHODS ---- */
    public static BattleSummary capture(Round round) {
        return new BattleSummary(summarise(round.getTeamA(), round.getTeamACastle()),
                summarise(round.getTeamB(), round.getTeamBCastle()));
    }

    /* ---- PRIVATE METHODS ---- */
    private static ArmyResult summarise(Team team, Castle castle) {
        // Removed deployment groups are no longer in the army; defeated groups remain.
        int started = team.getArmy().stream().mapToInt(Troop::getInitialGroupSize).sum();
        int survived = team.getArmy().stream().mapToInt(Troop::getGroupSize).sum();
        return new ArmyResult(started, survived, Math.max(0, castle.getHealth()));
    }
}
