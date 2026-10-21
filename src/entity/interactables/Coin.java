package entity.interactables;

import entity.troops.Troop;

// gold spawns on the battlefield. The first troop to reach it collects the
// coin for their team.

public class Coin extends TileEffect {
    /* ---- PRIVATE VARIABLES ---- */
    private final int amount;
    private Troop.Team collectedBy;

    /* ---- CONSTRUCTORS ---- */
    public Coin(int row, int column, int amount) {
        // Gold is neutral until someone picks it up.
        super(row, column, null);
        this.amount = amount;
        collectedBy = null;
    }

    /* ---- GETTER METHODS ---- */
    public int getAmount() {
        return amount;
    }

    public Troop.Team getCollectedBy() {
        return collectedBy;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void onEnter(Troop troop) {
        if (collectedBy == null) {
            collectedBy = troop.getTeam();
        }
    }

    // Unlike a Hill, a collected coin is spent and leaves the board.
    @Override
    public boolean isConsumed() {
        return collectedBy != null;
    }

}