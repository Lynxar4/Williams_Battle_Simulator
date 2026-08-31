package entity;

public abstract class TileEffect {
    public TileEffect(int row, int column, Troop.Team owner)
    {
        m_row = row;
        m_column = column;
        m_owner = owner;
    }

    public int getRow() { return m_row; }
    public int getColumn() { return m_column; }
    public Troop.Team getOwner() { return m_owner; }

    public boolean isAt(int row, int column)
    {
        return m_row == row && m_column == column;
    }

    // called when a troop moves onto x cel
    public abstract void onEnter(Troop troop);

    // true once the effect is spent and should be removed
    // hills always return false, traps will return true after firing.
    public abstract boolean isConsumed();

    private int m_row;
    private int m_column;
    private Troop.Team m_owner;
}