package com.kntrel.mc.regionLib.region.rule;

import org.bukkit.entity.Animals;
import org.bukkit.event.entity.EntityDamageEvent;

public final class Rules {

    private Rules() {}

    public static final Rule<Boolean> ANIMALS_TAKE_DAMAGE = Rule.on(EntityDamageEvent.class)
            .when(e -> e.getEntity() instanceof Animals)
            .at(e -> e.getEntity().getLocation())
            .ifFalse()
            .thenCancel().build();

}
