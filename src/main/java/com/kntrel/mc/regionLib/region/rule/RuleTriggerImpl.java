package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.util.Priority;
import com.kntrel.util.TriPredicate;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RuleTriggerImpl<T, E extends Event> implements RuleTrigger<T, E> {

    //FIELDS
    private final Class<E> eventClass_;
    private final EventPriority bukkitPriority_;
    private final Priority priority_;
    private final Predicate<E> validator_;
    private final TriPredicate<T, Region, E> test_;
    private final TriConsumer<T, Region, E> action_;
    private final Function<E, Bounds> localizer_;


    //CONSTRUCTOR
    public RuleTriggerImpl(Class<E> eventClass, EventPriority bukkitPriority, Priority priority, Predicate<E> validator, TriPredicate<T, Region, E> test, TriConsumer<T, Region, E> action, Function<E, Bounds> localizer) {
        this.eventClass_ = eventClass;
        this.bukkitPriority_ = bukkitPriority;
        this.priority_ = priority;
        this.validator_ = validator;
        this.test_ = test;
        this.action_ = action;
        this.localizer_ = localizer;
    }


    //IMPLEMENTATION
    @Override public Class<E> eventClass() {
        return this.eventClass_;
    }
    @Override public EventPriority bukkitPriority() {
        return this.bukkitPriority_;
    }
    @Override public Priority priority() {
        return this.priority_;
    }
    @Override public Bounds localize(E event) {
        return this.localizer_.apply(event);
    }
    @Override public boolean appliesTo(E event) {
        return this.validator_.test(event);
    }
    @Override public boolean test(T value, Region region, E event) {
        return this.test_.test(value, region, event);
    }
    @Override public void fire(T value, Region region, E event) {
        this.action_.accept(value, region, event);
    }
}
