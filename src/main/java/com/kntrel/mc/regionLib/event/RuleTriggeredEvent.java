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

/**
 * Fired when a rule trigger evaluates for a region.
 */
public class RuleTriggeredEvent extends RegionEvent implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * Returns the static handler list for this event.
     *
     * @return handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @NotNull
    /**
     * Returns the handler list for this event instance.
     *
     * @return handler list
     */
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
    /**
     * Creates a rule triggered event.
     *
     * @param region region tied to the rule
     * @param rule rule that was evaluated
     * @param value rule value if present
     * @param trigger trigger that fired
     * @param event Bukkit event that triggered the evaluation
     * @param place location/area context
     */
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
    /**
     * Returns the rule that was evaluated.
     *
     * @return rule instance
     */
    public Rule<?> getRule() { return this.rule_; }
    /**
     * Returns the rule value if present.
     *
     * @return value holder or null
     */
    @Nullable public ValueHolder<?> getValue() { return this.value_; }
    /**
     * Returns the trigger that fired.
     *
     * @return trigger instance
     */
    public RegionTrigger<?> getTrigger() { return this.trigger_; }
    /**
     * Returns the underlying Bukkit event.
     *
     * @return triggering event
     */
    public Event getEvent() { return this.event_; }
    /**
     * Returns the trigger place context.
     *
     * @return place context
     */
    public Place getPlace() { return this.place_; }
    /**
     * Returns whether the trigger fired at a single point.
     *
     * @return true if point-based
     */
    public boolean wasAtPoint() { return this.place_.isPoint(); }
    /**
     * Returns whether the trigger fired within an area.
     *
     * @return true if area-based
     */
    public boolean wasInArea() { return this.place_.isArea(); }
    /**
     * Returns whether the rule had a present value.
     *
     * @return true if present
     */
    public boolean wasPresent() { return this.value_ != null; }
    /**
     * Returns whether the rule was absent.
     *
     * @return true if absent
     */
    public boolean wasAbsent() { return this.value_ == null; }
    /**
     * Returns whether this event is cancelled.
     *
     * @return true if cancelled
     */
    @Override public boolean isCancelled() { return this.cancelled_; }


    //SETTERS
    /**
     * Sets whether this event is cancelled.
     *
     * @param b true to cancel
     */
    @Override public void setCancelled(boolean b) { this.cancelled_ = b; }
}
