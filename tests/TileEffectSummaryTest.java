import controller.Round;
import controller.TileEffectSummary;
import controller.TileEffectSummaryCollector;
import entity.Team;
import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.interactables.Trap;
import entity.troops.Troop;
import entity.troops.melee.Infantry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

// checks the custom collector that counts what is still on the board, and
// that running it in parallel gives the same answer.
public class TileEffectSummaryTest {

    private static int failures = 0;

    public static void main(String[] args) {
        countsEachKind();
        hillsSplitByHolder();
        consumedEffectsAreNotCounted();
        emptyBoardGivesZeroes();
        parallelMatchesSequential();
        mergeIsAssociative();
        roundReportsItsOwnBoard();

        if (failures > 0) {
            System.out.println(failures + " tile effect summary checks FAILED.");
            System.exit(1);
        }
        System.out.println("Tile effect summary checks passed.");
    }

    /* ---- counting ---- */

    private static void countsEachKind() {
        TileEffectSummary summary = collect(List.of(
                new Hill(0, 0, 5),
                new Coin(0, 1, 25),
                new Coin(0, 2, 25),
                new Trap(0, 3, 20, Troop.Team.teamA),
                new Trap(0, 4, 20, Troop.Team.teamB)));

        check("unclaimed hills", 1, summary.hillsUnclaimed());
        check("coins still out", 2, summary.coinsOnField());
        check("gold still out", 50, summary.goldOnField());
        check("team A traps live", 1, summary.trapsArmedA());
        check("team B traps live", 1, summary.trapsArmedB());
    }

    private static void hillsSplitByHolder() {
        Hill takenByA = new Hill(0, 0, 5);
        takenByA.onEnter(new Infantry(0, 0, 10, Troop.Team.teamA));
        Hill takenByB = new Hill(0, 1, 5);
        takenByB.onEnter(new Infantry(0, 1, 10, Troop.Team.teamB));

        TileEffectSummary summary = collect(List.of(takenByA, takenByB, new Hill(0, 2, 5)));

        check("hills held by A", 1, summary.hillsHeldA());
        check("hills held by B", 1, summary.hillsHeldB());
        check("hills still free", 1, summary.hillsUnclaimed());
        check("hills add up", 3, summary.hillsTotal());
        check("lookup by team works", 1, summary.hillsHeldBy(Troop.Team.teamB));
    }

    // a collected coin and a sprung trap leave the board, so the census only
    // ever sees what is left. this records that rather than assuming it.
    private static void consumedEffectsAreNotCounted() {
        Coin coin = new Coin(0, 0, 25);
        coin.onEnter(new Infantry(0, 0, 10, Troop.Team.teamA));
        Trap trap = new Trap(0, 1, 20, Troop.Team.teamA);
        trap.onEnter(new Infantry(0, 1, 10, Troop.Team.teamB));

        check("a spent coin is consumed", true, coin.isConsumed());
        check("a sprung trap is consumed", true, trap.isConsumed());

        // what the board would hold after removeIf(TileEffect::isConsumed)
        List<TileEffect> left = new ArrayList<>(List.of(coin, trap, new Hill(0, 2, 5)));
        left.removeIf(TileEffect::isConsumed);
        TileEffectSummary summary = collect(left);

        check("nothing spent is counted", 0, summary.coinsOnField() + summary.trapsArmedA());
        check("what remains still is", 1, summary.hillsUnclaimed());
    }

    private static void emptyBoardGivesZeroes() {
        TileEffectSummary summary = collect(List.of());
        check("empty board, no hills", 0, summary.hillsTotal());
        check("empty board, no gold", 0, summary.goldOnField());
        check("empty() matches an empty collect", TileEffectSummary.empty(), summary);
    }

    /* ---- parallel safety ---- */

    // the collector is only worth running in parallel if it gives the same
    // answer, so this builds a board far bigger than a real one and compares.
    private static void parallelMatchesSequential() {
        List<TileEffect> big = IntStream.range(0, 60_000)
                .mapToObj(TileEffectSummaryTest::effectFor)
                .toList();

        TileEffectSummary sequential = big.stream().collect(new TileEffectSummaryCollector());
        TileEffectSummary parallel = big.parallelStream().collect(new TileEffectSummaryCollector());

        check("parallel equals sequential on 60k effects", sequential, parallel);
        check("and the numbers are not all zero", true, sequential.goldOnField() > 0);

        // repeat, because a race would not necessarily show on one run
        boolean stable = true;
        for (int i = 0; i < 20; i++) {
            stable &= sequential.equals(big.parallelStream().collect(new TileEffectSummaryCollector()));
        }
        check("stable over 20 parallel runs", true, stable);
    }

    // splitting the work differently must not change the total
    private static void mergeIsAssociative() {
        List<TileEffect> all = IntStream.range(0, 300)
                .mapToObj(TileEffectSummaryTest::effectFor)
                .toList();

        TileEffectSummary whole = collect(all);
        TileEffectSummary inTwo = merge(collect(all.subList(0, 120)), collect(all.subList(120, 300)));
        TileEffectSummary inThree = merge(merge(collect(all.subList(0, 50)),
                collect(all.subList(50, 200))), collect(all.subList(200, 300)));

        check("split in two gives the same totals", whole, inTwo);
        check("split in three gives the same totals", whole, inThree);
    }

    /* ---- through Round ---- */

    private static void roundReportsItsOwnBoard() {
        Round round = new Round(new Team("teamA"), new Team("teamB"));
        round.spawnTileEffects();
        TileEffectSummary summary = round.tileEffectSummary();

        check("round sees its four hills", 4, summary.hillsTotal());
        check("round sees its six coins", 6, summary.coinsOnField());
        check("round sees the gold", 150, summary.goldOnField());
        check("round sees two traps a side", 2, summary.trapsArmedA());
        check("and two for the other", 2, summary.trapsArmedB());
    }

    /* ---- helpers ---- */

    private static TileEffect effectFor(int i) {
        return switch (i % 3) {
            case 0 -> new Hill(i % 30, i % 29, 5);
            case 1 -> new Coin(i % 30, i % 29, 25);
            default -> new Trap(i % 30, i % 29, 20,
                    i % 6 == 2 ? Troop.Team.teamA : Troop.Team.teamB);
        };
    }

    private static TileEffectSummary collect(List<TileEffect> effects) {
        return effects.stream().collect(new TileEffectSummaryCollector());
    }

    // adds two summaries the way the collector's combiner does
    private static TileEffectSummary merge(TileEffectSummary a, TileEffectSummary b) {
        return new TileEffectSummary(
                a.hillsHeldA() + b.hillsHeldA(),
                a.hillsHeldB() + b.hillsHeldB(),
                a.hillsUnclaimed() + b.hillsUnclaimed(),
                a.goldOnField() + b.goldOnField(),
                a.coinsOnField() + b.coinsOnField(),
                a.trapsArmedA() + b.trapsArmedA(),
                a.trapsArmedB() + b.trapsArmedB());
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
