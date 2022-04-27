package com.jkantrell.regionslib.events;

import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.abilities.Ability;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;

public class AbilityTriggeredEvent extends PlayerEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private final Region region_;
    private final Ability<?> ability_;
    private final Location location_;
    private final Event event_;
    private boolean allowed_;

    //CONSTRUCTORS
    public AbilityTriggeredEvent(Ability<?> ability, Player who, boolean allowed, Region region, Location where, Event event) {
        super(who);
        this.region_ = region;
        this.ability_ = ability;
        this.allowed_ = allowed;
        this.location_ = where;
        this.event_ = event;
    }

    //Getters
    public Region getRegion() {
        return region_;
    }
    public Ability<?> getAbility() {
        return ability_;
    }
    public Location getLocation() {
        return location_;
    }
    public Event getTriggererEvent() {
        return event_;
    }
    public boolean isAllowed() {
        return allowed_;
    }

    //SETTERS
    public void setAllowed(boolean allowed) {
        this.allowed_ = allowed;
    }
}
