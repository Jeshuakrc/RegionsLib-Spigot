package com.kntrel.mc.regionLib.region.listen.build;

import com.kntrel.mc.regionLib.region.listen.Place;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.region.listen.RegionListener;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.util.Priority;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Base builder for region listeners.
 *
 * @param <E> event type
 * @param <T> trigger type
 * @param <L> listener type
 * @param <B> builder type
 */
public abstract class ListenerBuilder<
        E extends Event,
        T extends RegionTrigger<?>,
        L extends RegionListener<? extends T>,
        B extends ListenerBuilder<E, T, L, B>
> {

    //LISTENER MEMBERS
    protected final Set<T> triggers_;


    //CURRENT TRIGGER MEMBERS
    protected final Class<E> eventClass_;
    protected final B instance_;
    protected Function<E, Place> localizer_;
    protected Predicate<E> validator_ = e -> true;
    protected EventPriority bukkitPriority_ = EventPriority.NORMAL;
    protected Priority priority_ = Priority.NORMAL;


    // CONSTRUCTORS
    @SuppressWarnings("unchecked")
    protected ListenerBuilder(Class<E> eventClass, Set<T> existingTriggers) {
        this.eventClass_ = eventClass;
        this.instance_ = (B) this;
        this.triggers_ = new HashSet<>(existingTriggers);
    }

    protected ListenerBuilder(Class<E> eventClass) {
        this(eventClass, new HashSet<>());
    }


    // CHAINED CONFIG
    public B when(Predicate<E> validator) {
        this.validator_ = validator;
        return this.instance_;
    }
    public B at(Function<E, Location> pointGetter) {
        this.localizer_ = e -> Place.ofPoint(pointGetter.apply(e));
        return this.instance_;
    }
    public B in(Function<E, Area> areaGetter) {
        this.localizer_ = e -> Place.ofArea(areaGetter.apply(e));
        return this.instance_;
    }
    public B prioritize(Priority priority) {
        this.priority_ = priority;
        return this.instance_;
    }
    public B prioritize(int priority) {
        return this.prioritize(Priority.of(priority));
    }
    public B prioritize(EventPriority priority) {
        this.bukkitPriority_ = priority;
        return this.instance_;
    }
    public B prioritize(Priority priority, EventPriority eventPriority) {
        this.priority_ = priority;
        this.bukkitPriority_ = eventPriority;
        return this.instance_;
    }
    public B prioritize(int priority, EventPriority eventPriority) {
        return this.prioritize(Priority.of(priority), eventPriority);
    }
    public <N extends Enum<N>> EnumTriggerBuilder<E, N, B> withEnum(Function<E, N> instanceGetter) {
        return new EnumTriggerBuilder<>(this.instance_, instanceGetter);
    }
    public <E2 extends Event> ListenerBuilder<E2, T, ? extends RegionListener<? extends T>, ?> alsoOn(Class<E2> eventClass) {
        this.triggers_.add(this.buildTrigger());
        return this.next(eventClass, this.triggers_);
    }


    //FINALIZER
    protected L build() {
        this.triggers_.add(this.buildTrigger());
        return this.buildListener(this.triggers_);
    }


    //GETTERS
    protected Function<E, Place> getLocalizer() {
        if (this.localizer_ != null) { return this.localizer_; }
        if (EntityEvent.class.isAssignableFrom(this.eventClass_)) {
            this.localizer_ = e -> {
                Entity entity = ((EntityEvent) e).getEntity();
                return Place.ofArea(Area.ofEntity(entity));
            };
            return this.localizer_;
        }
        if (BlockEvent.class.isAssignableFrom(this.eventClass_)) {
            this.localizer_ = e -> {
                BlockEvent be = (BlockEvent) e;
                return Place.ofArea(Area.ofBlock(be.getBlock()));
            };
            return this.localizer_;
        }
        if (PlayerInteractEvent.class.isAssignableFrom(this.eventClass_)) {
            this.localizer_ = e -> {
                Block b = ((PlayerInteractEvent) e).getClickedBlock();
                if (b == null) return null;
                return Place.ofArea(new Area(b.getBoundingBox(), b.getWorld()));
            };
            return this.localizer_;
        }
        if (PlayerEvent.class.isAssignableFrom(this.eventClass_)) {
            this.localizer_ = e -> {
                Location loc = ((PlayerEvent) e).getPlayer().getLocation();
                return Place.ofPoint(loc);
            };
            return this.localizer_;
        }
        throw new IllegalStateException("Cannot build trigger. Unable to infer location");
    }



    //CONTRACT
    protected abstract T buildTrigger();
    protected abstract L buildListener(Set<T> triggers);
    protected abstract <E2 extends Event> ListenerBuilder<E2, T, ? extends RegionListener<? extends T>, ?> next(Class<E2> eventClass, Set<T> existingTriggers);

}
