package controller;

import controller.states.*;
import entity.*;
import entity.interactables.*;
import entity.troops.Troop;
import entity.troops.TroopType;
import view.GameView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/*
What Round is now responsible for?
- Holding the board: castles, tile effects, grid size
- Holding the two teams for this battle
- Knowing which state is active and passing itself to it
- Not deciding what happens in each phase — that's the states
*/
public class Round {

    /* ---- STATIC VARIABLES ---- */

    /* VARIABLES FOR GRID */
    private static final int ROW_COUNT = 30;
    private static final int COLUMN_COUNT = 30;
    private static final int CASTLE_SIZE = 3;

    /* VARIABLES FOR TILE EFFECT */
    private static final int HILL_COUNT = 4;
    private static final int HILL_DAMAGE_BONUS = 5;
    private static final int COIN_COUNT = 6;
    private static final int COIN_VALUE = 25;
    private static final int TRAP_COUNT = 4;
    private static final int TRAP_DAMAGE = 20;

    /* ---- PRIVATE VARIABLES ---- */
    /*
     * holds the information for all the entites on the board
    */
    private final GridContents<TileEffect> tileEffectBoard = new GridContents<>(ROW_COUNT, COLUMN_COUNT);
    private final GridContents<Castle> castleBoard = new GridContents<>(ROW_COUNT, COLUMN_COUNT);

    private final Team teamA;
    private final Team teamB;
    private final Castle teamACastle;
    private final Castle teamBCastle;

    private final Random random = new Random();

    // listeners that react when a tile effect fires. round holds them but
    // does not know what any of them do.
    private final List<TileEffectListener> tileEffectListeners = new ArrayList<>();
    private GameState currentState;
    private final GameView view;

    /* ---- CONSTRUCTORS ---- */
    public Round(Team teamA, Team teamB, GameView view) {
        this.teamA = teamA;
        this.teamB = teamB;
        int castleRow = ROW_COUNT / 2 - CASTLE_SIZE / 2;

        teamACastle = new Castle(castleRow, 0, 100, CASTLE_SIZE);
        teamBCastle = new Castle(castleRow, COLUMN_COUNT - CASTLE_SIZE, 100, CASTLE_SIZE);
        placeCastles();

        // paying out gold is a reaction, not part of resolving the effect
        addTileEffectListener(new BudgetListener(teamA, teamB));
        this.view = view;
    }

    /* ---- PUBLIC METHODS ---- */
    public void start() {
        setCurrentState(new AllocateState());
    }

    public void update() {
        currentState.update(this); // State needs access to Round's data
        currentState.render(this);
    }


    /*
     * Spawn all the tile effect that have been created
     */
    public void spawnTileEffects() {
        tileEffectBoard.clear(); // removes all the tile effects from previous round
        spawnHills();
        spawnCoins();
        spawnTraps();
    }

    public void addTileEffectListener(TileEffectListener listener) {
        tileEffectListeners.add(listener);
    }

    // fires whatever the troop is standing on, tells the listeners if
    // anything actually happened, then clears out spent effects
    public void applyTileEffects(Troop troop) {
        tileEffectBoard.at(troop.getRow(), troop.getColumn()).flatMap(effect -> effect.onEnter(troop)).ifPresent(this::fireTileEffectTriggered);

        tileEffectBoard.removeIf(TileEffect::isConsumed);
    }

    // extra damage the cell under this troop is worth. hills answer with
    // their bonus, everything else answers zero.
    public int damageBonusAt(Troop troop) {
        return tileEffectBoard.at(troop.getRow(), troop.getColumn()).map(effect -> effect.damageBonusFor(troop)).orElse(0);
    }

    // extra attack range the cell under this troop is worth. hills answer for
    // ranged troops only, everything else answers zero.
    public int rangeBonusAt(Troop troop) {
        return tileEffectBoard.at(troop.getRow(), troop.getColumn()).map(effect -> effect.rangeBonusFor(troop)).orElse(0);
    }

    public boolean isCellFree(int row, int column) {
        return !isBlocked(row, column) && tileEffectBoard.isCellFree(row, column);
    }

    public boolean isBlocked(int row, int column) {
        return !castleBoard.isWithinBounds(row, column) || !castleBoard.isCellFree(row, column) || isTroopAt(row, column);
    }

    public void tileClicked(Troop.Team team, TroopType type, int groupSize, int row, int column) {
        currentState.onTileClicked(this, team, type, groupSize, row, column);
    }

    public void moveTroopRequested(Troop troop) {
        currentState.onMoveTroopRequested(this, troop);
    }

    public void cancelMove() {
        currentState.onCancelMove(this);
    }

