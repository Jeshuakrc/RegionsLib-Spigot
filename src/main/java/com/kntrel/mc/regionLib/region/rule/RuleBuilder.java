package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.mc.regionLib.trigger.build.ReflectiveNameable;
import com.kntrel.mc.regionLib.trigger.build.TriggerBuilder;
import com.kntrel.util.TriPredicate;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.Set;
import java.util.function.*;

public abstract class RuleBuilder<E extends Event, T, B extends RuleBuilder<E, T, B>> extends TriggerBuilder<
        E,
        RuleTrigger<T, ?>,
        Rule<T>,
        B
> {

    //FACTORY
    public static <T> Starter<T> of(ValueType<T> type) {
        return new Starter<>(type);
    }
    public static <T> Starter<T> of(Class<T> clazz) {
        return of(ValueType.of(clazz));
    }
    public static <E extends Event> Bool<E> on(Class<E> eventClass) {
        return new Bool<>(eventClass, Set.of());
    }


    //FIELDS
    protected final ValueType<T> type_;
    protected TriPredicate<T, Region, E> test_;
    protected TriConsumer<T, Region, E> action_;
    protected String name_;


    //CONSTRUCTORS
    protected RuleBuilder(ValueType<T> valueType, Class<E> eventClass, Set<RuleTrigger<T, ?>> existingTriggers) {
        super(eventClass, existingTriggers);
        this.type_ = valueType;
        this.test_ = (t, r, e) -> true;
        this.action_ = null;
    }


    //CHAIN OPERATIONS
    public B iff(TriPredicate<T, Region, E> test) {
        this.test_ = test;
        return this.instance_;
    }
    public B iff(BiPredicate<T, E> test) {
        return this.iff((t, r, e) -> test.test(t, e));
    }
    public B iff(Predicate<T> test) {
        return this.iff((t, r, e) -> test.test(t));
    }
    public B then(TriConsumer<T, Region, E> action) {
        this.action_ = action;
        return this.instance_;
    }
    public B then(BiConsumer<E, T> action) {
        return this.then((t, r, e) -> action.accept(e, t));
    }
    public B then(Consumer<E> action) {
        return this.then((t, r, e) -> action.accept(e));
    }
    public B thenCancel() {
        if (!Cancellable.class.isAssignableFrom(this.eventClass_)) {
            throw new IllegalStateException("Event " + this.eventClass_.getName() + " is not cancellable");
        }
        return this.then(e -> ((Cancellable) e).setCancelled(true));
    }
    public Rule<T> underName(String name) {
        this.name_ = name;
        return this.build();
    }
    public Rule<T> done() {
        return this.build();
    }


    //IMPLEMENTATION
    @Override protected RuleTrigger<T, E> buildTrigger() {
        if (this.action_ == null) {
            throw new IllegalStateException("Rule trigger does nothing");
        }

        Function<E, Bounds> localizer = this.getLocalizer();
        Predicate<E> validator = this.getValidator();

        return new RuleTriggerImpl<>(
                this.eventClass_,
                this.bukkitPriority_,
                this.priority_,
                validator,
                this.test_,
                this.action_,
                localizer
        );
    }
    @Override protected Rule<T> buildListener(Set<RuleTrigger<T, ?>> triggers) {
        if (this.name_ == null) {
            return new UnnamedRule<>(triggers, this.type_);
        }

        return new Rule<>(
                this.name_,
                triggers,
                this.type_
        );
    }


    //SPECIALIZATIONS
    public static class Generic<E extends Event, T> extends RuleBuilder<E, T, Generic<E, T>> {
        protected Generic(ValueType<T> valueType, Class<E> eventClass, Set<RuleTrigger<T, ?>> existingTriggers) {
            super(valueType, eventClass, existingTriggers);
        }

        @Override @SuppressWarnings("unchecked")
        public <E2 extends Event> Generic<E2, T> alsoOn(Class<E2> eventClass) {
            return (Generic<E2, T>) super.alsoOn(eventClass);
        }

        @Override
        protected <E2 extends Event> Generic<E2, T> next(Class<E2> eventClass, Set<RuleTrigger<T, ?>> existingTriggers) {
            return new Generic<>(this.type_, eventClass, existingTriggers);
        }
    }
    public static class Bool<E extends Event> extends RuleBuilder<E, Boolean, Bool<E>> {
        protected Bool(Class<E> eventClass, Set<RuleTrigger<Boolean, ?>> existingTriggers) {
            super(ValueType.BOOL, eventClass, existingTriggers);
        }


        public Bool<E> ifTrue() {
            return this.iff((t, r, e) -> t);
        }
        public Bool<E> ifFalse() {
            return this.iff((t, r, e) -> !t);
        }

        @Override @SuppressWarnings("unchecked")
        public <E2 extends Event> Bool<E2> alsoOn(Class<E2> eventClass) {
            return (Bool<E2>) super.alsoOn(eventClass);
        }

        @Override
        protected <E2 extends Event> Bool<E2> next(Class<E2> eventClass, Set<RuleTrigger<Boolean, ?>> existingTriggers) {
            return new Bool<>(eventClass, existingTriggers);
        }
    }


    //SUBTYPES
    public static class Starter<T> {

        private final ValueType<T> type_;

        protected Starter(ValueType<T> type) {
            this.type_ = type;
        }

        public <E extends Event> Generic<E, T> on(Class<E> eventClass) {
            return new Generic<>(this.type_, eventClass, Set.of());
        }
    }
    protected static class UnnamedRule<T> extends Rule<T> implements ReflectiveNameable<Rule<T>> {

        public UnnamedRule(Collection<RuleTrigger<T, ?>> ruleTriggers, ValueType<T> type) {
            super(null, ruleTriggers, type);
        }
        @Override public String name() {
            throw new IllegalStateException("Unnamed rule");
        }
        @Override public Rule<T> namedAs(String name) {
            return new Rule<>(name, this.triggers(), this.getValueType());
        }
    }
}