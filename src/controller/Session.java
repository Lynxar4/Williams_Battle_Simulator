package controller;

import entity.Team;
import javax.swing.Timer;

/*
 * Creates the game's pieces, connects them, and runs the tick timer.
 * Everything that happens in a round is decided by Round and its states.
 */
public class Session {
    /* ---- STATIC VARIABLES ---- */
    private static final int TICK_DURATION = 500;

    /* ---- PRIVATE VARIABLES ---- */
    private final Round round;
    private final Timer timer;

    /* ---- CONSTRUCTOR ---- */
    public Session(GameView view) {
        // Teams are created ONCE, so budgets carry over between rounds.
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        round = new Round(teamA, teamB, view);

        // Player input (clicks, buttons) goes to the round.
        view.setPlayerActions(round);

        // Show tile effect events (coins, traps...) as they happen.
        round.addTileEffectListener(event -> view.showLatestEvent(event.description()));

        // One tick = one round update.
        timer = new Timer(TICK_DURATION, e -> round.update());
    }

    /* ---- PUBLIC METHODS ---- */
    public void start() {
        round.start(); // AllocateState → board shown → PlaceState
        timer.start();
    }
}