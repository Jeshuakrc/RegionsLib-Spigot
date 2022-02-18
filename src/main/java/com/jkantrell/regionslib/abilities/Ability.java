package com.jkantrell.regionslib.abilities;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.function.Function;
import java.util.function.Predicate;

public class Ability<E extends Event> {

    public final String name;
    public final Predicate<E> validator;
    public final Function<E,Player> playerGetter;
    public final Function<E,Location> locationGetter;

    public Ability(
            String name,
            Predicate<E> validation,
            Function<E, Player> playerGetter,
            Function<E, Location> locationGetter) {
        this.name = name;
        this.validator = validation;
        this.playerGetter = playerGetter;
        this.locationGetter = locationGetter;
    }

    public boolean isValid(E event) {
        return this.validator.test(event);
    }

    public boolean isAllowed(E event) {
        return Region.checkAbilityAt(
            this.playerGetter.apply(event),
            this,
            this.locationGetter.apply(event)
        );
    }

}
