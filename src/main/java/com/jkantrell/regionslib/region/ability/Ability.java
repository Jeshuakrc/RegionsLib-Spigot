package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.event.AbilityTriggeredEvent;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.util.Area;
import io.avaje.lang.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class Ability implements BiPredicate<Event, RegionContext>, Comparable<Ability> {


    //ASSETS
    public interface PointGetter extends Function<Event, Location> {}
    public interface AreaGetter extends Function<Event, Area> {}


    //STATIC
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return AbilityBuilder.on(eventClass);
    }
    public static final Comparator<Ability> COMPARATOR = Comparator
            .comparingInt((Ability a) -> a.getBukkitPriority().getSlot())
            .thenComparingInt(Ability::getPriority)
            .thenComparing(Ability::getName);


    //FIELDS
    private final String name_;
    private final Class<? extends Event> eventClass_;
    private final Predicate<Event> validator_;
    private final Function<Event, Player> playerGetter_;
    private final int priority_;
    private final EventPriority bukkitPriority_;
    public final Ability extends_;
    private Function<Event, Location> pointGetter_;
    private Function<Event, Area> areaGetter_;


    //CONSTRUCTORS
    private Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, int priority, @Nullable EventPriority bukkitPriority, @Nullable Ability extendsAbility) {
        this.name_ = name;
        this.eventClass_ = eventClass;
        this.validator_ = validator;
        this.playerGetter_ = playerGetter;
        this.bukkitPriority_ = (bukkitPriority == null) ? EventPriority.NORMAL : bukkitPriority;
        this.extends_ = extendsAbility;
        this.priority_ = priority;
        this.pointGetter_ = null;
        this.areaGetter_ = null;
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
        this(name, eventClass, validator, playerGetter, order, priority, dependsOn);
        this.pointGetter_ = pointGetter;
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
        this(name, eventClass, validator, playerGetter, order, priority, dependsOn);
        this.areaGetter_ = areaGetter;
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter) {
        this(name, eventClass, validator, playerGetter, pointGetter, 0, null, null);
    }
    public Ability(String name, Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter) {
        this(name, eventClass, validator, playerGetter, areaGetter, 0, null, null);
    }



    //GETTERS
    public String getName() {
        return this.name_;
    }
    public int getPriority() {
        return this.priority_;
    }
    public Class<? extends Event> getEventClass() {
        return this.eventClass_;
    }
    public Predicate<Event> getValidator() {
        return this.validator_;
    }
    public Function<Event, Player> getPlayerGetter() {
        return this.playerGetter_;
    }
    public EventPriority getBukkitPriority() {
        return this.bukkitPriority_;
    }
    public Optional<Ability> getSupperAbility() {
        return Optional.ofNullable(this.extends_);
    }
    public Optional<Function<Event, Location>> getPointGetter() {
        return Optional.ofNullable(this.pointGetter_);
    }
    public Optional<Function<Event, Area>> getAreaGetter() {
        return Optional.ofNullable(this.areaGetter_);
    }
    public boolean isAreaBased() {
        return this.areaGetter_ != null;
    }
    public boolean isPointBased() {
        return this.pointGetter_ != null;
    }


    //IMPLEMENTATION
    @Override public boolean test(Event event, RegionContext context) {
        return this.fire(event, context);
    }
    @Override
    public int compareTo(@Nonnull Ability o) {
        return COMPARATOR.compare(this, o);
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


    //UTILITY
    public boolean appliesTo(Event event) {
        if (!(event instanceof Cancellable cancellable)) { return true; }
        return this.validator_.test(event);
    }
    public boolean fire(Event event, RegionContext context) {
        if (!(event instanceof Cancellable cancellable)) { return true; }
        if (!this.validator_.test(event)) { return true; }

        Player p = this.playerGetter_.apply(event);
        Location l = (this.pointGetter_ != null) ? this.pointGetter_.apply(event) : null;
        Area a = (this.areaGetter_ != null) ? this.areaGetter_.apply(event) : null;

        if (p == null) { return true; }
        if (l == null && a == null) { return true; }

        List<Region> regions = this.isPointBased() ? context.getAt(l) : context.getIn(a);
        Collections.sort(regions);
        if (regions.isEmpty()) { return true; }

        boolean[] results = new boolean[regions.size()];
        int i = 0;
        for (Region region : regions) {
            boolean allowed = region.checkAbility(p, this);
            AbilityTriggeredEvent e = this.isPointBased()
                    ? new AbilityTriggeredEvent(this, p, allowed, region, l, event)
                    : new AbilityTriggeredEvent(this, p, allowed, region, a, event);
            context.callEvent(e);
            results[i++] = e.isAllowed();
        }

        boolean r = overlappingPermissionCheck_(results, RegionsLib.CONFIG.overlappingPermissionsMode);
        if (!cancellable.isCancelled()) { cancellable.setCancelled(!r); }
        return r;
    }


    //PRIVATE
    private static boolean overlappingPermissionCheck_(boolean[] bools, Config.OverlappingPermissionsMode mode) {
        return switch (mode) {
            case newest -> bools[0];
            case oldest -> bools[bools.length - 1];
            case all -> {
                for (boolean b : bools) {
                    if (!b) {
                        yield false;
                    }
                }
                yield true;
            }
            case any -> {
                for (boolean b : bools) {
                    if (b) {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }
}
