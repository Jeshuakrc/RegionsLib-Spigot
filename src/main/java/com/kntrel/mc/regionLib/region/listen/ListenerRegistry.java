package com.kntrel.mc.regionLib.region.listen;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.util.SetMap;
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
    protected final SetMap<EventKey, TriggerKey<T, L>> eventMap_;


    // CONSTRUCTORS
    public ListenerRegistry(RegionContext context) {
        this.context_ = context;
        this.plugin_ = this.context_.getPlugin();
        this.keyMap_ = new HashMap<>();
        this.eventMap_ = new SetMap<>(TreeSet::new); // Keeps the reactors sorted by priority
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
        TreeSet<TriggerKey<T, L>> keys = (TreeSet<TriggerKey<T,L>>) this.eventMap_.get(eventKey);
        if (keys == null) { return; }
        this.handle(keys, event, eventKey.priority());
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handle(SequencedCollection<? extends Pair<L, T>> triggerEntries, Event event, EventPriority priority) {
        for (Pair<L, T> lt : triggerEntries) {
            RegionTrigger trg = lt.second();
            try {
                if (!trg.appliesTo(event)) { continue; }
            } catch (Throwable e) {
                this.context_.getServer().getLogger().severe("Trigger in listener'" + lt.first().name() + "' failed to validate. Event: " + event.getClass().getSimpleName() + ". Falling back as non-applicable.");
                this.context_.getServer().getLogger().severe("Caused by: " + e);
                continue;
            }

            L listener = lt.first();
            Bounds loc = trg.localize(event);
            if (loc == null) { continue; }

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
        TriggerKey<T, L> tk = new TriggerKey<>(listener, trigger);
        EventKey eventKey = new EventKey(trigger.eventClass(), trigger.bukkitPriority());

        if (!this.eventMap_.containsKey(eventKey)) {
            this.plugin_.getServer().getPluginManager().registerEvent(
                    eventKey.eventClass(),
                    VOID_LISTENER,
                    eventKey.priority(),
                    (l, e) -> this.handle(e, eventKey),
                    this.plugin_,
                    true
            );
        }

        this.eventMap_.putInto(eventKey, tk);
    }


    //SUBTYPES
    protected record EventKey(Class<? extends Event> eventClass, EventPriority priority) {
        public EventKey(Class<? extends Event> eventClass, EventPriority priority) {
            this.eventClass = eventClass;
            this.priority = priority;
        }

        @Override public String toString() {
            return eventClass().getSimpleName() + "::" + priority().name();
        }

    }

    protected record TriggerKey<T extends RegionTrigger<?>, L extends RegionListener<? extends T>>(L listener, T trigger) implements Comparable<TriggerKey<?, ?>>, Pair<L, T> {
        @Override public int compareTo(@NotNull TriggerKey<?, ?> o) {
            return this.trigger().compareTo(o.trigger());
        }
        @Override public L first() { return this.listener(); }
        @Override public T second() { return this.trigger(); }
    }
}
