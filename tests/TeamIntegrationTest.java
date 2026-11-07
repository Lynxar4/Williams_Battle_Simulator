import static testsupport.SimulationTestSupport.*;
import controller.EventLogListener;
import controller.Round;
import entity.Team;
import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.interactables.Trap;
import entity.troops.Troop;
import entity.troops.TroopType;
import entity.troops.melee.Infantry;
import entity.troops.support.Priest;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import view.BattlefieldPanel;

/* Regression checks for the buying branch combined with the team's tile effects and healing. */
public class TeamIntegrationTest {
    public static void main(String[] args) throws Exception {
        EventLogListener log = checkCombatAndTileEffects();
        checkHealingFeedback();
        SwingUtilities.invokeAndWait(() -> checkView(log));
        System.out.println("Team integration checks passed.");
    }

    private static Round placementRound() {
        Round round = newRound(new Team("teamA"), new Team("teamB"));
        round.start();
        round.update();
        return round;
    }

    private static EventLogListener checkCombatAndTileEffects() throws Exception {
        Round round = placementRound();
        EventLogListener log = new EventLogListener();
        round.addTileEffectListener(log);
        int[] left = emptyCell(round, 0);
        int[] right = emptyCell(round, round.getColumnCount() / 2);
        Infantry attacker = (Infantry) buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 2, left[0], left[1]);
        Troop enemy = buyTroop(round, Troop.Team.teamB, TroopType.INFANTRY, 1, right[0], right[1]);
        Coin coin = (Coin) round.getTileEffects().stream().filter(effect -> effect instanceof Coin).findFirst().orElseThrow();
        attacker.setPosition(coin.getRow(), coin.getColumn());
        // An adjacent opponent keeps the attacker on the coin during its turn.
        int[][] neighbours = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        boolean adjacent = false;
        for (int[] direction : neighbours) {
            int row = coin.getRow() + direction[0];
            int column = coin.getColumn() + direction[1];
            if (round.isCellFree(row, column)) {
                enemy.setPosition(row, column);
                adjacent = true;
                break;
            }
        }
        check(adjacent, "Need an empty neighbouring cell");
        int budgetA = round.getTeamA().getBudget();
        int budgetB = round.getTeamB().getBudget();
        startBattle(round);
        round.update();
        check(round.getTeamA().getBudget() == budgetA + coin.getAmount(), "Combat did not pay collected gold");
        check(round.getTeamB().getBudget() == budgetB, "Gold was paid to the wrong team");
        check(!round.getTileEffects().contains(coin), "Collected gold stayed on the board");
        check(log.getLatest().contains("collected"), "The second observer missed the coin event");
        round.applyTileEffects(attacker);
        check(round.getTeamA().getBudget() == budgetA + coin.getAmount(), "A coin paid twice");

        Hill hill = (Hill) round.getTileEffects().stream().filter(effect -> effect instanceof Hill).findFirst().orElseThrow();
        attacker.setPosition(hill.getRow(), hill.getColumn());
        round.applyTileEffects(attacker);
        check(round.damageBonusAt(attacker) == hill.getDamageBonus(), "A claimed hill lost its damage bonus");
        int castleHealth = round.getTeamBCastle().getHealth();
        attacker.setTarget(round.getTeamBCastle());
        attacker.attack(round.damageBonusAt(attacker));
        check(round.getTeamBCastle().getHealth() == castleHealth - attacker.getAttack() - hill.getDamageBonus(),
                "The hill bonus did not reach the attack");
        attacker.setPosition(coin.getRow(), coin.getColumn());
        check(round.damageBonusAt(attacker) == 0, "The hill bonus followed the troop off the hill");

        Trap trap = (Trap) round.getTileEffects().stream().filter(effect -> effect instanceof Trap).findFirst().orElseThrow();
        Infantry friendly = new Infantry(trap.getRow(), trap.getColumn(), 1, trap.getOwner());
        round.applyTileEffects(friendly);
        check(friendly.getGroupHealth() == friendly.getMaxHealth() && !trap.isConsumed(), "A friendly troop triggered its trap");
        Troop.Team opponent = trap.getOwner() == Troop.Team.teamA ? Troop.Team.teamB : Troop.Team.teamA;
        Infantry intruder = new Infantry(trap.getRow(), trap.getColumn(), 1, opponent);
        round.applyTileEffects(intruder);
        check(intruder.getGroupHealth() < intruder.getMaxHealth(), "The enemy trap did no damage");
        check(!round.getTileEffects().contains(trap) && log.getLatest().contains("trap"), "Trap event or removal was lost");
        int health = intruder.getGroupHealth();
        round.applyTileEffects(intruder);
        check(intruder.getGroupHealth() == health, "A consumed trap fired again");
        return log;
    }

    private static void checkHealingFeedback() {
        Infantry patient = new Infantry(2, 2, 1, Troop.Team.teamA);
        Priest healer = new Priest(2, 3, 1, Troop.Team.teamA);
        patient.takeDamage(40);
        int health = patient.getGroupHealth();
        healer.setTarget(patient);
        healer.heal();
        check(patient.getGroupHealth() > health, "Healing no longer restores health");
        check(patient.wasRecentlyHealed(), "The healed troop lost its animation flag");
        check(healer.hasRecentlyHealed(), "The priest lost its book-glow flag");
    }

    private static void checkView(EventLogListener log) {
        BattlefieldPanel field = new BattlefieldPanel(); // Loads every class-specific sprite.
        connectBoard(placementRound(), field);
        field.showLatestEvent(log.getLatest());
        JPanel view = field.createView();
        field.refresh();
        check(hasLabel(view, log.getLatest()), "The merged view does not show the event log");
    }

    private static boolean hasLabel(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return true;
            if (component instanceof Container child && hasLabel(child, text)) return true;
        }
        return false;
    }

    private static int[] emptyCell(Round round, int firstColumn) {
        for (int row = 0; row < round.getRowCount(); row++) {
            for (int column = firstColumn; column < firstColumn + round.getColumnCount() / 2; column++) {
                if (round.isCellFree(row, column)) return new int[] { row, column };
            }
        }
        throw new AssertionError("No empty placement tile");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
