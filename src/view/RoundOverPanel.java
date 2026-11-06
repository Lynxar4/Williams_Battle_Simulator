package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* A glass-pane prompt keeps the final battlefield visible while blocking placement. */
public class RoundOverPanel extends JPanel {
    private final JButton nextRound = new JButton("Next Round");
    private final JLabel winner = new JLabel();
    private final JLabel reason = new JLabel();

    public RoundOverPanel(Runnable nextRoundAction) {
        super(new GridBagLayout());
        setOpaque(false);
        setFocusCycleRoot(true);
        addMouseListener(new MouseAdapter() { });

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SimulationTheme.BORDER),
                BorderFactory.createEmptyBorder(26, 32, 26, 32)));
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.insets = new Insets(0, 0, 12, 0);

        JLabel heading = new JLabel("Round over");
        heading.setFont(new Font("SansSerif", Font.PLAIN, 15));
        heading.setForeground(SimulationTheme.MUTED);
        row.gridy = 0;
        card.add(heading, row);

        winner.setFont(new Font("SansSerif", Font.BOLD, 28));
        winner.setForeground(SimulationTheme.TEXT);
        row.gridy = 1;
        card.add(winner, row);

        reason.setFont(SimulationTheme.BODY);
        reason.setForeground(SimulationTheme.TEXT);
        row.gridy = 2;
        card.add(reason, row);

        nextRound.setFont(new Font("SansSerif", Font.BOLD, 14));
        nextRound.setBackground(SimulationTheme.TEAM_A);
        nextRound.setForeground(Color.WHITE);
        nextRound.setFocusPainted(false);
        nextRound.setBorder(BorderFactory.createEmptyBorder(10, 32, 10, 32));
        nextRound.addActionListener(e -> nextRoundAction.run());
        row.gridy = 3;
        row.insets = new Insets(8, 0, 0, 0);
        card.add(nextRound, row);
        add(card);
    }

    public JButton getNextRoundButton() {
        return nextRound;
    }
    public void showResult(String title, String reasonText) {
        winner.setText(title);
        reason.setText(reasonText);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
