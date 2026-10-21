package entity.interactables;

import entity.Positioned;
import entity.troops.Troop;

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

    /* ---- GETTER METHODS ---- */
    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    public Troop.Team getOwner() {
        return owner;
    }

    /* ---- PUBLIC METHODS ---- */
    @Override
    public boolean isAt(int row, int column) {
        return this.row == row && this.column == column;
    }

    /* ---- ABSTRACT METHODS ---- */

    // called when a troop moves onto x cel
    public abstract void onEnter(Troop troop);

    // true once the effect is spent and should be removed
    // hills always return false, traps will return true after firing.
    public abstract boolean isConsumed();

}