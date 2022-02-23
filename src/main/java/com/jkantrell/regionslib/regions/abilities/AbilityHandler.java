package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityHandler {

    //Field
    private final AbilityList abilities_ = new AbilityList<>();
    private final AbilityListenerList listenerList_ = new AbilityListenerList();
    private final HashMap<Class<? extends Event>, AbilityListener<? extends Event>> listeners_ = new HashMap<>();

    //GETTERS
    public AbilityList<? extends Event> getRegisteredAbilities () {
        return this.abilities_.clone();
    }
    public List<AbilityListener<? extends Event>> getRegisteredListeners() {
        return this.listenerList_.toList();
    }

    public <E extends Event> void register(Ability<E> ability) {
        if (! ability.registrable) {
            RegionsLib.getMain().getLogger().warning(
            "Ability " + ability.name + " wasn't registered, as it's been marked as unregistrable!"
            );
            return;
        }
        StringBuilder log = new StringBuilder();
        log.append("Registering new Ability.\nName: ")
                .append(ability.name)
                .append("\nBukkit priority: ")
                .append(ability.getBukkitPriority().toString())
                .append("\nEvent: ")
                .append(ability.eventClass.toString()).append("\n");
        try {
            AbilityListener<E> listener = (AbilityListener<E>) this.listenerList_.get(ability.eventClass,ability.getBukkitPriority());
            if (listener == null) {
                log.append("No listener for this event. Creating a new one.");
                listener = new AbilityListener<E>(ability.eventClass,this,ability.getBukkitPriority());
                this.listenerList_.add(ability.eventClass,listener);
            } else {
                log.append("Adding to existing listener");
            }
            listener.add(ability);
            this.abilities_.add(ability);
        } catch (Exception e) {
            log.append("Unable to register. ").append(e.getMessage());
        }
        for (String s : StringUtils.split(log.toString(),"\n")) {
            RegionsLib.getMain().getLogger().info(s);
        }
    }

    public void unregister(Ability<?> ability) {
        for (AbilityListener<? extends Event> listener : this.listenerList_.toList()) {
            if (listener.abilities.removeIf(a -> a.name.equals(ability.name))) { break; }
        }
        this.abilities_.remove(ability.name);
    }
}
