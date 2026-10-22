package entity.troops;

/*
 * Anything that can be healed by a healer.
 */
public interface Healable {
    void beHealed(int healAmount);
}
