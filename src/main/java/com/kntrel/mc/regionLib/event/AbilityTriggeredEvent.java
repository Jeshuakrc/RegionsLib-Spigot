package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

/**
 * Fired when a region ability is triggered for a player.
 */
public class AbilityTriggeredEvent extends PlayerEvent {

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
    /**
     * Creates an ability trigger event for a location-based trigger.
     *
     * @param ability triggered ability
     * @param who player involved
     * @param allowed whether the ability is allowed
     * @param region region in which the ability triggered
     * @param where location of the trigger
     * @param event triggering Bukkit event
     */
    public AbilityTriggeredEvent(Ability ability, Player who, boolean allowed, Region region, Location where, Event event) {
        this(ability, who, allowed, region, event);
        this.location_ = where;
    }
    /**
     * Creates an ability trigger event for an area-based trigger.
     *
     * @param ability triggered ability
     * @param who player involved
     * @param allowed whether the ability is allowed
     * @param region region in which the ability triggered
     * @param where area of the trigger
     * @param event triggering Bukkit event
     */
    public AbilityTriggeredEvent(Ability ability, Player who, boolean allowed, Region region, Area where, Event event) {
        this(ability, who, allowed, region, event);
        this.area_ = where;
    }

    //Getters
    /**
     * Returns the region where the ability triggered.
     *
     * @return region instance
     */
    public Region getRegion() {
        return region_;
    }
    /**
     * Returns the triggered ability.
     *
     * @return ability instance
     */
    public Ability getAbility() {
        return ability_;
    }
    /**
     * Returns a representative location for the trigger.
     *
     * @return trigger location
     */
    public Location getLocation() {
        return (this.location_ != null) ? this.location_ : this.area_.middle();
    }
    /**
     * Returns the trigger area if available.
     *
     * @return optional trigger area
     */
    public Optional<Area> getArea() {
        return Optional.ofNullable(this.area_);
    }
    /**
     * Returns the underlying Bukkit event that triggered this ability.
     *
     * @return triggering event
     */
    public Event getTriggererEvent() {
        return event_;
    }
    /**
     * Returns whether the ability is currently allowed.
     *
     * @return true if allowed
     */
    public boolean isAllowed() {
        return allowed_;
    }


    //SETTERS
    /**
     * Updates whether the ability should be allowed.
     *
     * @param allowed true if allowed
     */
    public void setAllowed(boolean allowed) {
        this.allowed_ = allowed;
    }
}
