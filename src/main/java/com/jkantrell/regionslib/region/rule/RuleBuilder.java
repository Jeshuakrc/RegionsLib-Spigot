package com.jkantrell.regionslib.region.rule;

import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.react.RegionEventReactorBuilder;
import com.jkantrell.regionslib.util.Area;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import com.jkantrell.regionslib.util.TriPredicate;
import com.jkantrell.regionslib.util.valueType.ValueType;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import javax.annotation.Nonnull;
import java.util.function.*;

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
    public static <E extends Event> BooleanRuleBuilder<E> on(Class<E> eventClass) {
        return new BooleanRuleBuilder<>(eventClass);
    }

    protected RuleBuilder(RuleBuilder<E, ?> other, ValueType<T> type) {
        super(other.eventClass_);
        this.type_ = type;
        this.action_ = other.action_;
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
        PointGetter pg = null;
        AreaGetter ag = null;
        Exception exception = null;
        try {
            ag = this.getAreaGetter();
        } catch (Exception e) {
            exception = e;
        }
        try {
            pg = this.getPointGetter();
        } catch (Exception e) {
            exception = e;
        }
        if (ag == null && pg == null) {
            throw (exception != null) ? new RuntimeException(exception) : new IllegalStateException("Unable to build ability. Cannot infer location");
        }

        String name = this.getName();
        if (name == null) {
            name = "<unnamed>";
        }
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
    public static class BooleanRuleBuilder<E extends Event> extends RuleBuilder<E, Boolean> {

        protected BooleanRuleBuilder(Class<E> eventClass) {
            super(eventClass, ValueType.BOOL);
            this.ifTrue();
        }

        public <T> RuleBuilder<E, T> having(ValueType<T> type) {
            return new RuleBuilder<>(this, type);
        }

        public RuleBuilder<E, Boolean> ifFalse() {
            this.iff(b -> !b);
            return this;
        }
        public RuleBuilder<E, Boolean> ifTrue() {
            this.iff(b -> b);
            return this;
        }

        @Override public BooleanRuleBuilder<E> when(Predicate<E> validator) {
            super.when(validator);
            return this;
        }
        @Override public BooleanRuleBuilder<E> at(Function<E, Location> pointGetter) {
            super.at(pointGetter);
            return this;
        }
        @Override public BooleanRuleBuilder<E> in(Function<E, Area> areaGetter) {
            super.in(areaGetter);
            return this;
        }
    }

    static class ReflectiveRule<T> extends Rule<T> {

        private String name_;

        public ReflectiveRule(@Nonnull Class<? extends Event> eventClass, @Nonnull ValueType<T> type, @Nonnull PointGetter pointGetter, @Nonnull Predicate<Event> validator, int priority, EventPriority bukkitPriority, TriPredicate<Event, T, Region> test, TriConsumer<Event, T, Region> action) {
            super("", eventClass, type, pointGetter, validator, priority, bukkitPriority, test, action);
            this.name_ = null;
        }

        public void setName(String name) {
            if (this.name_ != null) {
                throw new IllegalStateException("Reflective rule is already named");
            }
            this.name_ = name;
        }

        @Override public String getName() {
            if (this.name_ == null) {
                throw new IllegalStateException("Trying to access an unnamed reflective rule's name");
            }
            return this.name_;
        }
    }
}