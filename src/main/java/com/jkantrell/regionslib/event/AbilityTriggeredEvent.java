package com.jkantrell.regionslib.event;

import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.ability.Ability;
import com.jkantrell.regionslib.util.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import javax.annotation.Nonnull;
import java.util.Optional;

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
    private final Ability ability_;
    private final Event event_;
    private Location location_;
    private Area area_;
    private boolean allowed_;

    //CONSTRUCTORS
    private AbilityTriggeredEvent(Ability ability, Player who, boolean allowed, Region region, Event event) {
        super(who);
        this.region_ = region;
        this.ability_ = ability;
        this.allowed_ = allowed;
        this.event_ = event;
        this.location_ = null;
        this.area_ = null;
    }
    public AbilityTriggeredEvent(Ability ability, Player who, boolean allowed, Region region, Location where, Event event) {
        this(ability, who, allowed, region, event);
        this.location_ = where;
    }
    public AbilityTriggeredEvent(Ability ability, Player who, boolean allowed, Region region, Area where, Event event) {
        this(ability, who, allowed, region, event);
        this.area_ = where;
    }

    //Getters
    public Region getRegion() {
        return region_;
    }
    public Ability getAbility() {
        return ability_;
    }
    public Location getLocation() {
        return (this.location_ != null) ? this.location_ : this.area_.middle();
    }
    public Optional<Area> getArea() {
        return Optional.ofNullable(this.area_);
    }
    public Event getTriggererEvent() {
        return event_;
    }
    public boolean isAllowed() {
        return allowed_;
    }
    public boolean isPointBased() {
        return this.ability_.isPointBased();
    }
    public boolean isAreaBased() {
        return this.ability_.isAreaBased();
    }


    //SETTERS
    public void setAllowed(boolean allowed) {
        this.allowed_ = allowed;
    }
}
