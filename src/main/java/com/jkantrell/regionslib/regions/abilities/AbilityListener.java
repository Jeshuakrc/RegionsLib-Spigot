package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class AbilityListener<E extends Event> implements Listener {

    AbilityListener () {
        Bukkit.getServer().getPluginManager().registerEvents(this, RegionsLib.getMain());
    }

    final AbilityList<E> abilities = new AbilityList<>();

    public void add(Ability<E> ability) {
        abilities.add(ability);
    }

    @EventHandler
    public void onEvent(E e) {
        boolean cancel = false;
        AbilityList<E> validAbilities = this.abilities.clone();
        validAbilities.removeIf(a -> !a.isValid(e));
        for (Ability<E> ability : validAbilities.toList()) {
            cancel = ability.isAllowed(e);
        }
        if (e instanceof Cancellable toCancel) { toCancel.setCancelled(cancel); }
    }
}
