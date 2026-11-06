package controller;

import controller.states.*;
import entity.*;
import entity.interactables.*;
import entity.troops.Troop;
import entity.troops.TroopType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/*
What Round is now responsible for?
- Holding the board: castles, tile effects, grid size
- Holding the two teams for this battle
- Knowing which state is active and passing itself to it
- Not deciding what happens in each phase — that's the states
*/
public class Round implements PlayerActions {
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

    // stops spawning spinning forever if there is nowhere left to put things
    private static final int MAX_SPAWN_ATTEMPTS = 500;

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

        // hills and gold are neutral so they can land anywhere, just not right
        // on top of a castle
        SpawnStrategy openGround = SpawnStrategy.anywhere().awayFromEdges(CASTLE_SIZE);
        spawn(HILL_COUNT, openGround,
                cell -> new Hill(cell.row(), cell.column(), HILL_DAMAGE_BONUS));
        spawn(COIN_COUNT, openGround,
                cell -> new Coin(cell.row(), cell.column(), COIN_VALUE));

        // a trap belongs to the team whose half it sits on, so each side gets
        // its own strategy and its own share
        spawnTrapsFor(Troop.Team.teamA);
        spawnTrapsFor(Troop.Team.teamB);
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

    // every effect passing the caller's test. the caller says what it wants
    // rather than round growing a method per question, and tests can be
    // combined with and / or / negate.
    public List<TileEffect> effectsMatching(Predicate<TileEffect> test) {
        return tileEffectBoard.matching(test);
    }

    // counts what is still on the board in one pass.
    // sequential on purpose: there are fourteen effects, and the benchmark in
    // tests shows the split costs more than it saves until there are tens of
    // thousands. the collector itself is parallel safe either way.
    public TileEffectSummary tileEffectSummary() {
        return getTileEffects().stream().collect(new TileEffectSummaryCollector());
    }

    public boolean isBlocked(int row, int column) {
        return !castleBoard.isWithinBounds(row, column) || !castleBoard.isCellFree(row, column) || isTroopAt(row, column);
    }

    public boolean isWithinBoard(int row, int column) {
        return castleBoard.isWithinBounds(row, column);// just reusing the isWithinBounds method, nth related to castle
    }

    public Team getTeam(Troop.Team owner) {
        return owner == Troop.Team.teamA ? teamA : teamB;
    }

    public void resetCastles() {
        teamACastle.resetHealth();
        teamBCastle.resetHealth();
    }

    public Troop.Team ownerOfHalf(int column) {
        return (column < COLUMN_COUNT / 2) ? Troop.Team.teamA : Troop.Team.teamB;
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

    /* ---- OVERRIDES ---- */
    /* PLAYER ACTIONS IMPLEMENTATION */
    @Override
    public void tileClicked(Troop.Team team, TroopType type, int groupSize, int row, int column) {
        currentState.onTileClicked(this, team, type, groupSize, row, column);
    }

    @Override
    public void moveTroopRequested(Troop troop) {
        currentState.onMoveTroopRequested(this, troop);
    }

    @Override
    public void cancelMove() {
        currentState.onCancelMove(this);
    }

    @Override
    public void removeTroopRequested(Troop troop) {
        currentState.onRemoveTroopRequested(this, troop);
    }

    @Override
    public void startBattleRequested() {
        currentState.onStartBattleRequested(this);
    }

    @Override
    public void nextRoundRequested() {
        currentState.onNextRoundRequested(this);
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

    public GameView getView() {
        return view;
    }

    /* ---- SETTER METHODS ---- */
    public void setCurrentState(GameState state) {
        this.currentState = state;
        state.onEnter(this);
    }

    /* ---- PRIVATE METHODS ---- */
    private void spawnTrapsFor(Troop.Team owner) {
        spawn(TRAP_COUNT / 2, SpawnStrategy.ownHalf(owner).awayFromEdges(CASTLE_SIZE),
                cell -> new Trap(cell.row(), cell.column(), TRAP_DAMAGE, owner));
    }

    // the notify step. every listener gets the same event.
    private void fireTileEffectTriggered(TileEffectEvent event) {
        tileEffectListeners.forEach(listener -> listener.onTileEffectTriggered(event));
    }

    private boolean isTroopAt(int row, int column) {
        return livingTroops().anyMatch(troop -> troop.isAt(row, column));
    }

    // both armies as one stream of whatever is still alive. several checks
    // want this and they all used to walk the two lists by hand.
    private Stream<Troop> livingTroops() {
        return Stream.concat(teamA.getArmy().stream(), teamB.getArmy().stream())
                .filter(troop -> !troop.isDestroyed());
    }

    private void placeCastles() {
        try {
            castleBoard.place(teamACastle);
            castleBoard.place(teamBCastle);
        } catch (InvalidPlacementException e) {
            throw new IllegalStateException("Castle positions are off the grid.", e);
        }
    }

    // the one spawning loop. where the effect may go and what to build there
    // are both passed in, so hills, gold and traps share this instead of
    // having a copy of it each.
    // gives up after a fixed number of tries rather than spinning forever if
    // the board has no room left.
    private void spawn(int count, SpawnStrategy strategy, Function<Cell, TileEffect> factory) {
        int placed = 0;
        int attempts = 0;

        while (placed < count && attempts < MAX_SPAWN_ATTEMPTS) {
            ++attempts;
            Cell cell = strategy.pick(random, ROW_COUNT, COLUMN_COUNT);

            if (tryAddTileEffect(factory.apply(cell))) {
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
}
