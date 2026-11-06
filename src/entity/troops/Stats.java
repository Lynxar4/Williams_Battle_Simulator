package entity.troops;

/*
 * Base stats for a troop type. Use the factory methods below so each
 * troop only lists the stats that matter to it.
 */
public record Stats(int baseHealth, int speed, int baseAttack, int atkSpeed, int attackRange, int armour,
                    int healAmount) {
    /* ---- FACTORY METHODS ---- */
    // Melee: has attack and armour, no healing
    public static Stats melee(int baseHealth, int speed, int atkSpeed, int baseAttack, int armour) {
        return new Stats(baseHealth, speed, baseAttack, atkSpeed, 1, armour, 0);
    }

    // Ranged: has attack, no armour, no healing, extended range
    public static Stats ranged(int baseHealth, int speed, int atkSpeed, int baseAttack) {
        return new Stats(baseHealth, speed, baseAttack, atkSpeed, 3, 0, 0);
    }

    // Healer: heals nearby allies, no attack or armour
    public static Stats healer(int baseHealth, int speed, int healAmount) {
        return new Stats(baseHealth, speed, 0, 0, 1, 0, healAmount);
    }
}
