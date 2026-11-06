package view;

import java.awt.Color;
import java.awt.Font;

/* Shared colours and fonts for the battlefield and buying controls. */
final class SimulationTheme {
    /* ---- STATIC VARIABLES ---- */
    static final Color BACKGROUND = new Color(237, 241, 245);
    static final Color TEXT = new Color(38, 52, 68);
    static final Color MUTED = new Color(111, 124, 139);
    static final Color BORDER = new Color(216, 224, 232);
    static final Color TEAM_A = new Color(66, 111, 204);
    static final Color TEAM_B = new Color(206, 100, 66);
    static final Color ERROR = new Color(177, 54, 54);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    static final Font LABEL = new Font("SansSerif", Font.BOLD, 11);

    /* ---- CONSTRUCTORS ---- */
    private SimulationTheme() { }
}
