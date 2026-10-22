package entity.troops;

import entity.Damageable;

public abstract class Troop implements Damageable {

    /* ---- ENUMS ---- */
    public enum Team {
        teamA,
        teamB
    }

    /* ---- PRIVATE VARIABLES ---- */
    // Position
    private int row;
    private int column;
    private final Team team;

    /* ---- PROTECTED VARIABLES ---- */
    protected final Stats stats; // base stats for this troop type, never change
    protected int initialGroupSize;
    protected int groupHealth;

    /* ---- CONSTRUCTORS ---- */
    public Troop(int row, int column, Stats stats, int initialGroupSize, Team team) {
        this.row = row;
        this.column = column;
        this.initialGroupSize = initialGroupSize;
        this.stats = stats;
        this.groupHealth = getMaxHealth(); // default groupHealth is maxed out
        this.team = team;
    }

    /* ---- PUBLIC METHODS ---- */
    public void move() {
        // Troop movement logic
    }

    @Override
    public void takeDamage(int dmg) {
        // Damage is reduced in proportion to the troop's armour
        int reducedDamage = dmg * (100 - stats.armour()) / 100;
        groupHealth = Math.max(groupHealth - reducedDamage, 0);
    }

    @Override
    public boolean isDestroyed() {
        return groupHealth <= 0;
    }

    @Override
    public boolean isAt(int row, int column) {
        return this.row == row && this.column == column;
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

    public int getGroupHealth() {
        return groupHealth;
    }

    public int getSpeed() {
        return stats.speed();
    }

    public int getInitialGroupSize() {
        return initialGroupSize;
    }

    public Team getTeam() {
        return team;
    }

    public Stats getStats() {
        return stats;
    }

    public int getGroupSize() {
        // (baseHealth - 1) is an offset so the last soldier is still counted,
        // since integer division rounds down
        return (groupHealth + (stats.baseHealth() - 1)) / stats.baseHealth();
    }

    public int getMaxHealth() {
        return getStats().baseHealth() * initialGroupSize;
    }

    /* ---- SETTER METHODS ---- */
    public void setPosition(int row, int column) {
        this.row = row;
        this.column = column;
    }
}
