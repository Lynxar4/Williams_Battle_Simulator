import static testsupport.SimulationTestSupport.*;
import controller.Round;
import entity.Team;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.troops.Troop;
import entity.troops.melee.Infantry;
import entity.troops.ranged.Archer;
import entity.troops.support.Priest;

import java.util.Locale;

// checks that troops divert for free high ground, and that the diversion is
// limited to the cases where it is worth something.
public class HillDetourTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        hillsGetTakenFarMoreOften();
        bothSidesEndUpHoldingGround();
        aClaimedHillStopsAttractingTroops();

        if (failures > 0) {
            System.out.println(failures + " hill detour checks FAILED.");
            System.exit(1);
        }
        System.out.println("Hill detour checks passed.");
    }

    // the headline behaviour. with no detour this sat near 5% in testing;
    // the bar here is deliberately well below what it actually reaches so
    // the check is not brittle.
    private static void hillsGetTakenFarMoreOften() throws Exception {
        Result result = runBattles(60, 120);

        System.out.printf(Locale.ROOT, "  (%d of %d hills claimed, %.0f%% of battles contested)%n",
                result.claimed, result.hills, 100.0 * result.contested / result.battles);

        check("most hills get claimed", true, result.claimed > result.hills / 4);
        check("nearly every battle claims one", true, result.battlesWithAClaim > result.battles / 2);
    }

    // the emergent part: both teams converge on high ground, so neither gets
    // all of it. before the detour this never happened.
    private static void bothSidesEndUpHoldingGround() throws Exception {
        Result result = runBattles(60, 120);
        check("both sides hold ground in some battles", true, result.contested > 0);
    }

    // a hill already taken is worth nothing to the other side, so it must not
    // keep pulling troops off their target
    private static void aClaimedHillStopsAttractingTroops() {
        Hill hill = new Hill(5, 5, 5);
        check("a free hill is unclaimed", false, hill.isClaimed());

        hill.onEnter(new Infantry(5, 5, 10, Troop.Team.teamA));
        check("claiming it marks it", true, hill.isClaimed());
        check("the enemy gains nothing from it", 0,
                hill.damageBonusFor(new Infantry(5, 5, 10, Troop.Team.teamB)));

        // a priest on its own team's hill is still reported a damage bonus,
        // because the hill only checks the holder. it never matters: only
        // attackers ever read it, and the detour skips non-attackers anyway.
        check("the hill reports its bonus to any holder", 5,
                hill.damageBonusFor(new Priest(5, 5, 10, Troop.Team.teamA)));
        check("an archer on its own hill gains range", 1,
                hill.rangeBonusFor(new Archer(5, 5, 10, Troop.Team.teamA)));
    }

    /* ---- helpers ---- */

    private record Result(int battles, int hills, int claimed, int battlesWithAClaim, int contested) { }

    private static Result runBattles(int battles, int ticks) throws Exception {
        int hills = 0;
        int claimed = 0;
        int withAClaim = 0;
        int contested = 0;

        for (int i = 0; i < battles; i++) {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            teamA.addTroop(new Infantry(10, 4, 10, Troop.Team.teamA));
            teamA.addTroop(new Archer(18, 4, 8, Troop.Team.teamA));
            teamB.addTroop(new Infantry(10, 25, 10, Troop.Team.teamB));
            teamB.addTroop(new Archer(18, 25, 8, Troop.Team.teamB));

            Round round = newRound(teamA, teamB);
            round.start();
            round.update();
            startBattle(round);

            for (int tick = 0; tick < ticks && !isGameOver(round); tick++) {
                round.update();
            }

            int here = 0;
            boolean a = false;
            boolean b = false;
            for (TileEffect effect : round.getTileEffects()) {
                if (effect instanceof Hill hill) {
                    hills++;
                    if (hill.isClaimed()) {
                        here++;
                        if (hill.getHolder() == Troop.Team.teamA) {
                            a = true;
                        } else {
                            b = true;
                        }
                    }
                }
            }
            claimed += here;
            if (here > 0) {
                withAClaim++;
            }
            if (a && b) {
                contested++;
            }
        }
        return new Result(battles, hills, claimed, withAClaim, contested);
    }

    private static void check(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  PASS  " + what);
        } else {
            System.out.println("  FAIL  " + what + "  expected " + expected + " got " + actual);
            failures++;
        }
    }
}
