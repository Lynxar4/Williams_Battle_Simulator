package entity.troops;

import entity.Damageable;

/*
 * Base class for attacking troop types.
 * Melee and ranged troop classes extend this.
 */
public abstract class Attacker extends Troop implements Healable {

    /* ---- CONSTRUCTORS ---- */
    public Attacker(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }

    /* ---- PUBLIC METHODS ---- */
    @Override
    public void beHealed(int healAmount) {
        groupHealth = Math.min(groupHealth + healAmount, getMaxHealth());
    }

    public void attack(Damageable target) {
        target.takeDamage(getAttack()); // attack is proportional to the group size
    }

    /* ---- GETTER METHODS ---- */
    public int getAttack() {
        return stats.baseAttack() * getGroupSize();
    }

    public int getRange() {
        return stats.attackRange();
    }
}
