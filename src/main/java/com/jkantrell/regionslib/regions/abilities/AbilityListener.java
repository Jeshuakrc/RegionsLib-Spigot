package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.event.*;


class AbilityListener<E extends Event> {

    private static final Listener voidListener_ = new Listener(){};
    private final AbilityHandler abilityHandler_;
    final AbilityList<E> abilities = new AbilityList<>();
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
        abilities.add(ability);
    }

    private void onEvent(E e) {

        boolean cancel = false;
        AbilityList<E> validAbilities = this.abilities.getRemovedIf(a -> !a.isValid(e));
        for (Ability<E> ability : validAbilities.prioritize()) {
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
