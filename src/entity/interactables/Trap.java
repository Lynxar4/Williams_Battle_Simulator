package entity.interactables;

import entity.troops.Troop;
import java.util.Optional;

// damages the first enemy troop to enter the cell, then is spent.
// traps spawn at random for now but carry an owning team from the start, so a
// placement stage can create them the same way later.

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

    /* ---- PUBLIC METHODS ---- */
    public boolean isTriggered() {
        return isTriggered;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public Optional<TileEffectEvent> onEnter(Troop troop) {
        if (isTriggered) {
            return Optional.empty();
        }

        // friendly troops walk over their own team's traps safely
        if (troop.getTeam() == getOwner()) {
            return Optional.empty();
        }

        troop.takeDamage(damage);
        isTriggered = true;

        return Optional.of(TileEffectEvent.of(
                this, troop, troop.getTeam() + " walked into a trap (-" + damage + " dmg)"));
    }

    @Override
    public boolean isConsumed() {
        return isTriggered;
    }

    @Override
    public <R> R accept(TileEffectVisitor<R> visitor) {
        return visitor.visitTrap(this);
    }

    /* ---- GETTER METHODS ---- */
    public int getDamage() {
        return damage;
    }
}
