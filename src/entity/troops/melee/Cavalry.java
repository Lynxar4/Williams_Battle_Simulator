package entity.troops.melee;

import entity.troops.Stats;

public class Cavalry extends Melee {

    /* ---- STATS ---- */
    private static final int BASE_HEALTH = 150;
    private static final int BASE_ATK = 20;
    private static final int BASE_ARMOUR = 35;

    public static Stats TroopStat = Stats.melee(BASE_HEALTH, BASE_ATK, BASE_ARMOUR);

    /* ---- CONSTRUCTORS ---- */
    public Cavalry(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
