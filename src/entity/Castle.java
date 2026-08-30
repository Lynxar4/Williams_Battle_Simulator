package entity;
public class Castle {
    public Castle(int row, int column, int health)
    {
        m_row = row;
        m_column = column;
        m_health = health;
    }

    public int getRow() { return m_row; }
    public int getColumn() { return m_column; }
    public int getHealth() { return m_health; }

    public void takeDamage(int dmg)
    {
        m_health -= dmg;
    }
    public boolean isDestroyed()
    {
        return m_health <= 0;
    }

    private int m_row;
    private int m_column;
    private int m_health;
}