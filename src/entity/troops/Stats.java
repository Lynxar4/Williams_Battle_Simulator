package entity.troops;

/*
 * Base stats for a troop type. Use the factory methods below so each
 * troop only lists the stats that matter to it.
 */
public record Stats(int baseHealth, int speed, int baseAttack, int attackRange, int armour, int healAmount) {

    /* ---- FACTORY METHODS ---- */

    // Melee: has attack and armour, no healing
    public static Stats melee(int baseHealth, int baseAttack, int armour) {
        return new Stats(baseHealth, 1, baseAttack, 1, armour, 0);
    }

    // Ranged: has attack, no armour, no healing, extended range
    public static Stats ranged(int baseHealth, int baseAttack) {
        return new Stats(baseHealth, 1, baseAttack, 5, 0, 0);
    }

    // Healer: only heals, no attack, no armour
    public static Stats healer(int baseHealth, int healAmount) {
        return new Stats(baseHealth, 1, 0, 0, 0, healAmount);
    }
}
