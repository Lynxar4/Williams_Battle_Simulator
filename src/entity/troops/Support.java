package entity.troops;

/*
 * Base class for support troop types.
 * Healer and other support classes extend this.
 */
public abstract class Support extends Troop {

    /* ---- CONSTRUCTORS ---- */
    public Support(int row, int column, Stats stats, int initialGroupSize, Team team) {
        super(row, column, stats, initialGroupSize, team);
    }
}
