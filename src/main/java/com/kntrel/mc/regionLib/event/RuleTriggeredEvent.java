package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.Place;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RuleTriggeredEvent extends RegionEvent implements Cancellable {
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
    private final Rule<?> rule_;
    private final ValueHolder<?> value_;
    private final RegionTrigger<?> trigger_;
    private final Event event_;
    private final Place place_;
    private boolean cancelled_;


    //CONSTRUCTORS
    public RuleTriggeredEvent(Region region, Rule<?> rule, @Nullable ValueHolder<?> value, RegionTrigger<?> trigger, Event event, Place place) {
        super(region);
        this.rule_ = rule;
        this.value_ = value;
        this.trigger_ = trigger;
        this.event_ = event;
        this.place_ = place;
        this.cancelled_ = false;
    }


    //GETTERS
    public Rule<?> getRule() { return this.rule_; }
    @Nullable public ValueHolder<?> getValue() { return this.value_; }
    public RegionTrigger<?> getTrigger() { return this.trigger_; }
    public Event getEvent() { return this.event_; }
    public Place getPlace() { return this.place_; }
    public boolean wasAtPoint() { return this.place_.isPoint(); }
    public boolean wasInArea() { return this.place_.isArea(); }
    public boolean wasPresent() { return this.value_ != null; }
    public boolean wasAbsent() { return this.value_ == null; }
    @Override public boolean isCancelled() { return this.cancelled_; }


    //SETTERS
    @Override public void setCancelled(boolean b) { this.cancelled_ = b; }
}
