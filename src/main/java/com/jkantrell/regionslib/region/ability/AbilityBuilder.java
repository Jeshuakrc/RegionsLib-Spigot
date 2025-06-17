package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.region.react.RegionEventReactorBuilder;
import com.jkantrell.regionslib.region.react.UnNamedAbility;
import com.jkantrell.regionslib.util.Area;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import io.avaje.lang.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityBuilder<E extends Event> extends RegionEventReactorBuilder<E, Ability, AbilityBuilder<E>> {

    // Fields
    private String name_;
    private Predicate<E> validator_;
    private Function<E, Player> playerGetter_;
    private Function<E, Location> pointGetter_;
    private Function<E, Area> areaGetter_;
    private Consumer<Ability> consumer_;
    private EventPriority bukkitPriority_;
    private Integer priority_;
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
        if (name == null) { name = "<unnamed>"; }
        Predicate<Event> validator = this.getValidator();
        int order = (this.priority_ != null) ? this.priority_ : 0;
        if (extraCheck != null) {
            validator = validator.and(e -> extraCheck.test(this.eventClass_.cast(e)));
        }

        if (pg != null) {
            return new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), pg, order, this.bukkitPriority_, this.extends_);
        }
        Ability ability = new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), ag, order, this.bukkitPriority_, this.extends_);

        if (this.consumer_ != null) { this.consumer_.accept(ability); }
        return ability;
    }
}
