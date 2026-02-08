package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public class RuleTriggeredEvent extends Event {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private final Region region_;
    private final Rule<?> rule_;
    private final Event event_;
    private Location location_;
    private Area area_;


    //CONSTRUCTORS
    private RuleTriggeredEvent(Rule<?> rule, Region region, Event event) {
        this.region_ = region;
        this.rule_ = rule;
        this.event_ = event;
        this.location_ = null;
        this.area_ = null;
    }
    public RuleTriggeredEvent(Rule<?> rule, Region region, Location where, Event event) {
        this(rule, region, event);
        this.location_ = where;
    }
    public RuleTriggeredEvent(Rule<?> rule, Region region, Area where, Event event) {
        this(rule, region, event);
        this.area_ = where;
    }

    //Getters
    public Region getRegion() {
        return this.region_;
    }
    public Rule<?> getRule() {
        return this.rule_;
    }
    public Location getLocation() {
        return (this.location_ != null) ? this.location_ : this.area_.middle();
    }
    public Optional<Area> getArea() {
        return Optional.ofNullable(this.area_);
    }
    public Event getTriggererEvent() {
        return this.event_;
    }

}
