package entity.troops.melee;

import entity.troops.Attacker;
import entity.troops.Stats;

/*
 * Base class for melee troop types: they fight up close and have armour.
 */
public abstract class Melee extends Attacker {

    /* ---- CONSTRUCTORS ---- */
    public Melee(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }

    /* ---- GETTER METHODS ---- */
    public int getArmour() {
        return stats.armour();
    }
}
