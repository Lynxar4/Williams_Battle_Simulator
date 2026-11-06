package view;

import entity.troops.Troop;

/* Copy these values on the Swing event thread before starting background work. */
public record GroupHealthSnapshot(int health, int maxHealth, int soldiers) {
    /* ---- CONSTRUCTORS ---- */
    public GroupHealthSnapshot {
        if (maxHealth <= 0 || health < 0 || health > maxHealth || soldiers < 0) {
            throw new IllegalArgumentException("Invalid group health snapshot.");
        }
    }

    /* ---- FACTORY METHODS ---- */
    public static GroupHealthSnapshot from(Troop troop) {
        return new GroupHealthSnapshot(troop.getGroupHealth(), troop.getMaxHealth(), troop.getGroupSize());
    }
}