    public void removeTroopRequested(Troop troop) {
        currentState.onRemoveTroopRequested(this, troop);
    }

    public void startBattleRequested() {
        currentState.onStartBattleRequested(this);
    }

    public void nextRoundRequested() {
        currentState.onNextRoundRequested(this);
    }

    public boolean isWithinBoard(int row, int column) {
        return castleBoard.isWithinBounds(row, column);// just reusing the isWithinBounds method, nth related to castle
    }

    public Team getTeam(Troop.Team owner) {
        return owner == Troop.Team.teamA ? teamA : teamB;
    }

    /*---- PRIVATE METHODS ---- */

    // the notify step. every listener gets the same event.
    private void fireTileEffectTriggered(TileEffectEvent event) {
        for (TileEffectListener listener : tileEffectListeners) {
            listener.onTileEffectTriggered(event);
        }
    }

    private boolean isTroopAt(int row, int column) {
        for (Troop troop : teamA.getArmy()) {
            if (!troop.isDestroyed() && troop.isAt(row, column)) {
                return true;
            }
        }

        for (Troop troop : teamB.getArmy()) {
            if (!troop.isDestroyed() && troop.isAt(row, column)) {
                return true;
            }
        }
        return false;
    }

    private void placeCastles() {
        try {
            castleBoard.place(teamACastle);
            castleBoard.place(teamBCastle);
        } catch (InvalidPlacementException e) {
            throw new IllegalStateException("Castle positions are off the grid.", e);
        }
    }

    /*
     * hills spawn at random empty cells at the start of the round
     */
    private void spawnHills() {
        int placed = 0;

        while (placed < HILL_COUNT) {
            int row = random.nextInt(ROW_COUNT);
            int column = random.nextInt(COLUMN_COUNT);

            if (tryAddTileEffect(new Hill(row, column, HILL_DAMAGE_BONUS))) {
                ++placed;
            }
        }
    }

    private void spawnCoins() {
        int placed = 0;

        while (placed < COIN_COUNT) {
            int row = random.nextInt(ROW_COUNT);
            int column = random.nextInt(COLUMN_COUNT);

            if (tryAddTileEffect(new Coin(row, column, COIN_VALUE))) {
                ++placed;
            }
        }
    }

    /*
     * Traps spawn at random for now. A trap belongs to whichever team owns the half
     * it lands on, which is the same rule a placement stage will enforce, so only
     * the choice of cell has to change later.
     */
    private void spawnTraps() {
        int placed = 0;

        while (placed < TRAP_COUNT) {
            int row = random.nextInt(ROW_COUNT);
            int column = random.nextInt(COLUMN_COUNT);

            if (tryAddTileEffect(new Trap(row, column, TRAP_DAMAGE, ownerOfHalf(column)))) {
                ++placed;
            }
        }
    }

    /*
     * Random spawning expects most cells to be taken, so a rejection here is
     * normal rather than an error worth reporting.
     */
    private boolean tryAddTileEffect(TileEffect effect) {
        try {
            addTileEffect(effect);
            return true;
        } catch (InvalidPlacementException e) {
            return false;
        }
    }

    /* Tile effects use the same occupancy check as troop purchases. */
    private void addTileEffect(TileEffect effect) throws InvalidPlacementException {
        if (!isCellFree(effect.getRow(), effect.getColumn())) {
            throw new InvalidPlacementException("Cell " + effect.getRow() + "," + effect.getColumn() + " is already taken.");
        }

        tileEffectBoard.place(effect);
    }

    public Troop.Team ownerOfHalf(int column) {
        return (column < COLUMN_COUNT / 2) ? Troop.Team.teamA : Troop.Team.teamB;
    }

    /* ---- GETTER METHODS ---- */
    public Team getTeamA() {
        return teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public Castle getTeamACastle() {
        return teamACastle;
    }

    public Castle getTeamBCastle() {
        return teamBCastle;
    }

    public List<TileEffect> getTileEffects() {
        return tileEffectBoard.getContents();
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public int getRowCount() {
        return ROW_COUNT;
    }

    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public Team getEnemyTeam(Troop troop) {
        return troop.getTeam() == Troop.Team.teamA ? teamB : teamA;
    }

    public Team getOwnTeam(Troop troop) {
        return troop.getTeam() == Troop.Team.teamA ? teamA : teamB;
    }

    public Castle getEnemyCastle(Troop troop) {
        return troop.getTeam() == Troop.Team.teamA ? teamBCastle : teamACastle;
    }

    public GameView getView() {
        return view;
    }

    /* ---- SETTER METHODS ---- */
    public void setCurrentState(GameState state) {
        this.currentState = state;
        state.onEnter(this);
    }

}
