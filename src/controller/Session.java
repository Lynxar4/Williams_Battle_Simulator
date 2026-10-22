package controller;

import entity.*;
import entity.troops.Troop;
import javax.swing.JFrame;
import javax.swing.Timer;
import view.*;

public class Session {
    /* ---- STATIC VARIABLES ---- */
    private static final int TICK_DURATION = 300;
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 500;

    /* ---- PRIVATE VARIABLES ---- */
    private final Team teamA;
    private final Team teamB;

    private BattlefieldPanel panel;
    private Round round;
    private Timer timer;

    /* ---- CONSTRUCTORS ---- */
    public Session() {
        teamA = new Team("teamA");
        teamB = new Team("teamB");
        setTemporaryTroops();
    }

    /* ---- PUBLIC METHODS ---- */
    public void start() {
        setUpWindow();
        startRound();
        startTimer();
    }

    /* ---- PRIVATE METHODS ---- */
    /*
     * Configures the jframe that will be shown on screen
     */
    private void setUpWindow() {
        panel = new BattlefieldPanel();

        JFrame frame = new JFrame("The Williams - Battle Simulator");
        frame.add(panel);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /*
     * Creates a round instance, sets the round for the panel and starts a round
     */
    private void startRound() {
        round = new Round(teamA, teamB);
        panel.setRound(round);
        round.start();
    }

    /*
     * Initiate the simulation timer that handle all the time related activities for
     * the simulation
     */
    private void startTimer() {
        timer = new Timer(TICK_DURATION, e -> {
            round.update();
            panel.repaint();
        });
        timer.start();
    }

    /* ---- TEMPORARY METHOD ---- */
    private void setTemporaryTroops() {
//        teamA.addTroop(new Troop(3, 2, 100, 1, 10, Troop.Team.teamA));
//        teamB.addTroop(new Troop(3, 9, 100, 1, 10, Troop.Team.teamB));
//        teamA.addTroop(new Troop(3, 2, 100, 1, 10, Troop.Team.teamA));
//        teamB.addTroop(new Troop(3, 9, 100, 1, 10, Troop.Team.teamB));
    }
}
