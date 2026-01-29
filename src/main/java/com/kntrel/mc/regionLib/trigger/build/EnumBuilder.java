package com.kntrel.mc.regionLib.trigger.build;

import com.kntrel.mc.regionLib.region.react.RegionEventReactorBuilder;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import org.bukkit.event.Event;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public class EnumBuilder<E extends Event, N extends Enum<N>, T extends RegionTrigger<E>> {

    private final TriggerBuilder<E, T, ?> father_;
    private final Function<E, N> supplier_;

    EnumBuilder(TriggerBuilder<E, T, ?> father, Function<E, N> supplier) {
        this.father_ = father;
        this.supplier_ = supplier;
    }

    public T build(N instance) {
        return this.father_.build(e -> this.supplier_.apply(e).equals(instance));
    }

    public T build(Predicate<N> check) {
        return this.father_.build(e -> check.test(this.supplier_.apply(e)));
    }

    public T buildOr(N... instances) {
        return this.father_.build(e -> Arrays.stream(instances).anyMatch(i -> this.supplier_.apply(e).equals(i)));
    }

    public T buildAnd(N... instances) {
        return this.father_.build(e -> Arrays.stream(instances).allMatch(i -> this.supplier_.apply(e).equals(i)));
    }

}
