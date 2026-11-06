package entity.interactables;

import entity.troops.Troop;
import entity.troops.ranged.Ranged;
import java.util.Optional;

// hill gets generated on the map, then gives a damage bonus to the team that holds it.
// ranged troops also shoot further from up here.

public class Hill extends TileEffect {
    /* ---- STATIC VARIABLES ---- */
    private static final int DEFAULT_RANGE_BONUS = 1;

    /* ---- PRIVATE VARIABLES ---- */
    private final int damageBonus;
    private final int rangeBonus;
    private Troop.Team holder;

    /* ---- CONSTRUCTORS ---- */
    public Hill(int row, int column, int damageBonus) {
        this(row, column, damageBonus, DEFAULT_RANGE_BONUS);
    }

    public Hill(int row, int column, int damageBonus, int rangeBonus) {
        // hills are neutral terrain, so no team owns them at placement time.
        super(row, column, null);
        this.damageBonus = damageBonus;
        this.rangeBonus = rangeBonus;
        holder = null;
    }

    /* ---- PUBLIC METHODS ---- */
    public boolean isClaimed() {
        return holder != null;
    }

    /* ---- OVERRIDES ---- */
    // only the holding team gets the bonus, and only while standing here.
    // read at the moment of the attack, so nothing is undone when it leaves.
    @Override
    public int damageBonusFor(Troop troop) {
        return (holder == troop.getTeam()) ? damageBonus : 0;
    }

    // only ranged troops gain range from high ground. melee still has to walk
    // up to whatever it is hitting, so height buys it nothing.
    @Override
    public int rangeBonusFor(Troop troop) {
        if (holder != troop.getTeam()) {
            return 0;
        }
        return (troop instanceof Ranged) ? rangeBonus : 0;
    }

    @Override
    public Optional<TileEffectEvent> onEnter(Troop troop) {
        // first team to arrive keeps it for the rest of the round
        if (holder != null) {
            return Optional.empty();
        }

        holder = troop.getTeam();

        return Optional.of(TileEffectEvent.of(
                this, troop, troop.getTeam() + " took the high ground (+" + damageBonus + " dmg)"));
    }

    @Override
    public boolean isConsumed() {
        return false;
    }

    @Override
    public <R> R accept(TileEffectVisitor<R> visitor) {
        return visitor.visitHill(this);
    }

    /* ---- GETTER METHODS ---- */
    public int getDamageBonus() {
        return damageBonus;
    }

    public int getRangeBonus() {
        return rangeBonus;
    }

    public Troop.Team getHolder() {
        return holder;
    }
}
