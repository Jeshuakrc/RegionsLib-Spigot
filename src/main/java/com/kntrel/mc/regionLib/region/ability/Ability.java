package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.RegionListener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.List;
import java.util.Optional;

public interface Ability extends RegionListener<AbilityTrigger<?>> {

    //FACTORY
    static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return AbilityBuilder.on(eventClass);
    }


    //CONTRACT
    default Optional<Ability> superAbility() { return Optional.empty(); }
    default void onAllowed(Event event, List<Region> regions) { /* nothing */ }
    default void onDenied(Event event, List<Region> regions) {
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
    }
}
