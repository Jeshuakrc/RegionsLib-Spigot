package com.kntrel.mc.regionLib.region.react;

import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.mc.regionLib.util.AreaGetter;
import com.kntrel.mc.regionLib.util.PointGetter;
import io.avaje.lang.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class RegionEventReactorBuilder<E extends Event ,T extends RegionEventReactor, B extends RegionEventReactorBuilder<E, T, B>> {

    // Fields
    protected final Class<E> eventClass_;
    protected final B instance_;
    protected String name_;
    protected Predicate<E> validator_;
    protected Function<E, Location> pointGetter_;
    protected Function<E, Area> areaGetter_;
    protected EventPriority bukkitPriority_;
    protected Integer priority_;


    // CONSTRUCTORS
    @SuppressWarnings("unchecked")
    protected RegionEventReactorBuilder(Class<E> eventClass) {
        this.eventClass_ = eventClass;
        this.instance_ = (B) this;
    }
    protected RegionEventReactorBuilder(RegionEventReactorBuilder<E, ?, ?> other) {
        this(other.eventClass_);
        this.name_ = other.name_;
        this.validator_ = other.validator_;
        this.pointGetter_ = other.pointGetter_;
        this.areaGetter_ = other.areaGetter_;
        this.bukkitPriority_ = other.bukkitPriority_;
        this.priority_ = other.priority_;
    }


    // CHAINED CONFIG
    public B called(String name) {
        this.name_ = name;
        return this.instance_;
    }
    public B when(Predicate<E> validator) {
        this.validator_ = validator;
        return this.instance_;
    }
    public B at(Function<E, Location> pointGetter) {
        this.pointGetter_ = pointGetter;
        this.areaGetter_ = null;
        return this.instance_;
    }
    public B in(Function<E, Area> areaGetter) {
        this.areaGetter_ = areaGetter;
        this.pointGetter_ = null;
        return this.instance_;
    }
    public B prioritize(int priority) {
        this.priority_ = priority;
        return this.instance_;
    }
    public B prioritize(EventPriority priority) {
        this.bukkitPriority_ = priority;
        return this.instance_;
    }
    public B prioritize(int priority, EventPriority eventPriority) {
        this.priority_ = priority;
        this.bukkitPriority_ = eventPriority;
        return this.instance_;
    }


    //GETTERS
    protected String getName() {
        return this.name_;
    }
    protected Predicate<Event> getValidator() {
        if (this.validator_ == null) {
            this.validator_ = e -> true;
        }
        return e -> {
            if (!this.eventClass_.isAssignableFrom(e.getClass())) { return false; }
            E cast = this.eventClass_.cast(e);
            return this.validator_.test(cast);
        };
    }
    protected PointGetter getPointGetter() {
        if (this.areaGetter_ != null) { return null; }
        if (this.pointGetter_ == null && EntityEvent.class.isAssignableFrom(this.eventClass_)) {
            this.pointGetter_ = e -> ((EntityEvent) e).getEntity().getLocation();
        }
        if (this.pointGetter_ != null) {
            return e -> this.pointGetter_.apply(this.eventClass_.cast(e));
        }
        throw new IllegalStateException("Cannot build ability. Unable to infer location");
    }
    protected AreaGetter getAreaGetter() {
        if (this.pointGetter_ != null) { return null; }
        if (this.areaGetter_ == null && BlockEvent.class.isAssignableFrom(this.eventClass_)) {
            this.areaGetter_ = e -> {
                BlockEvent be = (BlockEvent) e;
                return new Area(be.getBlock().getBoundingBox(), be.getBlock().getWorld());
            };
        }
        if (this.areaGetter_ == null && PlayerInteractEvent.class.isAssignableFrom(this.eventClass_)) {
            this.areaGetter_ = e -> {
                Block b = ((PlayerInteractEvent) e).getClickedBlock();
                if (b == null) return null;
                return new Area(b.getBoundingBox(), b.getWorld());
            };
        }
        if (this.areaGetter_ != null) {
            return e -> this.areaGetter_.apply(this.eventClass_.cast(e));
        }
        throw new IllegalStateException("Cannot build ability. Unable to infer location");
    }


    //FINAL OPERATIONS
    public <N extends Enum<N>> EnumBuilder<E, N, T> withEnum(Function<E, N> instanceGetter) {
        return new EnumBuilder<>(this, instanceGetter);
    }
    public T build() {
        return this.build(null);
    }
    protected abstract T build(@Nullable Predicate<E> extraCheck);

}
