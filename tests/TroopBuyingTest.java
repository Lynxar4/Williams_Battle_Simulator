import static testsupport.SimulationTestSupport.*;
import controller.Round;
import entity.InvalidPlacementException;
import entity.Team;
import entity.troops.Troop;
import entity.troops.TroopType;
import entity.troops.melee.Infantry;
import entity.troops.support.Priest;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import view.BattlefieldPanel;

/* Run with the standard JDK; no test libraries are needed. */
public class TroopBuyingTest {
    public static void main(String[] args) throws Exception {
        checkPurchases();
        checkRejections();
        checkBattleTransition();
        checkPriestOnlyArmies();
        checkPriestHealing();
        SwingUtilities.invokeAndWait(TroopBuyingTest::checkResizedPlacement);
        SwingUtilities.invokeAndWait(TroopBuyingTest::checkScrollableBoard);
        System.out.println("Troop buying checks passed.");
    }

    private static Round placementRound() {
        Round round = newRound(new Team("teamA"), new Team("teamB"));
        round.start();
        round.update();
        return round;
    }

    private static void checkPurchases() throws Exception {
        Round round = placementRound();
        check(round.getRowCount() == 30 && round.getColumnCount() == 30, "Expected a 30 x 30 battlefield");
        for (TroopType type : TroopType.values()) {
            int[] cell = emptyCell(round, Troop.Team.teamA);
            Troop troop = buyTroop(round, Troop.Team.teamA, type, 2, cell[0], cell[1]);
            check(troop.getClass().getSimpleName().equals(type.getDisplayName()), "Wrong troop type");
            check(troop.getInitialGroupSize() == 2, "Soldier count was not preserved");
            check(troop.isAt(cell[0], cell[1]), "Group was not placed on the chosen tile");
        }
        check(round.getTeamA().getBudget() == 370, "Four purchases should cost $130");
        check(round.getTeamA().getArmy().size() == 4, "Each purchase should create one group");
        check(round.getTeamB().getBudget() == 500, "Purchases charged the other team");

        int[] cell = emptyCell(round, Troop.Team.teamB);
        Troop troop = buyTroop(round, Troop.Team.teamB, TroopType.CAVALRY, 20, cell[0], cell[1]);
        check(troop.getTeam() == Troop.Team.teamB, "Wrong group owner");
        check(round.getTeamB().getBudget() == 0, "An exact-budget purchase should succeed");
    }

