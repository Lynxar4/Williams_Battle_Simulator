package entity.troops.ranged;

import entity.troops.Stats;

public class Archer extends Ranged {
    /* ---- STATIC VARIABLES ---- */
    /* STATS */
    private static final int BASE_HEALTH = 100;
    private static final int BASE_ATK = 10;
    private static final int BASE_ATK_SPEED = 2;
    private static final int BASE_SPEED = 1;

    public static Stats TroopStat = Stats.ranged(BASE_HEALTH, BASE_SPEED, BASE_ATK_SPEED, BASE_ATK);

    /* ---- CONSTRUCTORS ---- */
    public Archer(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
