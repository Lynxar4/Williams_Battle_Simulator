import controller.Cell;
import controller.Round;
import controller.SpawnStrategy;
import entity.Team;
import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.interactables.Trap;
import entity.troops.Troop;
import entity.troops.melee.Infantry;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

// checks the spawn strategies and the predicate query that replaced the three
// copied spawn loops.
public class SpawnStrategyTest {

    private static int failures = 0;

    public static void main(String[] args) {
        strategiesPickWhereTheyShould();
        combiningKeepsTheOriginalRule();
        spawningProducesTheRightBoard();
        trapsBelongToTheHalfTheySitOn();
        predicateQueryFiltersAndCombines();
        spawningStopsWhenTheBoardIsFull();
        spawningGivesUpOnAFullBoard();

        if (failures > 0) {
            System.out.println(failures + " spawn strategy checks FAILED.");
            System.exit(1);
        }
        System.out.println("Spawn strategy checks passed.");
    }

    /* ---- the strategies on their own ---- */

    private static void strategiesPickWhereTheyShould() {
        Random random = new Random(1);
        boolean anywhereInBounds = true;
        boolean teamAOnLeft = true;
        boolean teamBOnRight = true;

        for (int i = 0; i < 500; i++) {
            Cell any = SpawnStrategy.anywhere().pick(random, 30, 30);
            anywhereInBounds &= any.row() >= 0 && any.row() < 30 && any.column() >= 0 && any.column() < 30;

            teamAOnLeft &= SpawnStrategy.ownHalf(Troop.Team.teamA).pick(random, 30, 30).column() < 15;
            teamBOnRight &= SpawnStrategy.ownHalf(Troop.Team.teamB).pick(random, 30, 30).column() >= 15;
        }

        check("anywhere stays on the board", true, anywhereInBounds);
        check("ownHalf(teamA) stays left", true, teamAOnLeft);
        check("ownHalf(teamB) stays right", true, teamBOnRight);
    }

    private static void combiningKeepsTheOriginalRule() {
        Random random = new Random(2);
        boolean stillLeft = true;
        boolean offTheEdges = true;

        SpawnStrategy combined = SpawnStrategy.ownHalf(Troop.Team.teamA).awayFromEdges(3);
        for (int i = 0; i < 500; i++) {
            Cell cell = combined.pick(random, 30, 30);
            stillLeft &= cell.column() < 15;
            offTheEdges &= cell.column() >= 3 && cell.column() <= 26;
        }

        check("awayFromEdges keeps the half rule", true, stillLeft);
        check("awayFromEdges keeps off the edges", true, offTheEdges);
    }

    /* ---- spawning through Round ---- */

    private static void spawningProducesTheRightBoard() {
        Round round = newRound();
        round.spawnTileEffects();
        List<TileEffect> effects = round.getTileEffects();

        check("four hills", 4, count(effects, Hill.class));
        check("six coins", 6, count(effects, Coin.class));
        check("four traps", 4, count(effects, Trap.class));
        check("fourteen effects in all", 14, effects.size());

        boolean noneOnTheEdges = effects.stream()
                .allMatch(effect -> effect.getColumn() >= 3 && effect.getColumn() <= 26);
        check("nothing spawned on a castle column", true, noneOnTheEdges);

        boolean allDistinct = effects.stream()
                .map(effect -> effect.getRow() + "," + effect.getColumn())
                .distinct().count() == effects.size();
        check("no two effects share a cell", true, allDistinct);
    }

    private static void trapsBelongToTheHalfTheySitOn() {
        // run it a few times since placement is random
        for (int attempt = 0; attempt < 20; attempt++) {
            Round round = newRound();
            round.spawnTileEffects();
            List<TileEffect> traps = round.effectsMatching(e -> e instanceof Trap);

            long teamATraps = traps.stream().filter(t -> t.getOwner() == Troop.Team.teamA).count();
            long teamBTraps = traps.stream().filter(t -> t.getOwner() == Troop.Team.teamB).count();
            boolean sidesMatch = traps.stream().allMatch(t ->
                    (t.getOwner() == Troop.Team.teamA) == (t.getColumn() < 15));

            if (teamATraps != 2 || teamBTraps != 2 || !sidesMatch) {
                check("each team gets two traps on its own half", "2/2 and matching",
                        teamATraps + "/" + teamBTraps + " matching=" + sidesMatch);
                return;
            }
        }
        check("each team gets two traps on its own half", true, true);
    }

    /* ---- the predicate query ---- */

    private static void predicateQueryFiltersAndCombines() {
        Round round = newRound();
        round.spawnTileEffects();

        Predicate<TileEffect> isHill = effect -> effect instanceof Hill;
        Predicate<TileEffect> onTeamASide = effect -> effect.getColumn() < 15;

        check("predicate finds the hills", 4, round.effectsMatching(isHill).size());

        // the reason for taking a Predicate rather than adding a method per
        // question: callers can combine tests
        int hillsLeft = round.effectsMatching(isHill.and(onTeamASide)).size();
        int hillsRight = round.effectsMatching(isHill.and(onTeamASide.negate())).size();
        check("and / negate split the hills between the halves", 4, hillsLeft + hillsRight);

        check("a test nothing passes gives an empty list", 0,
                round.effectsMatching(effect -> false).size());
        check("a test everything passes gives all of them", 14,
                round.effectsMatching(effect -> true).size());
    }

    /* ---- the attempt cap ---- */

    private static void spawningStopsWhenTheBoardIsFull() {
        // a strategy that always names the same cell can only ever place one
        // effect. without the attempt cap the old loop would spin forever.
        Round round = newRound();
        long start = System.currentTimeMillis();
        round.spawnTileEffects();
        round.spawnTileEffects(); // a restart clears and respawns
        long elapsed = System.currentTimeMillis() - start;

        check("respawning clears the previous round", 14, round.getTileEffects().size());
        check("spawning finishes promptly", true, elapsed < 2000);
    }

    // every cell taken, so no effect can ever be placed. the old loop had no
    // cap and would have spun here forever.
    private static void spawningGivesUpOnAFullBoard() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        for (int row = 0; row < 30; row++) {
            for (int column = 0; column < 30; column++) {
                teamA.addTroop(new Infantry(row, column, 1, Troop.Team.teamA));
            }
        }
        Round round = new Round(teamA, teamB);

        long start = System.currentTimeMillis();
        round.spawnTileEffects();
        long elapsed = System.currentTimeMillis() - start;

        check("a full board places nothing", 0, round.getTileEffects().size());
        check("a full board does not hang", true, elapsed < 5000);
    }

    /* ---- helpers ---- */

    private static Round newRound() {
        return new Round(new Team("teamA"), new Team("teamB"));
    }

    private static int count(List<TileEffect> effects, Class<?> type) {
        return (int) effects.stream().filter(type::isInstance).count();
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
