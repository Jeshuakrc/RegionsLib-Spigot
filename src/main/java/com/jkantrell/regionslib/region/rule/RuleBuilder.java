package com.jkantrell.regionslib.region.rule;

import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.react.RegionEventReactorBuilder;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import com.jkantrell.regionslib.util.TriPredicate;
import com.jkantrell.regionslib.util.valueType.ValueType;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RuleBuilder<E extends Event, T> extends RegionEventReactorBuilder<E, Rule<T>, RuleBuilder<E, T>> {

    //FIELDS
    private final ValueType<T> type_;
    private TriPredicate<E, T, Region> testTri_ = null;
    private BiPredicate<E, T> testBi_ = null;
    private Predicate<T> test_ = null;
    private TriConsumer<E, T, Region> actionTri_ = null;
    private BiConsumer<E, T> actionBi_ = null;
    private Consumer<E> action_ = null;


    //CONSTRUCTORS
    public static <E extends Event> PreRuleBuilder<E> on(Class<E> eventClass) {
        return new PreRuleBuilder<>(eventClass);
    }
    protected RuleBuilder(Class<E> eventClass, ValueType<T> type) {
        super(eventClass);
        this.type_ = type;
    }


    //CHAIN OPERATIONS
    public RuleBuilder<E, T> iff(Predicate<T> test) {
        this.test_ = test;
        return this;
    }
    public RuleBuilder<E, T> iff(BiPredicate<E, T> test) {
        this.testBi_ = test;
        return this;
    }
    public RuleBuilder<E, T> iff(TriPredicate<E, T, Region> test) {
        this.testTri_ = test;
        return this;
    }
    public RuleBuilder<E, T> then(Consumer<E> action) {
        this.action_ = action;
        return this;
    }
    public RuleBuilder<E, T> then(BiConsumer<E, T> action) {
        this.actionBi_ = action;
        return this;
    }
    public RuleBuilder<E, T> then(TriConsumer<E, T, Region> action) {
        this.actionTri_ = action;
        return this;
    }
    public RuleBuilder<E, T> thenCancel() {
        if (!Cancellable.class.isAssignableFrom(this.eventClass_)) {
            return this;
        }
        this.then(e -> ((Cancellable) e).setCancelled(true));
        return this;
    }


    //GETTERS
    protected TriPredicate<Event, T, Region> getTest() {
        if (this.testTri_ != null) {
            return (e, t, r) -> this.testTri_.test(this.eventClass_.cast(e), t, r);
        }
        if (this.testBi_ != null) {
            return (e, t, r) -> this.testBi_.test(this.eventClass_.cast(e), t);
        }
        if (this.test_ != null) {
            return (e, t, r) -> this.test_.test(t);
        }
        if (Boolean.class.isAssignableFrom(this.type_.getType())) {
            return (e, t, r) -> (Boolean) t;
        }
        return (e, t, r) -> true;
    }
    protected TriConsumer<Event, T, Region> getAction() {
        if (this.actionTri_ != null) {
            return (e, t, r) -> this.actionTri_.accept(this.eventClass_.cast(e), t, r);
        }
        if (this.actionBi_ != null) {
            return (e, t, r) -> this.actionBi_.accept(this.eventClass_.cast(e), t);
        }
        if (this.action_ != null) {
            return (e, t, r) -> this.action_.accept(this.eventClass_.cast(e));
        }
        throw new IllegalStateException("Rule does nothing");
    }



    @Override
    protected Rule<T> build(Predicate<E> extraCheck) {
        PointGetter pg = null; AreaGetter ag = null; Exception exception = null;
        try {
            ag = this.getAreaGetter();
        } catch (Exception e) { exception = e; }
        try {
            pg = this.getPointGetter();
        } catch (Exception e) { exception = e; }
        if (ag == null && pg == null) {
            throw (exception != null) ? new RuntimeException(exception) : new IllegalStateException("Unable to build ability. Cannot infer location");
        }

        String name = this.getName();
        if (name == null) { name = "<unnamed>"; }
        Predicate<Event> validator = this.getValidator();
        int order = (this.priority_ != null) ? this.priority_ : 0;
        if (extraCheck != null) {
            validator = validator.and(e -> extraCheck.test(this.eventClass_.cast(e)));
        }

        if (pg != null) {
            return new Rule<T>(name, this.eventClass_, this.type_, pg, validator, order, this.bukkitPriority_, this.getTest(), this.getAction());
        }
        return new Rule<T>(name, this.eventClass_, this.type_, ag, validator, order, this.bukkitPriority_, this.getTest(), this.getAction());
    }


    //CLASSES
    public static class PreRuleBuilder<E extends Event> {

        //FIELDS
        private final Class<E> eventClass_;

        //CONSTRUCTORS
        private PreRuleBuilder(Class<E> eventClass) {
            this.eventClass_ = eventClass;
        }

        //UTILITY
        public <T> RuleBuilder<E, T> having(ValueType<T> type) {
            return new RuleBuilder<>(this.eventClass_, type);
        }
    }
}
