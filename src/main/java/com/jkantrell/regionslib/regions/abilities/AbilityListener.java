package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import java.util.ArrayList;
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

        boolean allowed = false;
        List<Ability<E>> validAbilities = this.abilities_.prioritize();
        List<Ability<?>> toRemove = new ArrayList<>();
        Iterator<Ability<E>> iterator = validAbilities.iterator();
        StringBuilder log = new StringBuilder();

        while (iterator.hasNext()) {
            Ability<E> ability = iterator.next();
            if (toRemove.contains(ability) || !ability.isValid(e)) {
                iterator.remove();
                AbilityList<E> subAbilities = ability.getSubAbilities();
                toRemove.addAll(subAbilities.toList());
            }
        }
        if (!validAbilities.isEmpty()) {
            Player player = validAbilities.get(0).playerGetter.apply(e);
            log.append(player.getName()).append(" has fired abilities:\n");
            for (Ability<E> ability : validAbilities) {
                allowed = ability.fire(e);
                ability.invalidateTargets(e);
                log.append("     ").append(ability.getName()).append(" - ").append(allowed ? "A" : "Not a").append("llowed").append("\n");
            }
            log.append("     ").append(allowed ? "Keeping" : "Cancelling").append(" event ").append(e.getClass().getSimpleName());

            for (String s : StringUtils.split(log.toString(),'\n')) {
                RegionsLib.getMain().getLogger().finest(s);
            }
        }
    }
}
