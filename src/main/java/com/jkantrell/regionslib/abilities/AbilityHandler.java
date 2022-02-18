package com.jkantrell.regionslib.abilities;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.HashMap;
import java.util.Map;

public class AbilityHandler {

    //Field
    private final AbilityList abilities_ = new AbilityList<>();
    private final HashMap<Class<? extends Event>, AbilityListener<? extends Event>> listeners_ = new HashMap<>();

    //GETTERS
    public AbilityList<? extends Event> getRegisteredAbilities () {
        return this.abilities_.clone();
    }

    public <E extends Event> void register(Ability<E> ability, Class<E> eventClass) {
        AbilityListener<E> listener;
        if (this.listeners_.containsKey(eventClass)) {
            listener = (AbilityListener<E>) this.listeners_.get(eventClass);
        } else {
            listener = new AbilityListener<E>();
            this.listeners_.put(eventClass,listener);
        }
        listener.add(ability);
        this.abilities_.add(ability);
    }

    public void unregister(Ability<?> ability) {
        AbilityListener<? extends Event> listener = null;
        Class<? extends Event> clazz = null;
        for (Map.Entry<Class<? extends Event>,AbilityListener<? extends Event>> entry : this.listeners_.entrySet()) {
            listener = entry.getValue();
            clazz = entry.getKey();
            if (listener.abilities.removeIf(a -> a.name.equals(ability.name))) { break; }
        }
        this.abilities_.remove(ability.name);
        if (listener == null) { return; }
        if (listener.abilities.isEmpty()) {
            HandlerList.unregisterAll(listener);
            this.listeners_.remove(clazz);
        }
    }
}
