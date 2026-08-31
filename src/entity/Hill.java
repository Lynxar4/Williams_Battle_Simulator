package entity;

//hill gets genrerated on the map, then give damage bonus to the team that holds it.

public class Hill extends TileEffect {
    public Hill(int row, int column, int damageBonus)
    {
        // hills are neutral terrain, so no team owns them at placement time.
        super(row, column, null);
        m_damageBonus = damageBonus;
        m_holder = null;
    }

    public int getDamageBonus() { return m_damageBonus; }
    public Troop.Team getHolder() { return m_holder; }
    public boolean isClaimed() { return m_holder != null; }

    @Override
    public void onEnter(Troop troop)
    {
        // first team to arrive keeps it for the rest of the round.
        if (m_holder == null)
        {
            m_holder = troop.getTeam();
        }
    }

    @Override
    public boolean isConsumed()
    {
        return false;
    }

    private int m_damageBonus;
    private Troop.Team m_holder;
}