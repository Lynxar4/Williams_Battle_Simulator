package controller;

import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.TileEffect;
import entity.interactables.TileEffectVisitor;
import entity.interactables.Trap;
import entity.troops.Troop;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

// counts up what is still on the board in one pass.
//
// the accumulator is the tile effect visitor added earlier, so the collector
// never asks what kind of effect it is holding. each effect hands itself to
// the tally and the matching count goes up.
//
// safe to run in parallel: a tally is only ever touched by the thread that
// made it, and merging two tallies is addition, which is associative, so it
// does not matter how the stream splits the work.
public class TileEffectSummaryCollector
        implements Collector<TileEffect, TileEffectSummaryCollector.Tally, TileEffectSummary> {

    /* ---- THE ACCUMULATOR ---- */

    // mutable on purpose, and never shared. visitHill and friends return the
    // tally so the accumulator below is a single expression.
    public static final class Tally implements TileEffectVisitor<Tally> {
        private int hillsHeldA;
        private int hillsHeldB;
        private int hillsUnclaimed;
        private int goldOnField;
        private int coinsOnField;
        private int trapsArmedA;
        private int trapsArmedB;

        @Override
        public Tally visitHill(Hill hill) {
            if (!hill.isClaimed()) {
                hillsUnclaimed++;
            } else if (hill.getHolder() == Troop.Team.teamA) {
                hillsHeldA++;
            } else {
                hillsHeldB++;
            }
            return this;
        }

        @Override
        public Tally visitCoin(Coin coin) {
            coinsOnField++;
            goldOnField += coin.getAmount();
            return this;
        }

        @Override
        public Tally visitTrap(Trap trap) {
            // a sprung trap leaves the board, so anything still here is live
            if (trap.getOwner() == Troop.Team.teamA) {
                trapsArmedA++;
            } else {
                trapsArmedB++;
            }
            return this;
        }

        // joins another thread's partial count into this one
        Tally merge(Tally other) {
            hillsHeldA += other.hillsHeldA;
            hillsHeldB += other.hillsHeldB;
            hillsUnclaimed += other.hillsUnclaimed;
            goldOnField += other.goldOnField;
            coinsOnField += other.coinsOnField;
            trapsArmedA += other.trapsArmedA;
            trapsArmedB += other.trapsArmedB;
            return this;
        }

        TileEffectSummary toSummary() {
            return new TileEffectSummary(hillsHeldA, hillsHeldB, hillsUnclaimed,
                    goldOnField, coinsOnField, trapsArmedA, trapsArmedB);
        }
    }

    /* ---- THE COLLECTOR ---- */

    @Override
    public Supplier<Tally> supplier() {
        return Tally::new;
    }

    // one line, because the effect does the dispatching
    @Override
    public BiConsumer<Tally, TileEffect> accumulator() {
        return (tally, effect) -> effect.accept(tally);
    }

    @Override
    public BinaryOperator<Tally> combiner() {
        return Tally::merge;
    }

    // the tally is mutable and the summary is not, so the finisher converts
    // rather than handing the live counter out
    @Override
    public Function<Tally, TileEffectSummary> finisher() {
        return Tally::toSummary;
    }

    // no IDENTITY_FINISH because of that conversion, and not UNORDERED either:
    // leaving it off costs nothing here and keeps the contract honest.
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
