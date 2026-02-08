package com.kntrel.mc.regionLib.region.listen.build;

import org.bukkit.event.Event;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public class EnumTriggerBuilder<E extends Event, N extends Enum<N>, B extends ListenerBuilder<E, ?, ?, B>> {

    private final B father_;
    private final Function<E, N> supplier_;

    EnumTriggerBuilder(B father, Function<E, N> supplier) {
        this.father_ = father;
        this.supplier_ = supplier;
    }

    public B whenIs(N instance) {
        return this.father_.when(e -> this.supplier_.apply(e).equals(instance));
    }

    public B when(Predicate<N> check) {
        return this.father_.when(e -> check.test(this.supplier_.apply(e)));
    }

    public B whenIsAnyOf(N... instances) {
        return this.father_.when(e -> Arrays.stream(instances).anyMatch(i -> this.supplier_.apply(e).equals(i)));
    }
}
