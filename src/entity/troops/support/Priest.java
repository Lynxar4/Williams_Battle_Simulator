package entity.troops.support;

import entity.troops.Stats;

public class Priest extends Healer {

    /* ---- STATS ---- */
    private static final int BASE_HEALTH = 100;
    private static final int HEAL_AMOUNT = 5;

    public static Stats TroopStat = Stats.healer(BASE_HEALTH, HEAL_AMOUNT);

    /* ---- CONSTRUCTORS ---- */
    public Priest(int row, int column, int initialGroupSize, Team team) {
        super(row, column, TroopStat, initialGroupSize, team);
    }
}
