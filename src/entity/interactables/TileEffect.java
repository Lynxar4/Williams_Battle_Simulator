package entity.interactables;

import entity.Positioned;
import entity.troops.Troop;
import java.util.Optional;

public abstract class TileEffect implements Positioned {
    /* ---- PRIVATE VARIABLES ---- */
    private final int row;
    private final int column;
    private final Troop.Team owner;

    /* ---- CONSTRUCTORS ---- */
    public TileEffect(int row, int column, Troop.Team owner) {
        this.row = row;
        this.column = column;
        this.owner = owner;
    }

    /* ---- PUBLIC METHODS ---- */
    // extra damage this effect gives a troop standing on it.
    // only hill overrides it, so combat never has to check what is underfoot.
    public int damageBonusFor(Troop troop) {
        return 0;
    }

    // extra attack range this effect gives a troop standing on it.
    // same deal as the damage bonus: default zero, only hill overrides.
    public int rangeBonusFor(Troop troop) {
        return 0;
    }

    /* ---- ABSTRACT METHODS ---- */
    // called when a troop moves onto this cell.
    // returns an event if something happened, empty if nothing did: a coin
    // already taken, a hill already claimed, a troop on its own trap.
    public abstract Optional<TileEffectEvent> onEnter(Troop troop);

    // true once the effect is spent and should be removed
    // hills always return false, traps return true after firing.
    public abstract boolean isConsumed();

    // hands this effect to the visitor so the visitor's matching method runs.
    // each subclass calls its own one, which is what replaces instanceof.
    public abstract <R> R accept(TileEffectVisitor<R> visitor);

    /* ---- OVERRIDES ---- */
    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public boolean isAt(int row, int column) {
        return this.row == row && this.column == column;
    }

    /* ---- GETTER METHODS ---- */
    public Troop.Team getOwner() {
        return owner;
    }
}
