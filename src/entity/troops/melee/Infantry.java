package entity.troops.melee;

import entity.troops.Stats;

public class Infantry extends Melee {
    /* ---- STATIC VARIABLES ---- */
    /* STATS */
    private static final int BASE_HEALTH = 100;
    private static final int BASE_ATK = 10;
    private static final int BASE_ARMOUR = 25;
    private static final int BASE_ATK_SPEED = 5;
    private static final int BASE_SPEED = 1;

    public static Stats TroopStat = Stats.melee(BASE_HEALTH, BASE_SPEED, BASE_ATK_SPEED, BASE_ATK, BASE_ARMOUR);

    /* ---- CONSTRUCTORS ---- */
    public Infantry(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
