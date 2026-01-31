package com.kntrel.mc.regionLib.trigger;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.util.tuple.Pair;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public abstract class ListenerRegistry<T extends RegionTrigger<? extends Event>, L extends RegionListener<? extends T>> {

    //ASSETS
    private static final Listener VOID_LISTENER = new Listener(){};


    //FIELDS
    protected final RegionContext context_;
    protected final Plugin plugin_;
    protected final Map<String, L> keyMap_;
    protected final Map<EventKey, Set<TriggerKey<T>>> eventMap_;


    // CONSTRUCTORS
    public ListenerRegistry(RegionContext context) {
        this.context_ = context;
        this.plugin_ = this.context_.getPlugin();
        this.keyMap_ = new HashMap<>();
        this.eventMap_ = new HashMap<>();
    }


    // UTILITY
    public Map<String, L> getAllByKey() {
        return Map.copyOf(this.keyMap_);
    }
    public List<L> getAll() {
        return List.copyOf(this.keyMap_.values());
    }
    public Optional<L> get(String key) {
        return Optional.ofNullable(this.keyMap_.get(key));
    }
    public boolean exists(String key) {
        return this.keyMap_.containsKey(key);
    }
    public void register(L newElm) {
        if (this.keyMap_.containsKey(newElm.name())) {
            throw new IllegalStateException("Entry '" + newElm.name() + "' is already registered.");
        }
        this.keyMap_.put(newElm.name(), newElm);
        newElm.triggers().forEach(trg -> this.registerTrigger(newElm, trg));
    }


    //HANDLE PIPELINE
    protected void handle(Event event, EventKey eventKey) {
        Set<TriggerKey<T>> keys = this.eventMap_.get(eventKey);
        if (keys == null) { return; }
        List<Pair<L, T>> triggerEntries = new ArrayList<>();
        for (TriggerKey<T> tk : keys) {
            L listener = this.keyMap_.get(tk.listenerName());
            if (listener == null) {
                throw new IllegalStateException("No TriggerListener registered with name '" + tk.listenerName() + "'");
            }
            triggerEntries.add(Pair.of(listener, tk.trigger()));
        }
        this.handle(triggerEntries, event);
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handle(List<Pair<L, T>> triggerEntries, Event event) {
        for (Pair<L, T> lt : triggerEntries) {
            RegionTrigger trg = lt.second();
            if (trg.appliesTo(event)) { return; }

            L listener = lt.first();
            Bounds loc = trg.localize(event);
            if (loc == null) { return; }

            RegionReadRepository repo = this.context_.getHotRegionRepository();
            List<Region> regions = (loc.isArea())
                    ? repo.getIn(loc.getArea())
                    : repo.getAt(loc.getPoint());

            this.handle(regions, event, listener, lt.second());
        }
    }
    protected abstract void handle(List<Region> regions, Event event, L listener, T trigger);


    //HELPERS
    private void registerTrigger(L listener, T trigger) {
        TriggerKey<T> tk = new TriggerKey<>(listener.name(), trigger);
        EventKey eventKey = new EventKey(trigger.eventClass(), trigger.bukkitPriority());

        if (!this.eventMap_.containsKey(eventKey)) {
            this.eventMap_.put(eventKey, new TreeSet<>());  // Keeps the reactors sorted by priority

            this.plugin_.getServer().getPluginManager().registerEvent(
                    eventKey.eventClass(),
                    VOID_LISTENER,
                    eventKey.priority(),
                    (l, e) -> this.handle(e, eventKey),
                    this.plugin_,
                    true
            );
        }

        this.eventMap_.get(eventKey).add(tk);
    }


    //SUBTYPES
    protected record EventKey(Class<? extends Event> eventClass, EventPriority priority) {
        @Override public String toString() {
            return eventClass().getSimpleName() + "::" + priority().name();
        }

    }

    protected record TriggerKey<T extends RegionTrigger<?>>(String listenerName, T trigger) implements Comparable<TriggerKey<?>> {
        @Override public int compareTo(@NotNull TriggerKey<?> o) {
            return this.trigger().compareTo(o.trigger());
        }
    }
}
