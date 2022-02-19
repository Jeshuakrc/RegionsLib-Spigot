package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;


class AbilityListener<E extends Event> implements EventExecutor {

    private static final Listener voidListener_ = new Listener(){};

    AbilityListener(Class<E> eventClass) {
        Bukkit.getServer().getPluginManager().registerEvent(
                eventClass,
                voidListener_,
                EventPriority.NORMAL,
                this,
                RegionsLib.getMain(),
                false
        );
    }

    final AbilityList<E> abilities = new AbilityList<>();

    public void add(Ability<E> ability) {
        abilities.add(ability);
    }

    public void onEvent(E e) {
        boolean cancel = false;
        AbilityList<E> validAbilities = this.abilities.clone();
        validAbilities.removeIf(a -> !a.isValid(e));
        for (Ability<E> ability : validAbilities.toList()) {
            cancel = ability.isAllowed(e);
        }
        if (e instanceof Cancellable toCancel) { toCancel.setCancelled(cancel); }
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        this.onEvent((E) event);
    }
}
