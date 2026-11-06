package entity.troops;

import entity.troops.melee.Cavalry;
import entity.troops.melee.Infantry;
import entity.troops.ranged.Archer;
import entity.troops.support.Priest;

/* Each shop entry owns its price and a constructor for its troop type. */
public enum TroopType {
    INFANTRY("Infantry", 10, Infantry::new),
    ARCHER("Archer", 15, Archer::new),
    CAVALRY("Cavalry", 25, Cavalry::new),
    PRIEST("Priest", 15, Priest::new);

    /* ---- INTERFACES ---- */
    @FunctionalInterface
    private interface TroopFactory {
        Troop create(int row, int column, int groupSize, Troop.Team team);
    }

    /* ---- PRIVATE VARIABLES ---- */
    private final String displayName;
    private final int price;
    private final TroopFactory factory;

    /* ---- CONSTRUCTORS ---- */
    TroopType(String displayName, int price, TroopFactory factory) {
        this.displayName = displayName;
        this.price = price;
        this.factory = factory;
    }

    /* ---- PUBLIC METHODS ---- */
    public int getTotalCost(int groupSize) {
        if (groupSize < 1) {
            throw new IllegalArgumentException("Group size must be a positive whole number.");
        }
        if (groupSize > Integer.MAX_VALUE / price) {
            throw new IllegalArgumentException("That group is too large.");
        }
        return price * groupSize;
    }

    public Troop createTroop(int row, int column, int groupSize, Troop.Team team) {
        return factory.create(row, column, groupSize, team);
    }

    /* ---- OVERRIDES ---- */
    @Override
    public String toString() {
        return displayName + " ($" + price + " each)";
    }

    /* ---- GETTER METHODS ---- */
    public String getDisplayName() {
        return displayName;
    }
}
