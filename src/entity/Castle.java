package entity;

public class Castle implements Damageable {
    private final int BASE_HEALTH = 1000;
    private int row;
    private int column;
    private int health;

    public Castle(int row, int column, int health) {
        this.row = row;
        this.column = column;
        this.health = BASE_HEALTH;
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

    public int getHealth() {
        return health;
    }

    public void takeDamage(int dmg) {
        health -= dmg;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

}