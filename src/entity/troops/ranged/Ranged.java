package entity.troops.ranged;

import entity.troops.Attacker;
import entity.troops.Stats;

/*
 * Base class for ranged troop types: they have attack range but no armour.
 */
public abstract class Ranged extends Attacker {

    /* ---- CONSTRUCTORS ---- */
    public Ranged(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }
}
