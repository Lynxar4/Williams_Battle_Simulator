package entity.troops.melee;

import entity.troops.Stats;

public class Infantry extends Melee {

    /* ---- STATS ---- */
    private static final int BASE_HEALTH = 100;
    private static final int BASE_ATK = 10;
    private static final int BASE_ARMOUR = 25;

    public static Stats TroopStat = Stats.melee(BASE_HEALTH, BASE_ATK, BASE_ARMOUR);

    /* ---- CONSTRUCTORS ---- */
    public Infantry(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
