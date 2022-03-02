package com.jkantrell.regionslib.regions.abilities;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class AbilityEnumBuilder<E extends Event,T extends Enum> {

    //FIELDS
    public final Class<E> eventClass;
    public final BiPredicate<E,T> validator;
    public final Function<E, Player> playerGetter;
    public final Function<E, Location> locationGetter;

    //CONSTRUCTORS
    public AbilityEnumBuilder(Class<E> eventClass, BiPredicate<E,T> validator, Function<E, Player> playerGetter, Function<E, Location> locationGetter) {
        this.eventClass = eventClass;
        this.validator = validator;
        this.playerGetter = playerGetter;
        this.locationGetter = locationGetter;
    }

    //METHODS
    public Ability<E> build(String name, T member) {
        return new Ability<E>(
                this.eventClass,
                name,
                e -> this.validator.test(e,member),
                this.playerGetter,
                this.locationGetter
        );
    }
    public Ability<E> build(T member) {
        return this.build(null,member);
    }

}
