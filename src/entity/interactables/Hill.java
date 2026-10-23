package entity.interactables;

import entity.troops.Troop;

//hill gets genrerated on the map, then give damage bonus to the team that holds it.

public class Hill extends TileEffect {

    /* ---- PRIVATE VARIABLES ---- */
    private final int damageBonus;
    private Troop.Team holder;

    /* ---- CONSTRUCTORS ---- */
    public Hill(int row, int column, int damageBonus) {
        // hills are neutral terrain, so no team owns them at placement time.
        super(row, column, null);
        this.damageBonus = damageBonus;
        holder = null;
    }

    /* ---- GETTER METHODS ---- */
    public int getDamageBonus() {
        return damageBonus;
    }

    public Troop.Team getHolder() {
        return holder;
    }

    /* ---- PUBLIC METHODS ---- */
    public boolean isClaimed() {
        return holder != null;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public void onEnter(Troop troop) {
        // first team to arrive keeps it for the rest of the round.
        if (holder == null) {
            holder = troop.getTeam();
        }
    }

    @Override
    public boolean isConsumed() {
        return false;
    }

}