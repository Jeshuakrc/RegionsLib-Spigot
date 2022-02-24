package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AbilityHandler {

    //Field
    private final AbilityList abilities_ = new AbilityList<>();
    private final AbilityListenerList listeners_ = new AbilityListenerList();
    private final HashMap<Ability<? extends Event>,ArrayList<Ability<? extends Event>>> extensions_ = new HashMap<>();

    //GETTERS
    public AbilityList<? extends Event> getRegisteredAbilities () {
        return this.abilities_.clone();
    }
    public List<AbilityListener<? extends Event>> getRegisteredListeners() {
        return this.listeners_.toList();
    }
    public boolean isRegistered(Ability<? extends Event> ability) {
        return this.abilities_.contains(ability);
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
            AbilityListener<E> listener = (AbilityListener<E>) this.listeners_.get(ability.eventClass,ability.getBukkitPriority());
            if (listener == null) {
                log.append("No listener for this event. Creating a new one.");
                listener = new AbilityListener<E>(ability.eventClass,this,ability.getBukkitPriority());
                this.listeners_.add(ability.eventClass,listener);
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
    public <E extends Event> void unregister(Ability<E> ability) {
        AbilityListener<?> genericListener = this.listeners_.get(ability.eventClass,ability.getBukkitPriority());
        if (genericListener != null) {
            AbilityListener<E> listener = (AbilityListener<E>) genericListener;
            listener.remove(ability);
        }
        this.abilities_.remove(ability.name);
    }
}
