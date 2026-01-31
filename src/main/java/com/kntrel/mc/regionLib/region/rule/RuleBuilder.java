package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import com.kntrel.mc.regionLib.trigger.build.ReflectiveNameable;
import com.kntrel.mc.regionLib.trigger.build.ListenerBuilder;
import com.kntrel.util.Priority;
import com.kntrel.util.SetMap;
import com.kntrel.util.TriPredicate;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.*;

public abstract class RuleBuilder<E extends Event, T, B extends RuleBuilder<E, T, B>> extends ListenerBuilder<
        E,
        RegionTrigger<?>,
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

    //LISTENER FIELDS
    protected final SetMap<Class<? extends Event>, TriConsumer<T, Event, Region>> actions_;
    protected final SetMap<Class<? extends Event>, BiConsumer<Event, Region>> absentActions_;
    protected String name_;


    //TRIGGER FIELDS
    protected final ValueType<T> type_;
    protected TriPredicate<T, E, Region> test_;
    protected TriConsumer<T, E, Region> action_;
    protected BiConsumer<Event, Region> absentAction_;


    //CONSTRUCTORS
    protected RuleBuilder(
            ValueType<T> valueType,
            Class<E> eventClass,
            Set<RegionTrigger<?>> existingTriggers,
            SetMap<Class<? extends Event>,TriConsumer<T, Event, Region>> actions,
            SetMap<Class<? extends Event>,BiConsumer<Event, Region>> absentActions
    ) {
        super(eventClass, existingTriggers);
        this.type_ = valueType;
        this.test_ = (t, r, e) -> true;
        this.action_ = null;
        this.actions_ = actions;
        this.absentActions_ = absentActions;
    }

    protected RuleBuilder(ValueType<T> valueType, Class<E> eventClass, Set<RegionTrigger<?>> existingTriggers) {
        this(valueType, eventClass, existingTriggers, new SetMap<>(), new SetMap<>());
    }


    //CHAIN OPERATIONS
    public B iff(TriPredicate<T, E, Region> test) {
        this.test_ = test;
        return this.instance_;
    }

    public B iff(BiPredicate<T, E> test) {
        return this.iff((t, e, r) -> test.test(t, e));
    }

    public B iff(Predicate<T> test) {
        return this.iff((t, e, r) -> test.test(t));
    }

    public B then(TriConsumer<T, E, Region> action) {
        this.action_ = action;
        return this.instance_;
    }

    public B then(BiConsumer<T, E> action) {
        return this.then((t, e, r) -> action.accept(t, e));
    }

    public B then(Consumer<E> action) {
        return this.then((t, e, r) -> action.accept(e));
    }

    public B thenCancel() {
        if (!Cancellable.class.isAssignableFrom(this.eventClass_)) {
            throw new IllegalStateException("Event " + this.eventClass_.getName() + " is not cancellable");
        }
        return this.then(e -> ((Cancellable) e).setCancelled(true));
    }

    public B ifAbsent(BiConsumer<Event, Region> action) {
        this.absentAction_ = action;
        return this.instance_;
    }
    public B ifAbsent(Consumer<Event> action) {
        return this.ifAbsent((e, r) -> action.accept(e));
    }
    public Rule<T> underName(String name) {
        this.name_ = name;
        return this.build();
    }
    public Rule<T> done() {
        return this.build();
    }


    //IMPLEMENTATION
    @Override protected RegionTrigger<E> buildTrigger() {
        if (this.action_ == null) {
            throw new IllegalStateException("Rule trigger does nothing");
        }

        Function<E, Bounds> localizer = this.getLocalizer();
        Predicate<E> validator = this.getValidator();

        TriConsumer<T, Event, Region> action = (v, e, r) -> {
            E ev = this.eventClass_.cast(e);
            if (this.test_.test(v, ev, r)) {
                this.action_.accept(v, ev, r);
            }
        };
        this.actions_.putInto(this.eventClass_, action);
        this.absentActions_.putInto(this.eventClass_, this.absentAction_);

        return new RuleTriggerImpl<>(
                this.eventClass_,
                this.bukkitPriority_,
                this.priority_,
                localizer,
                validator
        );
    }
    @Override protected Rule<T> buildListener(Set<RegionTrigger<?>> triggers) {
        if (this.name_ == null) {
            return new UnnamedRule<>(
                    this.type_,
                    triggers,
                    new FinalAction<>(this.actions_),
                    new FinalAbsentAction(this.absentActions_)
            );
        }

        return new RuleImpl<>(
                this.name_,
                this.type_,
                triggers,
                new FinalAction<>(this.actions_),
                new FinalAbsentAction(this.absentActions_)
        );
    }


    //HELPERS



    //SPECIALIZATIONS
    public static class Generic<E extends Event, T> extends RuleBuilder<E, T, Generic<E, T>> {
        protected Generic(ValueType<T> valueType, Class<E> eventClass, Set<RegionTrigger<?>> existingTriggers) {
            super(valueType, eventClass, existingTriggers);
        }

        @Override @SuppressWarnings("unchecked")
        public <E2 extends Event> Generic<E2, T> alsoOn(Class<E2> eventClass) {
            return (Generic<E2, T>) super.alsoOn(eventClass);
        }

        @Override
        protected <E2 extends Event> Generic<E2, T> next(Class<E2> eventClass, Set<RegionTrigger<?>> existingTriggers) {
            return new Generic<>(this.type_, eventClass, existingTriggers);
        }
    }
    public static class Bool<E extends Event> extends RuleBuilder<E, Boolean, Bool<E>> {
        protected Bool(Class<E> eventClass, Set<RegionTrigger<?>> existingTriggers) {
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
        protected <E2 extends Event> Bool<E2> next(Class<E2> eventClass, Set<RegionTrigger<?>> existingTriggers) {
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
    protected record FinalAction<T>(Map<Class<? extends Event>, Set<TriConsumer<T, Event, Region>>> actionMap) implements TriConsumer<T, Event, Region> {
        @Override
        public void accept(T t, Event e, Region r) {
            var actions = this.actionMap.get(e.getClass());
            if (actions == null) { return; }
            for (var action : actions) {
                action.accept(t, e, r);
            }
        }
    }
    protected record FinalAbsentAction(Map<Class<? extends Event>, Set<BiConsumer<Event, Region>>> actionMap) implements BiConsumer<Event, Region> {
        @Override
        public void accept(Event e, Region r) {
            var actions = this.actionMap.get(e.getClass());
            if (actions == null) { return; }
            for (var action : actions) {
                action.accept(e, r);
            }
        }
    }
    protected static class UnnamedRule<T> implements Rule<T>, ReflectiveNameable<Rule<T>> {

        //FIELDS

        private final ValueType<T> type_;
        private final Collection<RegionTrigger<?>> triggers_;
        private final TriConsumer<T, Event, Region> action_;
        private final BiConsumer<Event, Region> absentAction_;


        //CONSTRUCTOR
        public UnnamedRule(ValueType<T> type, Collection<RegionTrigger<?>> ruleTriggers, TriConsumer<T, Event, Region> action, BiConsumer<Event, Region> absentAction) {
            this.triggers_ = ruleTriggers;
            this.type_ = type;
            this.action_ = action;
            this.absentAction_ = absentAction;
        }

        //IMPLEMENTATION
        @Override public String name() {
            throw new IllegalStateException("Unnamed rule.");
        }
        @Override public Collection<RegionTrigger<?>> triggers() {
            return this.triggers_;
        }
        @Override public Rule<T> namedAs(String name) {
            return new RuleImpl<>(name, this.valueType(), this.triggers_, this.action_, this.absentAction_);
        }
        @Override public ValueType<T> valueType() {
            return this.type_;
        }
        @Override public void fire(T value, Event event, Region triggerer) {
            this.action_.accept(value, event, triggerer);
        }
        @Override public void fireOnAbsent(Event event, Region triggerer) {
            this.absentAction_.accept(event, triggerer);
        }
    }
    protected record RuleTriggerImpl<E extends Event, T>(
            Class<E> eventClass,
            EventPriority bukkitPriority,
            Priority priority,
            Function<E, Bounds> localizer,
            Predicate<E> validator
    ) implements RegionTrigger<E> {
        @Override public Bounds localize(E event) { return this.localizer.apply(event); }
        @Override public boolean appliesTo(E event) { return this.validator.test(event); }
    }
}