package entity.troops;

import entity.Damageable;

import java.util.Optional;

/*
 * Base class for attacking troop types.
 * Melee and ranged troop classes extend this.
 */
public abstract class Attacker extends Troop implements Healable {
    /* ---- PRIVATE VARIABLES ---- */
    private Damageable target;

    /* ---- CONSTRUCTORS ---- */
    public Attacker(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }

    /* ---- PUBLIC METHODS ---- */
    /*
     * damageBonus comes from whatever the attacker happens to be standing on -
     * currently a hill held by its own team. It is passed in rather than
     * stored so nothing has to be cleared when the troop moves off.
     */
    public void attack(int damageBonus) {
        this.target.takeDamage(getAttack() + damageBonus); // attack is proportional to the group size
    }

    public void attack() {
        attack(0);
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void beHealed(int healAmount) {
        groupHealth = Math.min(groupHealth + healAmount, getMaxHealth());
        markHealed();
    }

    /* ---- GETTER METHODS ---- */
    public int getAttack() {
        return stats.baseAttack() * getGroupSize();
    }

    public int getRange() {
        return stats.attackRange();
    }

    public Damageable getTarget() {
        return target;
    }

    /* ---- SETTER METHODS ---- */
    public void setTarget(Damageable target) {
        this.target = target;
    }
}