package entity.troops.support;

import entity.troops.Healable;
import entity.troops.Stats;
import entity.troops.Support;

/*
 * Base class for healing troop types.
 */
public abstract class Healer extends Support {
    /* ---- STATIC VARIABLES ---- */
    private static final long HEAL_GLOW_DURATION = 1500;

    /* ---- PRIVATE VARIABLES ---- */
    private Healable target;
    private long lastHealTime;

    /* ---- CONSTRUCTORS ---- */
    public Healer(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
        lastHealTime = 0;
    }

    /* ---- PUBLIC METHODS ---- */
    public void heal() {
        this.target.beHealed(multipliedHealAmount());
        lastHealTime = System.currentTimeMillis();
    }

    public boolean hasRecentlyHealed() {
        return System.currentTimeMillis() - lastHealTime < HEAL_GLOW_DURATION;
    }

    /* ---- GETTER METHODS ---- */
    public int getHealAmount() {
        return stats.healAmount();
    }

    public Healable getTarget() {
        return target;
    }

    /* ---- SETTER METHODS ---- */
    public void setTarget(Healable target) {
        this.target = target;
    }

    /* ---- PRIVATE METHODS ---- */
    // Heal grows with the square root of the group size (diminishing returns)
    private int multipliedHealAmount() {
        return (int) (stats.healAmount() * Math.sqrt(getGroupSize()));
    }
}