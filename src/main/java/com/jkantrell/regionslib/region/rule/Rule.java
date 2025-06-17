package com.jkantrell.regionslib.region.rule;

import com.jkantrell.regionslib.event.RuleTriggeredEvent;
import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.region.react.RegionEventReactor;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import com.jkantrell.regionslib.util.TriPredicate;
import com.jkantrell.regionslib.util.valueType.ValueType;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.*;

public class Rule<T> extends RegionEventReactor implements BiConsumer<Event, RegionContext> {

    //STATIC
    public static <E extends Event> RuleBuilder.PreRuleBuilder<E> on(Class<E> eventClass) {
        return RuleBuilder.on(eventClass);
    }


    //FIELDS
    private final TriPredicate<Event, T, Region> test_;
    private final TriConsumer<Event, T, Region> action_;
    private final ValueType<T> type_;


    //CONSTRUCTORS
    public Rule(@Nonnull String name, @Nonnull Class<? extends Event> eventClass, @Nonnull ValueType<T> type, @Nonnull PointGetter pointGetter, @Nonnull Predicate<Event> validator, int priority, EventPriority bukkitPriority, TriPredicate<Event, T, Region> test, TriConsumer<Event, T, Region>action) {
        super(name, eventClass, pointGetter, validator, priority, bukkitPriority);
        this.test_ = test;
        this.action_ = action;
        this.type_ = type;
    }
    public Rule(@Nonnull String name, @Nonnull Class<? extends Event> eventClass, @Nonnull ValueType<T> type, @Nonnull AreaGetter areaGetter, @Nonnull Predicate<Event> validator, int priority, EventPriority bukkitPriority, TriPredicate<Event, T, Region> test, TriConsumer<Event, T, Region>action) {
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
