package view;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/* Each stream partition gets its own accumulator; no shared counters or locks. */
public class ArmyHealthCollector
        implements Collector<GroupHealthSnapshot, ArmyHealthCollector.Accumulator, ArmyHealthSummary> {
    /* ---- NESTED CLASSES ---- */
    public static final class Accumulator {
        private int healthy, wounded, critical, dead, soldiers;
        private long totalHealth, maxHealth;

        private void add(GroupHealthSnapshot group) {
            if (group.health() == 0) {
                dead++;
            } else if ((long) group.health() * 100 > (long) group.maxHealth() * 70) {
                healthy++;
            } else if ((long) group.health() * 100 >= (long) group.maxHealth() * 30) {
                wounded++;
            } else {
                critical++;
            }
            totalHealth += group.health();
            maxHealth += group.maxHealth();
            soldiers += group.soldiers();
        }

        private Accumulator combine(Accumulator other) {
            healthy += other.healthy;
            wounded += other.wounded;
            critical += other.critical;
            dead += other.dead;
            totalHealth += other.totalHealth;
            maxHealth += other.maxHealth;
            soldiers += other.soldiers;
            return this;
        }

        private ArmyHealthSummary finish() {
            return new ArmyHealthSummary(healthy, wounded, critical, dead, totalHealth, maxHealth, soldiers);
        }
    }

    /* ---- OVERRIDES ---- */
    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, GroupHealthSnapshot> accumulator() {
        return Accumulator::add;
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        return Accumulator::combine;
    }

    @Override
    public Function<Accumulator, ArmyHealthSummary> finisher() {
        return Accumulator::finish;
    }

    @Override
    public Set<Characteristics> characteristics() {
        // Order cannot change integer sums/counts. Accumulators are NOT concurrent,
        // and the finisher creates a different, immutable result type.
        return Set.of(Characteristics.UNORDERED);
    }
}
