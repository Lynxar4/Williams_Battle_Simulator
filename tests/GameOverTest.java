import static testsupport.SimulationTestSupport.*;
import controller.Round;
import controller.BattleSummary;
import controller.Session;
import controller.states.RoundOverState;
import entity.InvalidPlacementException;
import entity.Team;
import entity.interactables.Trap;
import entity.troops.Troop;
import entity.troops.TroopType;
import entity.troops.melee.Cavalry;
import entity.troops.melee.Infantry;
import entity.troops.support.Priest;
import javax.swing.SwingUtilities;
import view.RoundOverPanel;

public class GameOverTest {
    public static void main(String[] args) throws Exception {
        checkPlacementDoesNotEnd();
        checkDefeatConditions();
        checkRemainingGroupsAndDraws();
        checkBattleSummary();
        checkNoExtraTurns();
        checkTrapElimination();
        checkNextRound();
        SwingUtilities.invokeAndWait(GameOverTest::checkNextRoundButton);
        System.out.println("Round results and Next Round checks passed.");
    }

    private static Round placementRound() {
        Round round = newRound(new Team("teamA"), new Team("teamB"));
        round.start();
        round.update();
        return round;
    }

    private static Round battleRound() throws Exception {
        Round round = placementRound();
        round.getTeamA().addTroop(new Infantry(1, 1, 1, Troop.Team.teamA));
        round.getTeamB().addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
        startBattle(round);
        return round;
    }

    private static void checkPlacementDoesNotEnd() {
        Round round = placementRound();
        check(!isGameOver(round), "Empty armies during buying must not end the match");
        round.update();
        check(isPlacingTroops(round), "Buying should remain open until battle starts");
    }

