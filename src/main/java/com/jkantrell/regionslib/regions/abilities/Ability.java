package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.function.Function;
import java.util.function.Predicate;

public class Ability<E extends Event> {

    public final Class<E> eventClass;
    public final String name;
    public final Predicate<E> validator;
    public final Function<E,Player> playerGetter;
    public final Function<E,Location> locationGetter;

    public Ability(
            Class<E> eventClass,
            String name,
            Predicate<E> validation,
            Function<E, Player> playerGetter,
            Function<E, Location> locationGetter) {
        this.eventClass = eventClass;
        this.name = name;
        this.validator = validation;
        this.playerGetter = playerGetter;
        this.locationGetter = locationGetter;
    }

    public void register() {
        RegionsLib.getAbilityHandler().register(this);
    }

    boolean isValid(E event) {
        return this.validator.test(event);
    }

    boolean isAllowed(E event) {
        return Region.checkAbilityAt(
            this.playerGetter.apply(event),
            this,
            this.locationGetter.apply(event)
        );
    }

}
