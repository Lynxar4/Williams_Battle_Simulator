package entity.interactables;

import entity.troops.Troop;

// Damages the first enemy troop to enter the cell, then is spent.

// Traps are spawned at random for now, but they carry an owning team from the
// start so that a placement stage can create them the same way later
public class Trap extends TileEffect {

    /* ---- PRIVATE VARIABLES ---- */
    private final int damage;
    private boolean isTriggered;

    /* ---- CONSTRUCTORS ---- */
    public Trap(int row, int column, int damage, Troop.Team owner) {
        super(row, column, owner);
        this.damage = damage;
        isTriggered = false;
    }

    /* ---- GETTER METHODS ---- */
    public int getDamage() {
        return damage;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void onEnter(Troop troop) {
        if (isTriggered) {
            return;
        }

        // Friendly troops walk over their own team's traps safely.
        if (troop.getTeam() == getOwner()) {
            return;
        }

        troop.takeDamage(damage);
        isTriggered = true;
    }

    @Override
    public boolean isConsumed() {
        return isTriggered;
    }

}