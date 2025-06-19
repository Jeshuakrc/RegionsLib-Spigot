package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.region.react.ReflectiveNameable;
import com.jkantrell.regionslib.region.react.RegionEventReactorBuilder;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import io.avaje.lang.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityBuilder<E extends Event> extends RegionEventReactorBuilder<E, Ability, AbilityBuilder<E>> {

    // Fields
    private Function<E, Player> playerGetter_;
    private Ability extends_;


    // CONSTRUCTORS
    // Fabric static method for readability
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return new AbilityBuilder<>(eventClass);
    }
    protected AbilityBuilder(Class<E> eventClass) {
        super(eventClass);
    }


    // CHAINED CONFIG
    public AbilityBuilder<E> by(Function<E, Player> playerGetter) {
        this.playerGetter_ = playerGetter;
        return this;
    }
    public AbilityBuilder<E> extend(Ability extendAbility) {
        this.extends_ = extendAbility;
        return this;
    }


    //GETTERS
    protected PointGetter getPointGetter() {
        try {
            return super.getPointGetter();
        } catch (IllegalStateException ignored) {}
        Function<Event, Player> playerGetter = this.getPlayerGetter();
        return e -> playerGetter.apply(e).getLocation();
    }
    protected Function<Event, Player> getPlayerGetter() {
        // If explicitly provided, return right away
        if (playerGetter_ != null) {
            return e -> this.playerGetter_.apply(this.eventClass_.cast(e));
        }

        // Else, try to cast to PlayerEvent
        if (PlayerEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> ((PlayerEvent) e).getPlayer();
        }

        // Else, try to find a 'getPlayer' Method that returns a player
        Method playerGetterMethod = null;
        try {
            playerGetterMethod = this.eventClass_.getMethod("getPlayer");
        } catch (NoSuchMethodException ignored) {}
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


    //FINAL OPERATIONS
    @Override protected Ability build(@Nullable Predicate<E> extraCheck) {

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
        Predicate<Event> validator = this.getValidator();
        int order = (this.priority_ != null) ? this.priority_ : 0;
        if (extraCheck != null) {
            validator = validator.and(e -> extraCheck.test(this.eventClass_.cast(e)));
        }

        if (pg != null) {
            if (name == null) {
                return new ReflectiveAbility(this.eventClass_, validator, this.getPlayerGetter(), pg, order, this.bukkitPriority_, this.extends_);
            }
            return new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), pg, order, this.bukkitPriority_, this.extends_);
        }
        if (name == null) {
            return new ReflectiveAbility(this.eventClass_, validator, this.getPlayerGetter(), ag, order, this.bukkitPriority_, this.extends_);
        }
        return new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), ag, order, this.bukkitPriority_, this.extends_);
    }

    private static class ReflectiveAbility extends Ability implements ReflectiveNameable {

        private String renamed_;

        public ReflectiveAbility(Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
            super("", eventClass, validator, playerGetter, pointGetter, order, priority, dependsOn);
            this.renamed_ = null;
        }
        public ReflectiveAbility(Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
            super("", eventClass, validator, playerGetter, areaGetter, order, priority, dependsOn);
            this.renamed_ = null;
        }

        @Override public String getName() {
            if (this.renamed_ == null) {
                throw new IllegalStateException("Trying to access an unnamed ability's name");
            }
            return this.renamed_;
        }

        @Override public void setName(String name) {
            if (this.renamed_ != null) {
                throw new IllegalStateException("Cannot name an ability twice");
            }
            this.renamed_ = name;
        }
    }
}
