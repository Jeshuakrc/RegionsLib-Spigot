package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.Event;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

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
        if (!RegionsLib.isInitialized()) {
            RegionsLib.addPostEnableTask(() -> this.register(ability));
            return;
        }
        if (! ability.registrable) {
            RegionsLib.getMain().getLogger().warning(
            "Ability " + ability.getName() + " wasn't registered, as it's been marked as unregistrable!"
            );
            return;
        }
        if (ability.getName() == null) {
            RegionsLib.getMain().getLogger().warning(
            "Unable to register ability as it has no name!"
            );
            return;
        }
        StringBuilder log = new StringBuilder();
        Level logLevel = Level.FINE;
        log.append("Registering new Ability.\n - Name: ")
                .append(ability.getName())
                .append("\n - Bukkit priority: ")
                .append(ability.getBukkitPriority().toString())
                .append("\n - Event: ")
                .append(ability.eventClass.getSimpleName()).append("\n - ");
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
            logLevel = Level.WARNING;
        }
        for (String s : StringUtils.split(log.toString(),"\n")) {
            RegionsLib.getMain().getLogger().log(logLevel,s);
        }
    }
    public <E extends Event> void unregister(Ability<E> ability) {
        if (!RegionsLib.isInitialized()) {
            RegionsLib.addPostEnableTask(() -> unregister(ability));
            return;
        }
        AbilityListener<?> genericListener = this.listeners_.get(ability.eventClass,ability.getBukkitPriority());
        if (genericListener != null) {
            AbilityListener<E> listener = (AbilityListener<E>) genericListener;
            listener.remove(ability);
        }
        this.abilities_.remove(ability.getName());
    }

    public void registerAll(Class<?> abilityHolder) {
        if (!RegionsLib.isInitialized()) {
            RegionsLib.addPostEnableTask(() -> this.registerAll(abilityHolder));
            return;
        }
        int i = 0;
        for (Field field : abilityHolder.getFields()) {
            if (!field.isAnnotationPresent(AbilityRegistration.class)) { continue; }

            try {
                Object obj = field.get(null);
                if (obj instanceof Ability ability) {
                    if (ability.getName() == null) {
                        ability.setName(field.getName());
                    }
                    this.register(ability);
                    i++;
                } else {
                    RegionsLib.getMain().getLogger().severe("The " + field.getName() + " field is not of Ability type. Unable to register.");
                }
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        RegionsLib.getMain().getLogger().info(i + " abilities registered from class " + abilityHolder.getSimpleName());
    }
}
