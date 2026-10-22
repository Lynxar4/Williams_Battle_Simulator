package entity.troops.support;

import entity.troops.Healable;
import entity.troops.Stats;
import entity.troops.Support;

/*
 * Base class for healing troop types.
 */
public abstract class Healer extends Support {

    /* ---- CONSTRUCTORS ---- */
    public Healer(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }

    /* ---- PUBLIC METHODS ---- */
    public void heal(Healable target) {
        target.beHealed(multipliedHealAmount());
    }

    /* ---- PRIVATE METHODS ---- */
    // Heal grows with the square root of the group size (diminishing returns)
    private int multipliedHealAmount() {
        return (int) (stats.healAmount() * Math.sqrt(getGroupSize()));
    }

    /* ---- GETTER METHODS ---- */
    public int getHealAmount() {
        return stats.healAmount();
    }
}
