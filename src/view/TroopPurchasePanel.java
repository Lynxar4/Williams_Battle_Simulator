package view;

import controller.PlayerActions;
import entity.troops.Troop;
import entity.troops.TroopType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TroopPurchasePanel extends JPanel {
    private PlayerActions actions;   // set after Round is created
    private int teamAGold;
    private int teamBGold;
    private boolean placementEnabled;
    private final BattlefieldPanel battlefield;
    private final JComboBox<String> teamSelector = new JComboBox<>(new String[] { "Team A", "Team B" });
    private final JComboBox<TroopType> troopSelector = new JComboBox<>(TroopType.values());
    private final JSpinner groupSize = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JLabel quote = new JLabel();
    private final JLabel message = new JLabel(
            "Click an empty tile to buy a group. Right-click a group to move or remove it.");
    private final JButton startBattle = new JButton("Start Battle");
    private final JFormattedTextField sizeInput;

    /* ---- CONSTRUCTOR ---- */
    public TroopPurchasePanel(BattlefieldPanel battlefield) {
        this.battlefield = battlefield;
        setLayout(new BorderLayout(0, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, SimulationTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(groupSize, "0");
        groupSize.setEditor(editor);
        sizeInput = editor.getTextField();
        sizeInput.setColumns(4);
        // Keep invalid input visible so a rejected purchase can explain the error.
        sizeInput.setFocusLostBehavior(JFormattedTextField.PERSIST);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        teamSelector.setPreferredSize(new Dimension(90, 34));
        troopSelector.setPreferredSize(new Dimension(190, 34));
        groupSize.setPreferredSize(new Dimension(76, 34));
        quote.setPreferredSize(new Dimension(205, 34));
        startBattle.setPreferredSize(new Dimension(142, 34));
        startBattle.setBackground(SimulationTheme.TEAM_A);
        startBattle.setForeground(Color.WHITE);
        startBattle.setFocusPainted(false);
        startBattle.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        addInput(controls, "TEAM", teamSelector, 0, 1);
        addInput(controls, "TROOP TYPE", troopSelector, 1, 1.5);
        addInput(controls, "GROUP SIZE", groupSize, 2, 0.7);
        addInput(controls, "PURCHASE TOTAL", quote, 3, 1.5);
        addInput(controls, " ", startBattle, 4, 0);
        add(controls, BorderLayout.CENTER);
        message.setHorizontalAlignment(JLabel.CENTER);
        message.setFont(new Font("SansSerif", Font.PLAIN, 12));
        message.setForeground(SimulationTheme.MUTED);
        JCheckBox fitBoard = new JCheckBox("Fit whole board");
        fitBoard.setOpaque(false);
        fitBoard.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fitBoard.setForeground(SimulationTheme.TEXT);
        fitBoard.setToolTipText("Show every cell. Uncheck for larger tiles with scrolling.");
        fitBoard.addActionListener(e -> battlefield.setFitWholeBoard(fitBoard.isSelected()));
        JPanel feedback = new JPanel(new BorderLayout(12, 0));
        feedback.setOpaque(false);
        feedback.add(fitBoard, BorderLayout.WEST);
        feedback.add(message, BorderLayout.CENTER);
        add(feedback, BorderLayout.SOUTH);

        teamSelector.addActionListener(e -> {
            actions.cancelMove();
            battlefield.setPlacementTeam(selectedTeam());
            refreshQuote();
        });
        troopSelector.addActionListener(e -> {
            actions.cancelMove();
            refreshQuote();
        });
        sizeInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshQuote(); }

            @Override
            public void removeUpdate(DocumentEvent e) { refreshQuote(); }

            @Override
            public void changedUpdate(DocumentEvent e) { refreshQuote(); }
        });
        startBattle.addActionListener(e -> actions.startBattleRequested());
        battlefield.setPlacementHandler(this::onTileClicked);
        battlefield.setTroopMenuHandler(this::showTroopMenu);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancelMove");
        getActionMap().put("cancelMove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actions.cancelMove();
            }
        });
        refreshQuote();
    }

    /* ---- PRIVATE METHODS ---- */
    private void addInput(JPanel controls, String text, javax.swing.JComponent input, int column, double weight) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setLabelFor(input);
        label.setFont(SimulationTheme.LABEL);
        label.setForeground(SimulationTheme.MUTED);
        input.setFont(SimulationTheme.BODY);
        field.add(label, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.weightx = weight;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, column == 4 ? 0 : 14);
        controls.add(field, constraints);
    }

    private Troop.Team selectedTeam() {
        return teamSelector.getSelectedIndex() == 0 ? Troop.Team.teamA : Troop.Team.teamB;
    }

    private TroopType selectedType() {
        return (TroopType) troopSelector.getSelectedItem();
    }

    private int enteredGroupSize() {
        try {
            return Integer.parseInt(sizeInput.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void onTileClicked(int row, int column) {
        actions.tileClicked(selectedTeam(), selectedType(), enteredGroupSize(), row, column);
    }

    private void showTroopMenu(Troop troop, MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem move = new JMenuItem("Move group");
        move.addActionListener(e -> {
            teamSelector.setSelectedIndex(troop.getTeam() == Troop.Team.teamA ? 0 : 1); // FIRST
            actions.moveTroopRequested(troop);
        });
        JMenuItem remove = new JMenuItem("Remove group (full refund)");
        remove.addActionListener(e -> actions.removeTroopRequested(troop));
        menu.add(move);
        menu.add(remove);
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    private void refreshQuote() {
        if (!placementEnabled) {
            quote.setText("Buying closed");
            quote.setForeground(SimulationTheme.TEXT);
            return;
        }
        int gold = selectedTeam() == Troop.Team.teamA ? teamAGold : teamBGold;
        try {
            int cost = selectedType().getTotalCost(enteredGroupSize());
            quote.setText("Total: $" + cost + " | Gold: $" + gold);
            quote.setForeground(cost > gold ? SimulationTheme.ERROR : SimulationTheme.TEXT);
        } catch (IllegalArgumentException e) {
            quote.setText("Enter a valid group size");
            quote.setForeground(SimulationTheme.ERROR);
        }
    }


    /* ---- PUBLIC METHODS ---- */
    public void showMessage(String text, boolean error) {
        message.setText(text);
        message.setForeground(error ? SimulationTheme.ERROR : SimulationTheme.TEXT);
    }

    public void setPlayerActions(PlayerActions actions) {
        this.actions = actions;
    }

    public void setPlacementEnabled(boolean enabled) {
        placementEnabled = enabled;
        teamSelector.setEnabled(enabled);
        troopSelector.setEnabled(enabled);
        groupSize.setEnabled(enabled);
        startBattle.setEnabled(enabled);
        refreshQuote();
    }

    public void showGold(int teamAGold, int teamBGold) {
        this.teamAGold = teamAGold;
        this.teamBGold = teamBGold;
        refreshQuote();
    }

    public void setStartBattleAllowed(boolean allowed) {
        startBattle.setToolTipText(allowed ? null
                : "Each team needs an Infantry, Archer or Cavalry group. Priests only heal.");
    }
}
