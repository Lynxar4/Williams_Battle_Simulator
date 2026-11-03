package controller;

import controller.states.RoundOverState;
import entity.Team;
import entity.troops.Troop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import view.*;

public class Session {
    /* ---- STATIC VARIABLES ---- */
    private static final int TICK_DURATION = 500;
    private static final int WINDOW_WIDTH = 1180;
    private static final int BATTLEFIELD_HEIGHT = 680;

    /* ---- PRIVATE VARIABLES ---- */
    private JFrame frame;
    private BattlefieldPanel panel;
    private TroopPurchasePanel purchasePanel;
    private Round round;
    private Timer timer;

    private EventLogListener eventLog;

    /* ---- CONSTRUCTORS ---- */
    public Session() {
        startRound();
    }

    /* ---- PUBLIC METHODS ---- */
    public void start() {
        setUpWindow();
        startTimer();
    }

    public void restart() {
        if (timer != null) {
            timer.stop();
        }
        startRound();
        if (frame != null) {
            frame.getGlassPane().setVisible(false);
            frame.getRootPane().setDefaultButton(null);
            setUpRoundView();
            timer.start();
        }
    }

    /* ---- PRIVATE METHODS ---- */
    /*
     * Configures the jframe that will be shown on screen
     */
    private void setUpWindow() {
        frame = new JFrame("The Williams - Battle Simulator");
        setUpRoundView();
        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(900, screen.width), Math.min(640, screen.height)));
        frame.setSize(Math.min(frame.getWidth(), screen.width), Math.min(frame.getHeight(), screen.height));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setUpRoundView() {
        if (panel == null) {
            panel = new BattlefieldPanel();
        }
        panel.setRound(round);
        panel.setEventLog(eventLog);
        panel.setPlacementTeam(Troop.Team.teamA);
        panel.setFitWholeBoard(false);
        JPanel battlefieldView = panel.createView();
        battlefieldView.setPreferredSize(new Dimension(WINDOW_WIDTH, BATTLEFIELD_HEIGHT));
        purchasePanel = new TroopPurchasePanel(round, panel);
        JPanel content = new JPanel(new BorderLayout());
        content.add(battlefieldView, BorderLayout.CENTER);
        content.add(purchasePanel, BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.revalidate();
        frame.repaint();
    }

    /*
     * Creates the round and allocates its board before placement begins.
     */
    private void startRound() {
        eventLog = new EventLogListener();
        round = new Round(new Team("teamA"), new Team("teamB"));
        // A second listener alongside the budget one. Round fires once and
        // both react, without either knowing the other is registered.
        round.addTileEffectListener(eventLog);
        round.start();
        round.update(); // Allocate the board before showing the buying controls.
    }

    /*
     * Initiate the simulation timer that handle all the time related activities for
     * the simulation
     */
    private void startTimer() {
        timer = new Timer(TICK_DURATION, e -> {
            round.update();
            panel.refresh();
            if (round.getCurrentState() instanceof RoundOverState result) {
                timer.stop();
                purchasePanel.refreshState();
                GameOverPanel prompt = new GameOverPanel(result, this::restart);
                frame.setGlassPane(prompt);
                prompt.setVisible(true);
                frame.getRootPane().setDefaultButton(prompt.getRestartButton());
                prompt.getRestartButton().requestFocusInWindow();
            }
        });
        timer.start();
    }

}
