package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


class AbilityListener<E extends Event> {

    private static final Listener voidListener_ = new Listener(){};
    private final AbilityHandler abilityHandler_;
    private final AbilityList<E> abilities_ = new AbilityList<>();
    final EventPriority priority;

    AbilityListener(Class<E> eventClass, AbilityHandler abilityHandler, EventPriority priority) {
        this.abilityHandler_ = abilityHandler;
        this.priority = priority;
        Bukkit.getServer().getPluginManager().registerEvent(
                eventClass,
                voidListener_,
                priority,
                (l,e) -> {
                    try {
                        E event = (E) e;
                        this.onEvent(event);
                     } catch (ClassCastException ignored) {}
                },
                RegionsLib.getMain(),
                true
        );
    }

    void add(Ability<E> ability) {
        abilities_.add(ability);
    }
    void remove(Ability<E> ability) {
        if (this.abilities_.contains(ability)) {
            this.abilities_.remove(ability);
        }
    }

    private void onEvent(E e) {
        //Validating all registered abilities
        LinkedList<Ability<E>> abilities = new LinkedList<>(this.abilities_.prioritize());
        LinkedList<Ability<?>> toRemove = new LinkedList<>();
        abilities.removeIf(a -> {
            boolean r = toRemove.contains(a) || !a.isValid(e);
            if (r) { toRemove.addAll(a.getSubAbilities().toList()); }
            return r;
        });

        //Return if there's no valid abilities
        if (abilities.isEmpty()) { return; }

        //Firing the priority top most ability
        Ability<E> definitive = abilities.getLast();
        boolean allowed = definitive.fire(e);
        if (allowed) { definitive.invalidateTargets(e); }

        //Logging
        Player player = definitive.playerGetter.apply(e);
        this.log_(player.getName() + " fired the '" + definitive.getName() + "' ability. " + (allowed ? "A" : "Not A") + "llowed.",Level.FINER);
        if (abilities.size() > 2) { return; }
        this.log_("   Anility stack:", Level.FINEST);
        for (int i = 0; i < (abilities.size() - 1); i++) {
            this.log_("     - " + abilities.get(i).getName(), Level.FINEST);
        }
        this.log_("     >> " + definitive.getName(), Level.FINEST);
    }

    private void log_(String log, Level level) {
        RegionsLib.getMain().getLogger().log(level, log);
    }
}
