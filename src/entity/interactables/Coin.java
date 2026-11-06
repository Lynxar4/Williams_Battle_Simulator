package entity.interactables;

import entity.troops.Troop;
import java.util.Optional;

// gold spawns on the battlefield. the first troop to reach it collects the
// coin for their team.

public class Coin extends TileEffect {
    /* ---- PRIVATE VARIABLES ---- */
    private final int amount;
    private Troop.Team collectedBy;

    /* ---- CONSTRUCTORS ---- */
    public Coin(int row, int column, int amount) {
        // gold is neutral until someone picks it up.
        super(row, column, null);
        this.amount = amount;
        collectedBy = null;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public Optional<TileEffectEvent> onEnter(Troop troop) {
        // someone already took it this round
        if (collectedBy != null) {
            return Optional.empty();
        }

        collectedBy = troop.getTeam();

        // the coin names its own payout, so the listener never has to work out
        // what a coin is worth
        return Optional.of(TileEffectEvent.paying(
                this, troop, troop.getTeam() + " collected " + amount + " gold", amount));
    }

    // unlike a hill, a collected coin is spent and leaves the board.
    @Override
    public boolean isConsumed() {
        return collectedBy != null;
    }

    @Override
    public <R> R accept(TileEffectVisitor<R> visitor) {
        return visitor.visitCoin(this);
    }

    /* ---- GETTER METHODS ---- */
    public int getAmount() {
        return amount;
    }

    public Troop.Team getCollectedBy() {
        return collectedBy;
    }
}
