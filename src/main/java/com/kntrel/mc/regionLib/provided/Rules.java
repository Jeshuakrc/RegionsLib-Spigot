package com.kntrel.mc.regionLib.provided;

import com.kntrel.mc.regionLib.region.rule.DeclareRule;
import com.kntrel.mc.regionLib.region.rule.Rule;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import static com.kntrel.mc.regionLib.region.rule.Rule.*;

public final class Rules {

    private Rules() {}

    @DeclareRule
    public static final Rule<Boolean> ANIMALS_TAKE_DAMAGE = on(EntityDamageEvent.class)
            .when(e -> e.getEntity() instanceof Animals)
            .at(e -> e.getEntity().getLocation())
            .ifFalse()
            .thenCancel()
            .done();

    @DeclareRule
    public static final Rule<Boolean> FIRE_SPREADS = on(BlockSpreadEvent.class)
                .when(e -> e.getSource().getType().equals(Material.FIRE))
                .ifFalse()
                .thenCancel()
            .alsoOn(BlockBurnEvent.class)
                .ifFalse()
                .then(e -> {
                    if (Math.random() < 0.5) { e.setCancelled(true); }
                })
            .done();
}
