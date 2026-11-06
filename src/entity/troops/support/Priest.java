package entity.troops.support;

import entity.troops.Stats;

public class Priest extends Healer {
    /* ---- STATIC VARIABLES ---- */
    /* STATS */
    private static final int BASE_HEALTH = 100;
    private static final int HEAL_AMOUNT = 5;
    private static final int BASE_SPEED = 1;

    public static Stats TroopStat = Stats.healer(BASE_HEALTH, BASE_SPEED, HEAL_AMOUNT);

    /* ---- CONSTRUCTORS ---- */
    public Priest(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
