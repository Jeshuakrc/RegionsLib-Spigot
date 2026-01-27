package com.kntrel.mc.regionLib.region.react;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.mc.regionLib.util.AreaGetter;
import com.kntrel.mc.regionLib.util.PointGetter;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class RegionEventReactor implements Comparable<RegionEventReactor> {


    //FIELDS
    protected final String name_;
    protected final Class<? extends Event> eventClass_;
    protected final Predicate<Event> validator_;
    protected final int priority_;
    protected final EventPriority bukkitPriority_;
    protected Function<Event, Location> pointGetter_;
    protected Function<Event, Area> areaGetter_;


    //CONSTRUCTORS
    private RegionEventReactor(String name, Class<? extends Event> eventClass, Predicate<Event> validator, int priority, EventPriority bukkitPriority) {
        this.name_ = name;
        this.eventClass_ = eventClass;
        this.validator_ = validator;
        this.priority_ = priority;
        this.bukkitPriority_ = (bukkitPriority == null) ? EventPriority.NORMAL : bukkitPriority;
        this.pointGetter_ = null;
        this.areaGetter_ = null;
    }
    protected RegionEventReactor(String name, Class<? extends Event> eventClass, PointGetter pointGetter, Predicate<Event> validator, int priority, EventPriority bukkitPriority) {
        this(name, eventClass,validator, priority, bukkitPriority);
        this.pointGetter_ = pointGetter;
    }
    protected RegionEventReactor(String name, Class<? extends Event> eventClass, AreaGetter areaGetter, Predicate<Event> validator, int priority, EventPriority bukkitPriority) {
        this(name, eventClass,validator, priority, bukkitPriority);
        this.areaGetter_ = areaGetter;
    }


    //GETTERS
    public String getName() { return name_; }
    public int getPriority() { return priority_; }
    public Class<? extends Event> getEventClass() { return eventClass_; }
    public Predicate<Event> getValidator() { return validator_; }
    public EventPriority getBukkitPriority() { return bukkitPriority_; }
    private int getBukkitPrioritySlot() {
        return this.bukkitPriority_.getSlot();
    }
    public Optional<Function<Event, Location>> getPointGetter() {
        return Optional.ofNullable(this.pointGetter_);
    }
    public Optional<Function<Event, Area>> getAreaGetter() {
        return Optional.ofNullable(this.areaGetter_);
    }
    public boolean isPointBased() { return this.pointGetter_ != null; }
    public boolean isAreaBased() { return this.areaGetter_ != null; }


    //UTILITY
    public boolean appliesTo(Event event) {
        return this.validator_.test(event);
    }
    public abstract void fire(Event event, RegionContext context);


    //UTIL
    protected List<Region> resolveRegions(Event event, RegionContext context) {
        Location l = (pointGetter_ != null) ? pointGetter_.apply(event) : null;
        Area a = (areaGetter_ != null) ? areaGetter_.apply(event) : null;
        if (l == null && a == null) return Collections.emptyList();

        RegionReadRepository repo = context.getRegionRepository();
        return isPointBased() ? repo.getAt(l) : repo.getIn(a);
    }
    @Override public int compareTo(@NotNull RegionEventReactor o) {
        return Comparator.comparingInt(RegionEventReactor::getBukkitPrioritySlot)
                .thenComparingInt(RegionEventReactor::getPriority)
                .thenComparing(RegionEventReactor::getName)
                .compare(this, o);
    }
}
