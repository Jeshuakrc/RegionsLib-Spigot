package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.mc.regionLib.trigger.RegionListener;
import com.kntrel.mc.regionLib.trigger.build.ListenerBuilder;
import com.kntrel.mc.regionLib.trigger.build.ReflectiveNameable;
import com.kntrel.util.Priority;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityBuilder<E extends Event> extends ListenerBuilder<
        E,
        AbilityTrigger<?>,
        Ability,
        AbilityBuilder<E>
> {

    // CONSTANTS
    private static final BiConsumer<Event, List<Region>> DEFAULT_ON_ALLOWED = (e, r) -> {
    };
    private static final BiConsumer<Event, List<Region>> DEFAULT_ON_DENIED = (e, r) -> {
        if (e instanceof Cancellable) {
            ((Cancellable) e).setCancelled(true);
        }
    };


    // TRIGGER FIELDS
    private Function<E, Player> attributer = null;


    // ABILITY FIELDS
    private Ability extends_ = null;
    private String name_ = null;
    private BiConsumer<Event, List<Region>> onAllowed_ = DEFAULT_ON_ALLOWED;
    private BiConsumer<Event, List<Region>> onDenied_ = DEFAULT_ON_DENIED;

    //FACTORY
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return new AbilityBuilder<>(eventClass);
    }


    //CONSTRUCTOR
    private AbilityBuilder(Class<E> eventClass, Set<AbilityTrigger<?>> existingTriggers) {
        super(eventClass, existingTriggers);
    }
    private AbilityBuilder(Class<E> eventClass) {
        super(eventClass);
    }


    // CHAINED CONFIG
    public AbilityBuilder<E> by(Function<E, Player> playerGetter) {
        this.attributer = playerGetter;
        return this;
    }

    public AbilityBuilder<E> extend(Ability extendAbility) {
        this.extends_ = extendAbility;
        return this;
    }
    public AbilityBuilder<E> ifAllowed(BiConsumer<Event, List<Region>> onAllowed) {
        this.onAllowed_ = onAllowed;
        return this;
    }
    public AbilityBuilder<E> ifDenied(BiConsumer<Event, List<Region>> onDenied) {
        this.onDenied_ = onDenied;
        return this;
    }
    public AbilityBuilder<E> ifDeniedCancelAnd(BiConsumer<Event, List<Region>> onDenied) {
        this.onDenied_ = DEFAULT_ON_DENIED.andThen(onDenied);
        return this;
    }

    @Override @SuppressWarnings("unchecked")
    public <E2 extends Event> AbilityBuilder<E2> alsoOn(Class<E2> eventClass) {
        return (AbilityBuilder<E2>) super.alsoOn(eventClass);
    }

    //FINAL
    public Ability named(String name) {
        this.name_ = name;
        return this.build();
    }

    public Ability done() {
        return this.build();
    }


    //GETTERS
    protected Function<E, Player> getAttributer() {
        // If explicitly provided, return right away
        if (attributer != null) {
            return attributer;
        }

        // Else, try to cast to PlayerEvent
        if (PlayerEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> ((PlayerEvent) e).getPlayer();
        }

        // Else, try to find a 'getPlayer' Method that returns a player
        Method playerGetterMethod = null;
        try {
            playerGetterMethod = this.eventClass_.getMethod("getPlayer");
        } catch (NoSuchMethodException ignored) {
        }

        if (!(playerGetterMethod != null && playerGetterMethod.getReturnType().equals(Player.class))) {
            throw new IllegalStateException("Cannot build Ability. Unable de infer player from event.");
        }

        final Method finalPlayerGetterMethod = playerGetterMethod;
        return e -> {
            try {
                return (Player) finalPlayerGetterMethod.invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        };
    }


    //IMPLEMENTATION
    @Override
    protected AbilityTrigger<?> buildTrigger() {
        return new AbilityTriggerImpl<E>(
                this.eventClass_,
                this.priority_,
                this.getAttributer(),
                this.localizer_,
                this.validator_
        );
    }

    @Override
    protected Ability buildListener(Set<AbilityTrigger<?>> triggers) {
        if (this.name_ == null) {
            return new UnnamedAbility(triggers, this.extends_, this.onAllowed_, this.onDenied_);
        }

        return new AbilityImpl(this.name_, triggers, this.extends_, this.onAllowed_, this.onDenied_);
    }

    @Override
    protected <E2 extends Event> AbilityBuilder<E2> next(Class<E2> eventClass, Set<AbilityTrigger<?>> existingTriggers) {
        return new AbilityBuilder<>(eventClass, existingTriggers);
    }


    //SUBTYPES
    private record AbilityTriggerImpl<E extends Event>(
            Class<E> eventClass,
            Priority priority,
            Function<E, Player> attributer,
            Function<E, Bounds> localizer,
            Predicate<E> validator

    ) implements AbilityTrigger<E> {

        @Override
        public Player attribute(E event) {
            return this.attributer.apply(event);
        }

        @Override
        public Class<E> eventClass() {
            return this.eventClass;
        }

        @Override
        public Priority priority() {
            return this.priority;
        }

        @Override
        public Bounds localize(E event) {
            return this.localizer.apply(event);
        }

        @Override
        public boolean appliesTo(E event) {
            return this.validator.test(event);
        }
    }

    private static class UnnamedAbility implements Ability, ReflectiveNameable<Ability> {

        //FIELDS
        private final Set<AbilityTrigger<?>> triggers_;
        private final Ability super_;
        private final BiConsumer<Event, List<Region>> onAllowed_;
        private final BiConsumer<Event, List<Region>> onDenied_;


        //CONSTRUCTOR
        public UnnamedAbility(Set<AbilityTrigger<?>> triggers, @Nullable Ability superAbility, BiConsumer<Event, List<Region>> onAllowed, BiConsumer<Event, List<Region>> onDenied) {
            this.triggers_ = Set.copyOf(triggers);
            this.super_ = superAbility;
            this.onAllowed_ = onAllowed;
            this.onDenied_ = onDenied;
        }


        //IMPLEMENTATION
        @Override public Optional<Ability> superAbility() { return Optional.ofNullable(this.super_); }
        @Override public void onAllowed(Event event, List<Region> regions) { /* does nothing */ }
        @Override public void onDenied(Event event, List<Region> regions) { /* does nothing */ }
        @Override public String name() {
            throw new IllegalStateException("UnnamedAbility does not have a name.");
        }
        @Override public Collection<AbilityTrigger<?>> triggers() {
            return this.triggers_;
        }
        @Override public Ability namedAs(String name) {
            return new AbilityImpl(name, this.triggers_, this.super_, this.onAllowed_, this.onDenied_);
        }
    }
}
