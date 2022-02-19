package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Bukkit;
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

    public <E extends Event> void register(Ability<E> ability) {
        this.abilities_.add(ability);

        AbilityListener<E> listener;
        RegionsLib.getMain().getLogger().info("Registering new Ability. Event: " + ability.eventClass.toString());
        if (this.listeners_.containsKey(ability.eventClass)) {
            RegionsLib.getMain().getLogger().info("Adding to existing listener");
            listener = (AbilityListener<E>) this.listeners_.get(ability.eventClass);
        } else {
            RegionsLib.getMain().getLogger().info("No listener for this event. Creating a new one.");
            listener = new AbilityListener<E>(ability.eventClass);
            this.listeners_.put(ability.eventClass,listener);
        }
        listener.add(ability);
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
            this.listeners_.remove(clazz);
        }
    }
}
