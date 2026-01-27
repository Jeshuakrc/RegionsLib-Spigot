package com.kntrel.mc.regionLib.region.react;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.stream.Collectors;

public abstract class EventReactorRegistry<T extends RegionEventReactor> {

    //ASSETS
    private static final Listener VOID_LISTENER = new Listener(){};


    //FIELDS
    protected final RegionContext context_;
    protected final Plugin plugin_;
    protected final Map<String, T> keyMap_;
    protected final Map<EventKey, Set<T>> eventMap_;


    // CONSTRUCTORS
    public EventReactorRegistry(RegionContext context) {
        this.context_ = context;
        this.plugin_ = this.context_.getPlugin();
        this.keyMap_ = new HashMap<>();
        this.eventMap_ = new HashMap<>();
    }


    // UTILITY
    public Map<Class<? extends Event>, List<T>> getAllByEvent() {
        return this.keyMap_.values().stream().collect(Collectors.groupingBy(T::getEventClass));
    }
    public Map<String, T> getAllByKey() {
        return Map.copyOf(this.keyMap_);
    }
    public List<T> getAll() {
        return List.copyOf(this.keyMap_.values());
    }
    public Optional<T> get(String key) {
        return Optional.ofNullable(this.keyMap_.get(key));
    }
    public List<T> get(Class<? extends Event> eventClass) {
        return this.eventMap_.entrySet().stream()
                .filter(e -> e.getKey().eventClass().equals(eventClass))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }
    public List<T> get(Class<? extends Event> eventClass, EventPriority priority) {
        Set<T> l = this.eventMap_.get(new EventKey(eventClass, priority));
        if (l == null) { return Collections.emptyList(); }
        return List.copyOf(l);
    }
    public boolean exists(String key) {
        return this.keyMap_.containsKey(key);
    }
    public void register(T newElm) {
        if (this.keyMap_.containsKey(newElm.getName())) {
            throw new IllegalStateException("Entry '" + newElm.getName() + "' is already registered.");
        }
        this.keyMap_.put(newElm.getName(), newElm);


        EventKey eventKey = new EventKey(newElm.getEventClass(), newElm.getBukkitPriority());
        if (!this.eventMap_.containsKey(eventKey)) {
            this.eventMap_.put(eventKey, new TreeSet<>());  // Keeps the reactors sorted by priority

            this.plugin_.getServer().getPluginManager().registerEvent(
                    eventKey.eventClass(),
                    VOID_LISTENER,
                    eventKey.priority(),
                    (l,e) -> { try { this.onEvent(e, eventKey); } catch (ClassCastException ignored) {} },
                    this.plugin_,
                    true
            );
        }

        this.eventMap_.get(eventKey).add(newElm);
    }

    @SuppressWarnings("rawtypes")
    public abstract  <E extends Event> RegionEventReactorBuilder registerOn(Class<E> eventClass);


    //IMPLEMENTATION
    protected abstract void onEvent(Event event, EventKey eventKey);


    //CLASSES
    protected record EventKey(Class<? extends Event> eventClass, EventPriority priority) {
        @Override public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o == this) { return true; }
            if (!(o instanceof EventKey other)) { return false; }
            return other.eventClass.equals(this.eventClass) && other.priority.equals(this.priority);
        }
        @Override public int hashCode() {
            return (this.eventClass.hashCode() * 10) + this.priority.hashCode();
        }
    }
}