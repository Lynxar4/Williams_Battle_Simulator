package entity;

public class Castle implements Damageable {
    private final int BASE_HEALTH = 1000;
    private int row;
    private int column;
    private int health;
    private int size;

    public Castle(int row, int column, int health) {
        this(row, column, health, 1);
    }

    public Castle(int row, int column, int health, int size) {
        this.row = row;
        this.column = column;
        this.health = BASE_HEALTH;
        this.size = size;
    }

    @Override
    public boolean isAt(int row, int column) {
        return row >= this.row
                && row < this.row + size
                && column >= this.column
                && column < this.column + size;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    public void resetHealth() {
        health = BASE_HEALTH;
    }

    public int getHealth() {
        return health;
    }

    public int getSize() {
        return size;
    }

    public void takeDamage(int dmg) {
        health -= dmg;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

}