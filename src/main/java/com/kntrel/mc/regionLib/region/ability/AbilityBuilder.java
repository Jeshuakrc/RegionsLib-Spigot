package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.Bounds;
import com.kntrel.mc.regionLib.region.listen.build.ListenerBuilder;
import com.kntrel.mc.regionLib.region.listen.build.ReflectiveNameable;
import com.kntrel.util.Priority;
import com.kntrel.util.SetMap;
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
    private static <E extends Event> BiConsumer<E, List<Region>> defaultOnAllowed() {
        return (e, r) -> {};
    }
    private static <E extends Event> BiConsumer<E, List<Region>> defaultOnDenied() {
        return (e, r) -> {
            if (e instanceof Cancellable can) { can.setCancelled(true); }
        };
    }


    // TRIGGER FIELDS
    private Function<E, Player> attributer = null;
    private BiConsumer<E, List<Region>> onAllowed_ = defaultOnAllowed();
    private BiConsumer<E, List<Region>> onDenied_ = defaultOnDenied();


    // ABILITY FIELDS
    private Ability extends_ = null;
    private String name_ = null;
    private SetMap<Class<? extends Event>, BiConsumer<Event, List<Region>>> onAllowedMap_;
    private SetMap<Class<? extends Event>, BiConsumer<Event, List<Region>>> onDeniedMap_;

    //FACTORY
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return new AbilityBuilder<>(eventClass);
    }


    //CONSTRUCTOR
    private AbilityBuilder(
            Class<E> eventClass,
            Set<AbilityTrigger<?>> existingTriggers,
            SetMap<Class<? extends Event>, BiConsumer<Event, List<Region>>> onAllowedMap,
            SetMap<Class<? extends Event>, BiConsumer<Event, List<Region>>> onDeniedMap
        ) {
        super(eventClass, existingTriggers);
        this.onAllowedMap_ = onAllowedMap;
        this.onDeniedMap_ = onDeniedMap;
    }
    private AbilityBuilder(Class<E> eventClass) {
        super(eventClass);
        this.onAllowedMap_ = new SetMap<>();
        this.onDeniedMap_ = new SetMap<>();
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
    public AbilityBuilder<E> ifAllowed(BiConsumer<E, List<Region>> onAllowed) {
        this.onAllowed_ = onAllowed;
        return this;
    }
    public AbilityBuilder<E> ifDenied(BiConsumer<E, List<Region>> onDenied) {
        this.onDenied_ = onDenied;
        return this;
    }
    public AbilityBuilder<E> ifDeniedCancelAnd(BiConsumer<E, List<Region>> onDenied) {
        this.onDenied_ = (e, r) -> {
            defaultOnDenied().accept(e, r);
            onDenied.accept(e, r);
        };
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
    @Override @SuppressWarnings({ "rawtypes", "unchecked" })
    protected AbilityTrigger<?> buildTrigger() {

        this.onAllowedMap_.putInto(this.eventClass_, (BiConsumer<Event, List<Region>>) this.onAllowed_);
        this.onDeniedMap_.putInto(this.eventClass_, (BiConsumer<Event, List<Region>>) this.onDenied_);

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

        BiConsumer<Event, List<Region>> onAllowed = new FinalAction(this.onAllowedMap_),
                                        onDenied = new FinalAction(this.onDeniedMap_);

        if (this.name_ == null) {
            return new UnnamedAbility(triggers, this.extends_, onAllowed, onDenied);
        }

        return new AbilityImpl(this.name_, triggers, this.extends_, onAllowed, onDenied);
    }

    @Override
    protected <E2 extends Event> AbilityBuilder<E2> next(Class<E2> eventClass, Set<AbilityTrigger<?>> existingTriggers) {
        return new AbilityBuilder<>(eventClass, existingTriggers, this.onAllowedMap_, this.onDeniedMap_);
    }


    //SUBTYPES
    private record FinalAction(SetMap<Class<? extends Event>, BiConsumer<Event, List<Region>>> actionMap) implements BiConsumer<Event, List<Region>> {
        @Override
        public void accept(Event e, List<Region> r) {
            var actions = this.actionMap.get(e.getClass());
            if (actions == null) { return; }
            for (var action : actions) {
                action.accept(e, r);
            }
        }
    }
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
