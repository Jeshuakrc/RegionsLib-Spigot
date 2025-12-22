package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.event.AbilityTriggeredEvent;
import com.kntrel.mc.regionLib.io.Config;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.react.RegionEventReactor;
import com.kntrel.mc.regionLib.util.AreaGetter;
import com.kntrel.mc.regionLib.util.PointGetter;
import io.avaje.lang.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class Ability extends RegionEventReactor implements BiPredicate<Event, RegionContext> {

    //STATIC
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return AbilityBuilder.on(eventClass);
    }


    //FIELDS
    private final Function<Event, Player> playerGetter_;
    public final Ability extends_;


    //CONSTRUCTORS
    public Ability(@NotNull String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
        super(name, eventClass, pointGetter, validator, order, priority);
        this.playerGetter_ = playerGetter;
        this.extends_ = dependsOn;
    }
    public Ability(@NotNull String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
        super(name, eventClass, areaGetter, validator, order, priority);
        this.playerGetter_ = playerGetter;
        this.extends_ = dependsOn;
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter) {
        this(name, eventClass, validator, playerGetter, pointGetter, 0, null, null);
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter) {
        this(name, eventClass, validator, playerGetter, areaGetter, 0, null, null);
    }



    //GETTERS
    public Function<Event, Player> getPlayerGetter() {
        return this.playerGetter_;
    }
    public Optional<Ability> getSupperAbility() {
        return Optional.ofNullable(this.extends_);
    }


    //IMPLEMENTATION
    @Override public boolean test(Event event, RegionContext context) {
        if (!(event instanceof Cancellable cancellable)) { return true; }
        if (!this.validator_.test(event)) { return true; }

        Player p = this.playerGetter_.apply(event);
        if (p == null) { return true; }

        List<Region> regions = this.resolveRegions(event, context);
        if (regions.isEmpty()) { return true; }

        boolean[] results = new boolean[regions.size()];
        int i = 0;
        for (Region region : regions) {
            boolean allowed = region.checkAbility(p, this);
            AbilityTriggeredEvent e = this.isPointBased()
                    ? new AbilityTriggeredEvent(this, p, allowed, region, this.pointGetter_.apply(event), event)
                    : new AbilityTriggeredEvent(this, p, allowed, region, this.areaGetter_.apply(event), event);
            context.callEvent(e);
            results[i++] = e.isAllowed();
        }

        boolean r = overlappingPermissionCheck_(results, RegionLib.CONFIG.overlappingPermissionsMode);
        if (!cancellable.isCancelled()) { cancellable.setCancelled(!r); }
        return r;
    }
    @Override public void fire(Event event, RegionContext context) {
        this.test(event, context);
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof Ability other)) { return false; }
        return other.name_.equals(this.name_);
    }
    @Override public int hashCode() {
        return this.name_.hashCode();
    }


    //PRIVATE
    private static boolean overlappingPermissionCheck_(boolean[] bools, Config.OverlappingPermissionsMode mode) {
        return switch (mode) {
            case newest -> bools[0];
            case oldest -> bools[bools.length - 1];
            case all -> {
                for (boolean b : bools) {
                    if (!b) { yield false; }
                }
                yield true;
            }
            case any -> {
                for (boolean b : bools) {
                    if (b) { yield true; }
                }
                yield false;
            }
        };
    }
}
