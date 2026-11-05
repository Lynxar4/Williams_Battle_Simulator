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
import view.GameOverPanel;

public class GameOverTest {
    public static void main(String[] args) throws Exception {
        checkPlacementDoesNotEnd();
        checkDefeatConditions();
        checkRemainingGroupsAndDraws();
        checkBattleSummary();
        checkNoExtraTurns();
        checkTrapElimination();
        checkRestart();
        SwingUtilities.invokeAndWait(GameOverTest::checkRestartButton);
        System.out.println("Game over and restart checks passed.");
    }

    private static Round placementRound() {
        Round round = new Round(new Team("teamA"), new Team("teamB"));
        round.start();
        round.update();
        return round;
    }

    private static Round battleRound() throws Exception {
        Round round = placementRound();
        round.getTeamA().addTroop(new Infantry(1, 1, 1, Troop.Team.teamA));
        round.getTeamB().addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
        round.startBattle();
        return round;
    }

    private static void checkPlacementDoesNotEnd() {
        Round round = placementRound();
        check(!round.checkGameOver(), "Empty armies during buying must not end the match");
        round.update();
        check(round.isPlacingTroops(), "Buying should remain open until battle starts");
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
                    round.buyTroop(Troop.Team.teamA, TroopType.INFANTRY, 1, 0, 0);
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
        check(!round.isGameOver(), "One dead group should not defeat a team with survivors");
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
        Troop infantry = round.buyTroop(Troop.Team.teamA, TroopType.INFANTRY, 3, 0, 0);
        Troop priests = round.buyTroop(Troop.Team.teamA, TroopType.PRIEST, 2, 0, 1);
        Troop removed = round.buyTroop(Troop.Team.teamA, TroopType.ARCHER, 4, 0, 2);
        round.removePlacedTroop(removed);
        Troop cavalry = round.buyTroop(Troop.Team.teamB, TroopType.CAVALRY, 4, 0, 29);
        Troop enemy = round.buyTroop(Troop.Team.teamB, TroopType.INFANTRY, 2, 1, 29);
        round.startBattle();
        infantry.takeDamage(134); // Armour reduces this to 100: two soldiers survive.
        priests.takeDamage(101); // One partially injured priest survives.
        cavalry.takeDamage(10_000);
        enemy.takeDamage(5); // Partial damage does not count as a lost soldier.
        round.getTeamACastle().takeDamage(123);
        round.getTeamBCastle().takeDamage(10_000);
        round.checkGameOver();

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
        army.startBattle();
        int castleHealth = army.getTeamBCastle().getHealth();
        army.update();
        check(result(army).getOutcome() == RoundOverState.Outcome.TEAM_A_WINS, "Army defeat was missed during a turn");
        check(army.getTeamBCastle().getHealth() == castleHealth, "Another group acted after victory");

        Round castle = placementRound();
        castle.getTeamA().addTroop(new Cavalry(castle.getTeamBCastle().getRow() - 1,
                castle.getTeamBCastle().getColumn(), 50, Troop.Team.teamA));
        castle.getTeamB().addTroop(new Infantry(castle.getTeamACastle().getRow() - 1,
                castle.getTeamACastle().getColumn(), 1, Troop.Team.teamB));
        castle.startBattle();
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
        round.startBattle();
        round.update();
        check(round.isGameOver() && intruder.isDestroyed(), "A trap killing the last group should end the match");
        check(defender.getGroupHealth() == defender.getMaxHealth(), "The dead group attacked after triggering the trap");
    }

    private static void checkRestart() {
        Session session = new Session();
        Round old = session.getRound();
        old.getTeamA().spendBudget(200);
        old.getTeamA().addTroop(new Infantry(1, 1, 3, Troop.Team.teamA));
        old.getTeamB().addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
        old.getTeamACastle().takeDamage(10_000);
        for (int restart = 0; restart < 2; restart++) {
            session.restart();
            Round fresh = session.getRound();
            check(fresh != old && fresh.getTeamA() != old.getTeamA(), "Restart reused the old match");
            check(fresh.isPlacingTroops() && !fresh.isGameOver(), "Restart should return to troop buying");
            check(fresh.getTeamA().getBudget() == 500 && fresh.getTeamB().getBudget() == 500, "Budgets did not reset");
            check(fresh.getTeamA().getArmy().isEmpty() && fresh.getTeamB().getArmy().isEmpty(), "Old armies survived restart");
            check(fresh.getTeamACastle().getHealth() == 1000 && fresh.getTeamBCastle().getHealth() == 1000,
                    "Castles did not reset");
            check(fresh.getTileEffects().size() == 14, "The fresh battlefield was not allocated");
            old = fresh;
        }
    }

    private static void checkRestartButton() {
        try {
            Session session = new Session();
            Round round = session.getRound();
            round.getTeamA().addTroop(new Infantry(1, 1, 1, Troop.Team.teamA));
            round.getTeamB().addTroop(new Infantry(1, 28, 1, Troop.Team.teamB));
            round.startBattle();
            round.getTeamBCastle().takeDamage(10_000);
            round.update();
            GameOverPanel prompt = new GameOverPanel(result(round), session::restart);
            prompt.getRestartButton().doClick();
            check(session.getRound() != round && session.getRound().isPlacingTroops(), "Restart button did not reset the match");
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
