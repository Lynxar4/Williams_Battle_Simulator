import controller.Session;
import javax.swing.SwingUtilities;
import view.SwingGameView;

public class App {
    /* ---- FACTORY METHODS ---- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Session(new SwingGameView()).start());
    }
}