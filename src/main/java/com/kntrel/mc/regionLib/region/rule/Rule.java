package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.event.RuleTriggeredEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.react.RegionEventReactor;
import com.kntrel.mc.regionLib.util.AreaGetter;
import com.kntrel.mc.regionLib.util.PointGetter;
import com.kntrel.util.TriPredicate;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.function.*;

public class Rule<T> extends RegionEventReactor implements BiConsumer<Event, RegionContext> {

    //STATIC
    public static <E extends Event> RuleBuilder.BooleanRuleBuilder<E> on(Class<E> eventClass) {
        return RuleBuilder.on(eventClass);
    }


    //FIELDS
    private final TriPredicate<Event, T, Region> test_;
    private final TriConsumer<Event, T, Region> action_;
    private final ValueType<T> type_;


    //CONSTRUCTORS
    public Rule(@NotNull String name, @NotNull Class<? extends Event> eventClass, @NotNull ValueType<T> type, @NotNull PointGetter pointGetter, @NotNull Predicate<Event> validator, int priority, EventPriority bukkitPriority, TriPredicate<Event, T, Region> test, TriConsumer<Event, T, Region>action) {
        super(name, eventClass, pointGetter, validator, priority, bukkitPriority);
        this.test_ = test;
        this.action_ = action;
        this.type_ = type;
    }
    public Rule(@NotNull String name, @NotNull Class<? extends Event> eventClass, @NotNull ValueType<T> type, @NotNull AreaGetter areaGetter, @NotNull Predicate<Event> validator, int priority, EventPriority bukkitPriority, TriPredicate<Event, T, Region> test, TriConsumer<Event, T, Region>action) {
        super(name, eventClass, areaGetter, validator, priority, bukkitPriority);
        this.test_ = test;
        this.action_ = action;
        this.type_ = type;
    }


    //GETTERS
    public TriPredicate<Event, T, Region> getTest() {
        return this.test_;
    }
    public TriConsumer<Event, T, Region>getAction() {
        return this.action_;
    }
    public ValueType<T> getValueType() {
        return this.type_;
    }


    //IMPLEMENTATION
    @Override public void accept(Event event, RegionContext ctx) {
        this.fire(event, ctx);
    }
    public void fire(Event event, RegionContext context) {
        if (!this.validator_.test(event)) { return; }

        List<Region> regions = this.resolveRegions(event, context);
        if (regions.isEmpty()) return;

        int i = 0;
        for (Region region : regions) {
            T val = region.getRuleValue(this).orElse(null);
            if (val == null) { continue; }
            if (!this.test_.test(event, val, region)) { continue; }
            this.action_.accept(event, val, region);

            RuleTriggeredEvent e = this.isPointBased()
                    ? new RuleTriggeredEvent(this, region, this.pointGetter_.apply(event), event)
                    : new RuleTriggeredEvent(this, region, this.areaGetter_.apply(event), event);
            context.callEvent(e);
        }
    }

}
