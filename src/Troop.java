public class Troop {
    public enum Team
    {
        teamA,
        teamB,
    };

    public Troop(int row, int column, int health, int speed, int troopSize, Team team)
    {
        m_row = row;
        m_column = column;
        m_health = health;
        m_speed = speed;
        m_troopSize = troopSize;
        m_team = team;
    }

    public int getRow() {return m_row;}
    public int getColumn() {return m_column;}
    public int getHealth() {return m_health;}
    public int getSpeed() {return m_speed;}
    public int getTroopSize() {return m_troopSize;}
    public Team getTeam() {return m_team;}

    public void move()
    {
        if (m_team == Team.teamA)
        {
            m_column += m_speed;
        }
        else
        {
            m_column -= m_speed;
        }
    }

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
    private int m_speed;
    private int m_troopSize;
    private Team m_team;
}