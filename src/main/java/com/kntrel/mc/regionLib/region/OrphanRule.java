package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.PointGetter;
import com.kntrel.util.TriPredicate;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import java.util.function.Predicate;

final class OrphanRule extends Rule<String> {

    private static final PointGetter POINT_GETTER = e -> null;
    private static final Predicate<Event> VALIDATOR = e -> false;
    private static final TriPredicate<Event, String, Region> TEST = (e, v, r) -> false;
    private static final TriConsumer<Event, String, Region> ACTION = (e, v, r) -> {};


    public OrphanRule(@NotNull String name) {
        super(name, null, ValueType.STRING, POINT_GETTER, VALIDATOR, Integer.MIN_VALUE, EventPriority.LOWEST, TEST, ACTION);
    }
}
