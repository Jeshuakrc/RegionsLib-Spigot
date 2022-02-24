package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.bukkit.Bukkit;
import org.bukkit.event.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


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
                (l,e) -> onEvent((E) e),
                RegionsLib.getMain(),
                false
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

        boolean cancel = false;
        List<Ability<E>> validAbilities = this.abilities_.prioritize();
        List<Ability<?>> toRemove = new ArrayList<>();
        Iterator<Ability<E>> iterator = validAbilities.iterator();
        while (iterator.hasNext()) {
            Ability<E> ability = iterator.next();
            if (!ability.isValid(e) || toRemove.contains(ability)) {
                iterator.remove();
                AbilityList<E> subAbilities = Ability.getSubAbilities(ability);
                if (subAbilities != null) {
                    toRemove.addAll(subAbilities.toList());
                }
            }
        }
        for (Ability<E> ability : validAbilities) {
            RegionsLib.getMain().getLogger().info("Ability " + ability.name + " is valid in this context.");
            cancel = !ability.isAllowed(e);
            RegionsLib.getMain().getLogger().info((cancel) ? "not allowed" : "allowed");
            ability.invalidateTargets(e);
        }
        if (cancel) {
            if (e instanceof Cancellable toCancel) {
                RegionsLib.getMain().getLogger().info("Cancelling " + e.getClass().getName());
                toCancel.setCancelled(true);
            }
        }
    }
}