    private static void checkRejections() throws Exception {
        Round round = placementRound();
        int[] cell = emptyCell(round, Troop.Team.teamA);
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 0, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, -1, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 51, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.CAVALRY, Integer.MAX_VALUE, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, null, TroopType.INFANTRY, 1, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, null, 1, cell[0], cell[1]));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, -1, 0));
        int half = round.getColumnCount() / 2;
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, 0, round.getColumnCount()));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, round.getRowCount(), 0));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, 0, half));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamB, TroopType.INFANTRY, 1, 0, half - 1));
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1,
                round.getTeamACastle().getRow(), round.getTeamACastle().getColumn()));
        var effect = round.getTileEffects().get(0);
        Troop.Team owner = effect.getColumn() < half ? Troop.Team.teamA : Troop.Team.teamB;
        rejectsWithoutCharge(round, () -> buyTroop(round, owner, TroopType.INFANTRY, 1, effect.getRow(), effect.getColumn()));

        buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, cell[0], cell[1]);
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, cell[0], cell[1]));
    }

    private static void checkBattleTransition() throws Exception {
        Round round = placementRound();
        for (int tick = 0; tick < 5; tick++) {
            round.update();
        }
        check(isPlacingTroops(round), "Placement should wait for player input");
        rejectsWithoutCharge(round, () -> startBattle(round));
        int[] left = emptyCell(round, Troop.Team.teamA);
        buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, left[0], left[1]);
        rejectsWithoutCharge(round, () -> startBattle(round));
        int[] right = emptyCell(round, Troop.Team.teamB);
        buyTroop(round, Troop.Team.teamB, TroopType.ARCHER, 1, right[0], right[1]);
        check(view(round).startAllowed, "Both teams are ready");
        startBattle(round);
        check(!isPlacingTroops(round), "Start Battle did not change phase");
        rejectsWithoutCharge(round, () -> buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, 0, 0));
    }

    private static void checkPriestOnlyArmies() throws Exception {
        for (TroopType attackerType : new TroopType[] {
                TroopType.INFANTRY, TroopType.ARCHER, TroopType.CAVALRY }) {
            Round round = placementRound();
            for (Troop.Team owner : Troop.Team.values()) {
                int[] cell = emptyCell(round, owner);
                buyTroop(round, owner, TroopType.PRIEST, 3, cell[0], cell[1]);
            }
            check(!view(round).startAllowed, "Priest-only armies must not be ready");
            rejectsWithoutCharge(round, () -> startBattle(round));
            check(isPlacingTroops(round), "Rejected start must stay in placement");

            int[] left = emptyCell(round, Troop.Team.teamA);
            Troop attacker = buyTroop(round, Troop.Team.teamA, attackerType, 1, left[0], left[1]);
            check(!view(round).startAllowed, "Team B still needs an attacker");
            rejectsWithoutCharge(round, () -> startBattle(round));

            int[] right = emptyCell(round, Troop.Team.teamB);
            buyTroop(round, Troop.Team.teamB, attackerType, 1, right[0], right[1]);
            check(view(round).startAllowed, "Mixed armies with attackers should be ready");

            attacker.takeDamage(10_000);
            rejectsWithoutCharge(round, () -> startBattle(round));

            left = emptyCell(round, Troop.Team.teamA);
            buyTroop(round, Troop.Team.teamA, attackerType, 1, left[0], left[1]);
            startBattle(round);
            check(!isPlacingTroops(round), "Both teams with living attackers should start");
        }
    }

    private static void checkPriestHealing() throws Exception {
        Round round = placementRound();
        Infantry ally = new Infantry(0, 0, 1, Troop.Team.teamA);
        ally.takeDamage(40);
        int before = ally.getGroupHealth();
        round.getTeamA().addTroop(ally);
        round.getTeamA().addTroop(new Priest(0, 1, 1, Troop.Team.teamA));
        round.getTeamB().addTroop(new Infantry(round.getRowCount() - 1, round.getColumnCount() - 1, 1, Troop.Team.teamB));
        startBattle(round);
        round.update();
        check(ally.getGroupHealth() > before, "A priest should heal an adjacent injured ally");
    }

    private static int[] emptyCell(Round round, Troop.Team team) {
        for (int row = 0; row < round.getRowCount(); row++) {
            int half = round.getColumnCount() / 2;
            int firstColumn = team == Troop.Team.teamA ? 0 : half;
            for (int column = firstColumn; column < firstColumn + half; column++) {
                if (round.isCellFree(row, column)) {
                    return new int[] { row, column };
                }
            }
        }
        throw new AssertionError("No empty placement tile");
    }

    private static void checkResizedPlacement() {
        Round round = placementRound();
        BattlefieldPanel field = new BattlefieldPanel();
        connectBoard(round, field);
        field.setPlacementHandler((row, column) -> {
            try {
                buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, row, column);
            } catch (InvalidPlacementException expected) {
                // Outside-grid clicks should be rejected without spending gold.
            }
        });
        int previousWidth = 0;
        int column = round.getColumnCount() / 2 - 1;
        for (Dimension size : new Dimension[] {
                new Dimension(900, 500), new Dimension(1180, 680), new Dimension(1920, 920) }) {
            field.setSize(size);
            Rectangle board = field.getGridBounds();
            check(board.width > previousWidth, "The board should grow with the window");
            check(board.x >= 0 && board.y >= 0
                    && board.x + board.width < size.width && board.y + board.height < size.height,
                    "The board should fit inside the window");
            previousWidth = board.width;
            int row = round.getRowCount() - 1;
            while (!round.isCellFree(row, column)) {
                row--;
            }
            Rectangle tile = field.getTileBounds(row, column);
            int x = tile.x + tile.width / 2;
            int y = tile.y + tile.height / 2;
            int previousGroups = round.getTeamA().getArmy().size();
            click(field, x, y);
            check(round.getTeamA().getArmy().size() == previousGroups + 1, "Resized click did not buy a group");
            Troop bought = round.getTeamA().getArmy().get(previousGroups);
            check(bought.isAt(row, column), "Resized click bought a group on the wrong tile");
            check(field.getToolTipText(new MouseEvent(field, MouseEvent.MOUSE_MOVED, 0, 0,
                    x, y, 0, false)).contains("Infantry"), "Resized hover found the wrong tile");

            int budget = round.getTeamA().getBudget();
            click(field, board.x - 1, y);
            click(field, x, board.y - 1);
            click(field, board.x + board.width, y);
            click(field, x, board.y + board.height);
            check(round.getTeamA().getBudget() == budget
                    && round.getTeamA().getArmy().size() == previousGroups + 1,
                    "A click outside the resized board changed purchases");
        }
    }

    private static void click(BattlefieldPanel field, int x, int y) {
        field.dispatchEvent(new MouseEvent(field, MouseEvent.MOUSE_CLICKED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static void checkScrollableBoard() {
        Round round = placementRound();
        BattlefieldPanel field = new BattlefieldPanel();
        connectBoard(round, field);
        JPanel view = field.createView();
        view.setSize(1180, 680);
        layout(view);
        JScrollPane scroll = null;
        for (Component component : view.getComponents()) {
            if (component instanceof JScrollPane pane) {
                scroll = pane;
            }
        }
        check(scroll != null, "A smaller window needs a scrollable board");
        Rectangle tile = field.getTileBounds(0, 0);
        check(tile.width >= 30 && tile.height >= 30, "Normal view should keep tiles readable");
        check(scroll.getHorizontalScrollBar().isVisible() && scroll.getVerticalScrollBar().isVisible(),
                "The full board should be reachable by scrolling");
        scroll.getHorizontalScrollBar().setValue(scroll.getHorizontalScrollBar().getMaximum());
        scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        // Pick a free cell at the far end of the map, including the last row and column.
        int row = round.getRowCount() - 1;
        int column = round.getColumnCount() - 1;
        while (!round.isCellFree(row, column)) row--;
        Rectangle last = field.getTileBounds(row, column);
        check(scroll.getViewport().getViewRect().contains(last), "Scrolling should reveal the last tiles");
        field.setPlacementHandler((r, c) -> {
            try {
                buyTroop(round, Troop.Team.teamB, TroopType.INFANTRY, 1, r, c);
            } catch (InvalidPlacementException e) {
                throw new AssertionError(e.getMessage());
            }
        });
        click(field, last.x + last.width / 2, last.y + last.height / 2);
        check(round.getTeamB().getArmy().get(0).isAt(row, column), "Scrolled placement used the wrong tile");

        field.setFitWholeBoard(true);
        layout(view);
        check(!scroll.getHorizontalScrollBar().isVisible() && !scroll.getVerticalScrollBar().isVisible(),
                "Fit whole board should show every tile without scrolling");
        check(scroll.getViewport().getViewRect().contains(field.getGridBounds()), "Overview cropped the board");
        field.setFitWholeBoard(false);
        layout(view);
        check(field.getTileBounds(0, 0).height >= 30, "Leaving overview should restore larger tiles");
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component component : parent.getComponents()) {
            if (component instanceof Container child) layout(child);
        }
    }

    private static void rejectsWithoutCharge(Round round, RejectedAction action) throws Exception {
        int goldA = round.getTeamA().getBudget();
        int goldB = round.getTeamB().getBudget();
        int sizeA = round.getTeamA().getArmy().size();
        int sizeB = round.getTeamB().getArmy().size();
        try {
            action.run();
            throw new AssertionError("Invalid action was accepted");
        } catch (InvalidPlacementException expected) {
            check(round.getTeamA().getBudget() == goldA && round.getTeamB().getBudget() == goldB,
                    "Rejected action changed a budget");
            check(round.getTeamA().getArmy().size() == sizeA && round.getTeamB().getArmy().size() == sizeB,
                    "Rejected action changed an army");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface RejectedAction {
        void run() throws InvalidPlacementException;
    }
}
