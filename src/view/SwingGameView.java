package view;

import controller.BattleSummary;
import controller.GameView;
import controller.PlacementRule;
import controller.PlayerActions;
import entity.Castle;
import entity.Team;
import entity.interactables.TileEffect;
import entity.troops.Troop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

// Implementation of the GameView API. Acts as a single entity that the states talk to and delegate work to other panels
public class SwingGameView implements GameView {
    /* ---- STATIC VARIABLES ---- */
    private static final int WINDOW_WIDTH = 1180;
    private static final int BATTLEFIELD_HEIGHT = 680;

    /* ---- PRIVATE VARIABLES ---- */
    private final JFrame frame = new JFrame("The Williams - Battle Simulator");
    private final BattlefieldPanel battlefield = new BattlefieldPanel();
    private final TroopPurchasePanel purchasePanel = new TroopPurchasePanel(battlefield);
    private final RoundOverPanel roundOverPanel;
    private PlayerActions actions; // set after Round is created

    /* ---- CONSTRUCTORS ---- */
    public SwingGameView() {
        // reads 'actions' when clicked, not now (it's still null here)
        roundOverPanel = new RoundOverPanel(() -> actions.nextRoundRequested());
        setUpWindow();
    }

    /* ---- OVERRIDES ---- */
    /* BOARD */
    @Override
    public void showBoard(int rows, int columns, Team teamA, Team teamB,
                          Castle castleA, Castle castleB, List<TileEffect> tileEffects) {
        battlefield.showBoard(rows, columns, teamA, teamB, castleA, castleB, tileEffects);
    }

    @Override
    public void refreshBoard() {
        battlefield.refresh();
    }

    /* PLACEMENT */
    @Override
    public void setPlacementEnabled(boolean isEnabled) {
        purchasePanel.setPlacementEnabled(isEnabled);  // buying controls
        battlefield.setPlacementEnabled(isEnabled);    // clicks + hover on the board
    }

    @Override
    public void showGold(int teamAGold, int teamBGold) {
        purchasePanel.showGold(teamAGold, teamBGold);
    }

    @Override
    public void setStartBattleAllowed(boolean allowed) {
        purchasePanel.setStartBattleAllowed(allowed);
    }

    @Override
    public void setPlacementRule(PlacementRule rule) {
        battlefield.setPlacementRule(rule);
    }

    @Override
    public void highlightMovingTroop(Troop troop) {
        battlefield.setMovingTroop(troop);
    }

    /* MESSAGES */
    @Override
    public void showMessage(String text, boolean isError) {
        purchasePanel.showMessage(text, isError);
    }

    @Override
    public void showLatestEvent(String text) {
        battlefield.showLatestEvent(text);
    }

    /* ROUND OVER */
    @Override
    public void showRoundResult(String title, String reason, BattleSummary summary) {
        roundOverPanel.showResult(title, reason, summary);
        roundOverPanel.setVisible(true);
        // Enter presses Next Round
        frame.getRootPane().setDefaultButton(roundOverPanel.getNextRoundButton());
        roundOverPanel.getNextRoundButton().requestFocusInWindow();
    }

    @Override
    public void hideRoundResult() {
        roundOverPanel.setVisible(false);
        frame.getRootPane().setDefaultButton(null);
    }

    /* ---- SETTER METHODS ---- */
    public void setPlayerActions(PlayerActions actions) {
        this.actions = actions;
        purchasePanel.setPlayerActions(actions);
    }

    /* ---- PRIVATE METHODS ---- */
    // Moved from Session: builds the window once.
    private void setUpWindow() {
        JPanel battlefieldView = battlefield.createView();
        battlefieldView.setPreferredSize(new Dimension(WINDOW_WIDTH, BATTLEFIELD_HEIGHT));

        JPanel content = new JPanel(new BorderLayout());
        content.add(battlefieldView, BorderLayout.CENTER);
        content.add(purchasePanel, BorderLayout.SOUTH);
        frame.setContentPane(content);

        // the result card sits on the glass pane: over the board, hidden until needed
        frame.setGlassPane(roundOverPanel);
        roundOverPanel.setVisible(false);

        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(900, screen.width), Math.min(640, screen.height)));
        frame.setSize(Math.min(frame.getWidth(), screen.width), Math.min(frame.getHeight(), screen.height));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}