    private static void checkDefeatConditions() throws Exception {
        for (boolean defeatedA : new boolean[] { true, false }) {
            for (boolean castle : new boolean[] { true, false }) {
                Round round = battleRound();
                if (castle) {
                    var target = defeatedA ? round.getTeamACastle() : round.getTeamBCastle();
                    target.takeDamage(target.getHealth());
                } else {
                    var team = defeatedA ? round.getTeamA() : round.getTeamB();
                    team.getArmy().forEach(troop -> troop.takeDamage(10_000));
                }
                round.update();
                RoundOverState result = result(round);
                check(result.getOutcome() == (defeatedA ? RoundOverState.Outcome.TEAM_B_WINS
                        : RoundOverState.Outcome.TEAM_A_WINS), "The wrong team won");
                check(result.getReason().contains(castle ? "castle" : "army"), "The wrong defeat reason was shown");
                int healthA = round.getTeamACastle().getHealth();
                int healthB = round.getTeamBCastle().getHealth();
                int row = round.getTeamA().getArmy().get(0).getRow();
                for (int tick = 0; tick < 3; tick++) round.update();
                check(round.getCurrentState() == result && round.getTeamACastle().getHealth() == healthA
                        && round.getTeamBCastle().getHealth() == healthB
                        && round.getTeamA().getArmy().get(0).getRow() == row, "The finished match kept changing");
                int budget = round.getTeamA().getBudget();
                try {
                    buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 1, 0, 0);
                    throw new AssertionError("Buying was allowed after game over");
                } catch (InvalidPlacementException expected) {
                    check(round.getTeamA().getBudget() == budget, "A finished match spent gold");
                }
            }
        }
    }

    private static void checkRemainingGroupsAndDraws() throws Exception {
        Round round = battleRound();
        Infantry survivor = new Infantry(3, 3, 1, Troop.Team.teamA);
        round.getTeamA().addTroop(survivor);
        round.getTeamA().getArmy().get(0).takeDamage(10_000);
        round.update();
        check(!isGameOver(round), "One dead group should not defeat a team with survivors");
        survivor.takeDamage(10_000);
        round.update();
        check(result(round).getOutcome() == RoundOverState.Outcome.TEAM_B_WINS, "The last group did not end the match");

        for (boolean castles : new boolean[] { true, false }) {
            Round draw = battleRound();
            if (castles) {
                draw.getTeamACastle().takeDamage(10_000);
                draw.getTeamBCastle().takeDamage(10_000);
            } else {
                draw.getTeamA().getArmy().get(0).takeDamage(10_000);
                draw.getTeamB().getArmy().get(0).takeDamage(10_000);
            }
            draw.update();
            check(result(draw).getOutcome() == RoundOverState.Outcome.DRAW, "Simultaneous defeat should be a draw");
            BattleSummary summary = result(draw).getSummary();
            check(summary.teamA().started() == 1 && summary.teamB().started() == 1
                            && summary.teamA().survived() == (castles ? 1 : 0)
                            && summary.teamB().survived() == (castles ? 1 : 0),
                    "Draws need a summary for both teams");
        }
    }

    private static void checkBattleSummary() throws Exception {
        Round round = placementRound();
        Troop infantry = buyTroop(round, Troop.Team.teamA, TroopType.INFANTRY, 3, 0, 0);
        Troop priests = buyTroop(round, Troop.Team.teamA, TroopType.PRIEST, 2, 0, 1);
        Troop removed = buyTroop(round, Troop.Team.teamA, TroopType.ARCHER, 4, 0, 2);
        removePlacedTroop(round, removed);
        Troop cavalry = buyTroop(round, Troop.Team.teamB, TroopType.CAVALRY, 4, 0, 29);
        Troop enemy = buyTroop(round, Troop.Team.teamB, TroopType.INFANTRY, 2, 1, 29);
        startBattle(round);
        infantry.takeDamage(134); // Armour reduces this to 100: two soldiers survive.
        priests.takeDamage(101); // One partially injured priest survives.
        cavalry.takeDamage(10_000);
        enemy.takeDamage(5); // Partial damage does not count as a lost soldier.
        round.getTeamACastle().takeDamage(123);
        round.getTeamBCastle().takeDamage(10_000);
        round.update();

        BattleSummary summary = result(round).getSummary();
        check(summary.teamA().equals(new BattleSummary.ArmyResult(5, 3, 877)),
                "Summary must include priests and exclude refunded groups");
        check(summary.teamA().lost() == 2, "Summary must count lost soldiers, not groups");
        check(summary.teamB().equals(new BattleSummary.ArmyResult(6, 2, 0)) && summary.teamB().lost() == 4,
                "Summary must include defeated groups and clamp destroyed castle HP to zero");

        infantry.takeDamage(10_000);
        round.getTeamB().getArmy().clear();
        round.getTeamACastle().takeDamage(10_000);
        check(summary.teamA().survived() == 3 && summary.teamA().castleHealth() == 877
                        && summary.teamB().started() == 6 && result(round).getSummary() == summary,
                "Finished results must be a fixed snapshot of the end of battle");
    }

    private static void checkNoExtraTurns() throws Exception {
        Round army = placementRound();
        army.getTeamA().addTroop(new Cavalry(0, 0, 10, Troop.Team.teamA));
        army.getTeamA().addTroop(new Infantry(15, 28, 1, Troop.Team.teamA));
        army.getTeamB().addTroop(new Infantry(0, 1, 1, Troop.Team.teamB));
        startBattle(army);
        int castleHealth = army.getTeamBCastle().getHealth();
        army.update();
        check(result(army).getOutcome() == RoundOverState.Outcome.TEAM_A_WINS, "Army defeat was missed during a turn");
        check(army.getTeamBCastle().getHealth() == castleHealth, "Another group acted after victory");

        Round castle = placementRound();
        castle.getTeamA().addTroop(new Cavalry(castle.getTeamBCastle().getRow() - 1,
                castle.getTeamBCastle().getColumn(), 50, Troop.Team.teamA));
        castle.getTeamB().addTroop(new Infantry(castle.getTeamACastle().getRow() - 1,
                castle.getTeamACastle().getColumn(), 1, Troop.Team.teamB));
        startBattle(castle);
        castleHealth = castle.getTeamACastle().getHealth();
        castle.update();
        check(result(castle).getOutcome() == RoundOverState.Outcome.TEAM_A_WINS, "Castle destruction was missed during a turn");
        check(castle.getTeamACastle().getHealth() == castleHealth, "The defeated team acted after its castle fell");
    }

    private static void checkTrapElimination() throws Exception {
        Round round = placementRound();
        Trap trap = (Trap) round.getTileEffects().stream().filter(effect -> effect instanceof Trap).findFirst().orElseThrow();
        Troop.Team loser = trap.getOwner() == Troop.Team.teamA ? Troop.Team.teamB : Troop.Team.teamA;
        Infantry intruder = new Infantry(trap.getRow(), trap.getColumn(), 1, loser);
        intruder.takeDamage(120); // Ten health remains after armour reduction.
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        Priest defender = null;
        for (int[] step : directions) {
            int row = trap.getRow() + step[0];
            int column = trap.getColumn() + step[1];
            if (round.isCellFree(row, column)) {
                defender = new Priest(row, column, 1, trap.getOwner());
                break;
            }
        }
        check(defender != null, "Need an adjacent trap defender");
        (loser == Troop.Team.teamA ? round.getTeamA() : round.getTeamB()).addTroop(intruder);
        (loser == Troop.Team.teamA ? round.getTeamB() : round.getTeamA()).addTroop(defender);
        // The defending army also needs an attacker; keep it far from the trap.
        int farRow = trap.getRow() < round.getRowCount() / 2 ? round.getRowCount() - 1 : 0;
        for (int column = 0; column < round.getColumnCount(); column++) {
            if (round.isCellFree(farRow, column)) {
                round.getOwnTeam(defender).addTroop(new Infantry(farRow, column, 1, trap.getOwner()));
                break;
            }
        }
        startBattle(round);
        round.update();
        check(isGameOver(round) && intruder.isDestroyed(), "A trap killing the last group should end the match");
        check(defender.getGroupHealth() == defender.getMaxHealth(), "The dead group attacked after triggering the trap");
    }

    private static void checkNextRound() {
        Round round = placementRound();
        Team a = round.getTeamA();
        Team b = round.getTeamB();
        a.spendBudget(200);
        for (int next = 0; next < 2; next++) {
            a.addTroop(new Infantry(1, 1, 3, Troop.Team.teamA));
            b.addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
            try {
                startBattle(round);
            } catch (InvalidPlacementException e) {
                throw new AssertionError(e);
            }
            round.getTeamACastle().takeDamage(10_000);
            round.update();
            check(isGameOver(round) && view(round).resultVisible, "The result was not shown");
            int goldA = a.getBudget();
            int goldB = b.getBudget();
            round.nextRoundRequested();
            check(isPlacingTroops(round) && !view(round).resultVisible, "Next Round should return to buying");
            check(round.getTeamA() == a && round.getTeamB() == b, "Next Round must retain the teams");
            check(a.getBudget() == goldA + 500 && b.getBudget() == goldB + 500,
                    "Next Round must carry unspent gold and add the round allowance");
            check(a.getArmy().isEmpty() && b.getArmy().isEmpty(), "Old armies survived Next Round");
            check(round.getTeamACastle().getHealth() == 1000 && round.getTeamBCastle().getHealth() == 1000,
                    "Castle health did not reset");
            check(round.getTileEffects().size() == 14, "The new battlefield was not allocated");
        }
    }

    private static void checkNextRoundButton() {
        try {
            RecordingView display = new RecordingView();
            Session session = new Session(display);
            check(display.actions instanceof Round, "Session must connect PlayerActions before starting");
            session.start();
            Round round = (Round) display.actions;
            check(round.getTeamA().getBudget() == 500 && round.getTeamB().getBudget() == 500,
                    "A new session must start with exactly $500 per team");
            round.getTeamA().addTroop(new Infantry(1, 1, 1, Troop.Team.teamA));
            round.getTeamB().addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
            startBattle(round);
            round.getTeamBCastle().takeDamage(10_000);
            round.update();
            RoundOverState result = result(round);
            RoundOverPanel prompt = new RoundOverPanel(round::nextRoundRequested);
            prompt.showResult(result.getTitle(), result.getReason(), result.getSummary());
            int gold = round.getTeamA().getBudget();
            prompt.getNextRoundButton().doClick();
            check(isPlacingTroops(round) && !display.resultVisible && round.getTeamA().getBudget() == gold + 500,
                    "Next Round button did not start a clean round with carried gold");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static RoundOverState result(Round round) {
        check(round.getCurrentState() instanceof RoundOverState, "The match should be over");
        return (RoundOverState) round.getCurrentState();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
