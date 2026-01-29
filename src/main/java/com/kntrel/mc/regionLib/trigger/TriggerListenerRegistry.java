package com.kntrel.mc.regionLib.trigger;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.util.tuple.Pair;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public abstract class TriggerListenerRegistry<T extends RegionTrigger<? extends Event>, L extends TriggerListener<? extends T>> {

    //ASSETS
    private static final Listener VOID_LISTENER = new Listener(){};


    //FIELDS
    protected final RegionContext context_;
    protected final Plugin plugin_;
    protected final Map<String, L> keyMap_;
    protected final Map<EventKey, Set<TriggerKey<T>>> eventMap_;


    // CONSTRUCTORS
    public TriggerListenerRegistry(RegionContext context) {
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


    //CONTRACT
    protected void handle(List<Pair<L, T>> triggerEntries, Event event, EventPriority bukkitPriority) {
        for (Pair<L, T> lt : triggerEntries) {


            this.handle(Collections.emptyList(), event, lt.first(), lt.second(), bukkitPriority);
        }
    }
    protected abstract void handle(List<Region> regions, Event event, L listener, T trigger, EventPriority bukkitPriority);


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
                    new Handler<>(this, eventKey),
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

    protected record Handler<
        T extends RegionTrigger<?>,
        L extends TriggerListener<? extends T>
    >(TriggerListenerRegistry<T, L> registry, EventKey eventKey) implements EventExecutor {

        @Override
        public void execute(@NotNull Listener ignored, @NotNull Event event) throws EventException {
            Set<TriggerKey<T>> keys = this.registry().eventMap_.get(this.eventKey());
            if (keys == null) { return; }
            keys.forEach(tk -> this.handleTrigger(tk, event));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void handleTrigger(TriggerKey<T> tk, Event event) {
            RegionTrigger trg = tk.trigger();
            if (trg.appliesTo(event)) { return; }

            L listener = this.registry().keyMap_.get(tk.listenerName());
            if (listener == null) {
                throw new IllegalStateException("No TriggerListener registered with name '" + tk.listenerName() + "'");
            }

            Bounds loc = trg.localize(event);
            if (loc == null) { return; }

            RegionReadRepository repo = this.registry().context_.getHotRegionRepository();
            List<Region> regions = (loc.isArea())
                    ? repo.getIn(loc.getArea())
                    : repo.getAt(loc.getPoint());

            this.registry().handle(regions, event, listener, tk.trigger(), trg.bukkitPriority());
        }
    }

}
