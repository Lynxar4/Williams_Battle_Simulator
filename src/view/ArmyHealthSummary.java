package view;

/* Immutable results can safely pass from the background worker to Swing. */
public record ArmyHealthSummary(int healthy, int wounded, int critical, int dead,
                                long totalHealth, long maxHealth, int soldiers) {
    /* ---- FACTORY METHODS ---- */
    public static ArmyHealthSummary empty() {
        return new ArmyHealthSummary(0, 0, 0, 0, 0, 0, 0);
    }

    /* ---- PUBLIC METHODS ---- */
    public int livingGroups() {
        return healthy + wounded + critical;
    }

    public int groupCount() {
        return livingGroups() + dead;
    }

    // Defeated groups retain their starting maximum HP in the denominator.
    public double healthPercent() {
        return maxHealth == 0 ? 0 : 100.0 * totalHealth / maxHealth;
    }
}
