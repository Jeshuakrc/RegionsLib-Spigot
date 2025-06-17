package com.jkantrell.regionslib.region.rule;

import com.jkantrell.regionslib.util.valueType.ValueType;
import org.bukkit.entity.Animals;
import org.bukkit.event.entity.EntityDamageEvent;

public final class Rules {

    private Rules() {}

    public static final Rule<Boolean> ANIMALS_TAKE_DAMAGE = Rule.on(EntityDamageEvent.class).having(ValueType.BOOL)
            .when(e -> e.getEntity() instanceof Animals)
            .at(e -> e.getEntity().getLocation())
            .iff(b -> b)
            .thenCancel().build();

}
