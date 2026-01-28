package com.kntrel.mc.regionLib.region.rule;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class Rules {

    private Rules() {}

    @DeclareRule
    public static final Rule<Boolean> ANIMALS_TAKE_DAMAGE = Rule.on(EntityDamageEvent.class)
            .when(e -> e.getEntity() instanceof Animals)
            .at(e -> e.getEntity().getLocation())
            .ifFalse()
            .thenCancel()
            .build();

    @DeclareRule
    public static final Rule<Boolean> FIRE_SPREADS = Rule.on(BlockSpreadEvent.class)
            .when(e -> e.getSource().getType().equals(Material.FIRE))
            .ifFalse()
            .thenCancel()
            .build();
}
