package entity.troops;

import entity.Damageable;
import entity.Positioned;

public abstract class Troop implements Damageable {
    /* ---- ENUMS ---- */
    public enum Team {
        teamA,
        teamB
    }

    /* ---- STATIC VARIABLES ---- */
    private static final long HEAL_GLOW_DURATION = 1500;

    /* ---- PRIVATE VARIABLES ---- */
    // Position
    private int row;
    private int column;
    private final Team team;

    private long lastHealedTime;
    private long deathTime;

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
        this.lastHealedTime = 0;
        this.deathTime = 0;
    }

    /* ---- PUBLIC METHODS ---- */
    public void move() {
        // Troop movement logic
    }

    public void markHealed() {
        lastHealedTime = System.currentTimeMillis();
    }

    public boolean wasRecentlyHealed() {
        return System.currentTimeMillis() - lastHealedTime < HEAL_GLOW_DURATION;
    }

    /* ---- ABSTRACT METHODS ---- */
    public abstract Positioned getTarget();

    /* ---- OVERRIDES ---- */
    @Override
    public void takeDamage(int dmg) {
        // Damage is reduced in proportion to the troop's armour
        boolean wasAlive = groupHealth > 0;

        int reducedDamage = dmg * (100 - stats.armour()) / 100;
        groupHealth = Math.max(groupHealth - reducedDamage, 0);

        if (wasAlive && groupHealth == 0) {
            deathTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isDestroyed() {
        return groupHealth <= 0;
    }

    @Override
    public boolean isAt(int row, int column) {
        return this.row == row && this.column == column;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    /* ---- GETTER METHODS ---- */
    public int getGroupHealth() {
        return groupHealth;
    }

    public int getSpeed() {
        return stats.speed();
    }

    public int getAttackSpeed() {
        return stats.atkSpeed();
    }

    public int getRange() {
        return stats.attackRange();
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

    public long getDeathTime() {
        return deathTime;
    }

    public int getGroupSize() {
        /*
        (baseHealth - 1) is an offset so the last soldier is still counted,
        as integer division rounds down
         */
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