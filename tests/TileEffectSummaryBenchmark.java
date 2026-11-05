import controller.TileEffectSummary;
import controller.TileEffectSummaryCollector;
import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.interactables.Trap;
import entity.troops.Troop;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/*
 * A timing experiment, not a performance guarantee. The point is to answer
 * whether the census should run in parallel on a real board, and the answer
 * is no: a round holds fourteen effects, which is far below where splitting
 * the work starts to pay for itself.
 */
public class TileEffectSummaryBenchmark {

    private static volatile TileEffectSummary result;

    public static void main(String[] args) {
        System.out.println("Counting only; board construction is excluded.");
        System.out.println("Median of 7 runs after 5 warm-up runs.");
        System.out.println("Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        // 14 is a real board. the rest are there to find the crossover.
        for (int count : new int[] { 14, 1_000, 50_000, 500_000 }) {
            List<TileEffect> board = IntStream.range(0, count)
                    .mapToObj(TileEffectSummaryBenchmark::effectFor)
                    .toList();

            TileEffectSummary expected = board.stream().collect(new TileEffectSummaryCollector());

            for (int i = 0; i < 5; i++) {
                measure(board, false, expected);
                measure(board, true, expected);
            }

            long[] sequential = new long[7];
            long[] parallel = new long[7];
            for (int i = 0; i < 7; i++) {
                // alternate the order so neither gets the warm cache every time
                if (i % 2 == 0) {
                    sequential[i] = measure(board, false, expected);
                    parallel[i] = measure(board, true, expected);
                } else {
                    parallel[i] = measure(board, true, expected);
                    sequential[i] = measure(board, false, expected);
                }
            }
            Arrays.sort(sequential);
            Arrays.sort(parallel);

            double seq = sequential[3] / 1_000_000.0;
            double par = parallel[3] / 1_000_000.0;
            System.out.printf(Locale.ROOT,
                    "%7d effects: sequential %8.4f ms, parallel %8.4f ms  -> %s%n",
                    count, seq, par, seq <= par ? "sequential wins" : "parallel wins");
        }

        System.out.println();
        System.out.println("Identical results either way, so the collector is safe to parallelise.");
        System.out.println("A real board is 14 effects, so Round keeps the sequential path.");
    }

    private static long measure(List<TileEffect> board, boolean parallel, TileEffectSummary expected) {
        long start = System.nanoTime();
        TileEffectSummary summary = (parallel ? board.parallelStream() : board.stream())
                .collect(new TileEffectSummaryCollector());
        long elapsed = System.nanoTime() - start;

        if (!summary.equals(expected)) {
            throw new IllegalStateException("parallel and sequential disagreed: "
                    + summary + " vs " + expected);
        }
        result = summary;
        return elapsed;
    }

    private static TileEffect effectFor(int i) {
        return switch (i % 3) {
            case 0 -> new Hill(i % 30, i % 29, 5);
            case 1 -> new Coin(i % 30, i % 29, 25);
            default -> new Trap(i % 30, i % 29, 20,
                    i % 6 == 2 ? Troop.Team.teamA : Troop.Team.teamB);
        };
    }
}
