package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.mc.regionLib.trigger.build.TriggerBuilder;
import com.kntrel.util.TriPredicate;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.function.*;

public class RuleTriggerBuilder<E extends Event, T> extends TriggerBuilder<E, RuleTrigger<T, E>, RuleTriggerBuilder<E, T>> {

    //FIELDS
    private final ValueType<T> type_;
    private TriPredicate<T, Region, E> test_;
    private TriConsumer<T, Region, E> action_;


    //CONSTRUCTORS
    protected RuleTriggerBuilder(Class<E> eventClass, ValueType<T> type) {
        super(eventClass);
        this.type_ = type;

        this.test_ = (t, r, e) -> true;
        this.action_ = null;
    }
    protected RuleTriggerBuilder(RuleTriggerBuilder<E, ?> other, ValueType<T> type) {
        this(other.eventClass_, type);
    }


    //CHAIN OPERATIONS
    public RuleTriggerBuilder<E, T> iff(Predicate<T> test) {
        this.test_ = (t, r, e) -> test.test(t);
        return this;
    }

    public RuleTriggerBuilder<E, T> iff(BiPredicate<T, E> test) {
        this.test_ = (t, r, e) -> test.test(t, e);
        return this;
    }

    public RuleTriggerBuilder<E, T> iff(TriPredicate<T, Region, E> test) {
        this.test_ = test;
        return this;
    }

    public RuleTriggerBuilder<E, T> then(Consumer<E> action) {
        this.action_ = (t, r, e) -> action.accept(e);
        return this;
    }

    public RuleTriggerBuilder<E, T> then(BiConsumer<E, T> action) {
        this.action_ = (t, r, e) -> action.accept(e, t);
        return this;
    }

    public RuleTriggerBuilder<E, T> then(TriConsumer<T, Region, E> action) {
        this.action_ = action;
        return this;
    }

    public RuleTriggerBuilder<E, T> thenCancel() {
        if (!Cancellable.class.isAssignableFrom(this.eventClass_)) {
            return this;
        }
        this.then(e -> ((Cancellable) e).setCancelled(true));
        return this;
    }



    @Override
    protected RuleTrigger<T, E> build(Predicate<E> extraCheck) {
        if (this.action_ == null) {
            throw new IllegalStateException("Rule trigger does nothing");
        }

        Function<E, Bounds> localizer = this.getLocalizer();
        Predicate<E> validator = this.getValidator();
        if (extraCheck != null) {
            validator = validator.and(e -> extraCheck.test(this.eventClass_.cast(e)));
        }

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


}