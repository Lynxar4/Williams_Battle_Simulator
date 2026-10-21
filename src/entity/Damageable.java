package entity;

public interface Damageable extends Positioned {
    void takeDamage(int dmg);
    boolean isAlive();
}
