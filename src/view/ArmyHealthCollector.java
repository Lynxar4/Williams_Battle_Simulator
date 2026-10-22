package view;

import entity.troops.Troop;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ArmyHealthCollector
        implements Collector<Troop, ArmyHealthSummary, ArmyHealthSummary> {

    @Override
    public Supplier<ArmyHealthSummary> supplier() {
        return ArmyHealthSummary::new;
    }

    @Override
    public BiConsumer<ArmyHealthSummary, Troop> accumulator() {
        return (summary, troop) -> {
            if (troop.isDestroyed()) {
                summary.addDead();
            } else if (troop.getHealth() > 70) {
                summary.addHealthy(troop.getHealth());
            } else if (troop.getHealth() >= 30) {
                summary.addWounded(troop.getHealth());
            } else {
                summary.addCritical(troop.getHealth());
            }
        };
    }

    @Override
    public BinaryOperator<ArmyHealthSummary> combiner() {
        return (summary1, summary2) -> {
            summary1.combine(summary2);
            return summary1;
        };
    }

    @Override
    public Function<ArmyHealthSummary, ArmyHealthSummary> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.IDENTITY_FINISH);
    }
}