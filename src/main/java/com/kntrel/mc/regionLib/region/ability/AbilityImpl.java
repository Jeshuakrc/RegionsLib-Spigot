package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class AbilityImpl implements Ability {

    //FIELDS
    private final String name_;
    private final Set<AbilityTrigger<?>> triggers_;
    private final Ability super_;
    private final BiConsumer<Event, List<Region>> onAllowed_;
    private final BiConsumer<Event, List<Region>> onDenied_;


    //CONSTRUCTOR
    public AbilityImpl(String name, Collection<AbilityTrigger<?>> triggers, @Nullable Ability superAbility, BiConsumer<Event, @Nullable List<Region>> onAllowed, @Nullable BiConsumer<Event, List<Region>> onDenied) {
        this.name_ = name;
        this.triggers_ = Set.copyOf(triggers);
        this.super_ = superAbility;
    }
    public AbilityImpl(String name, Collection<AbilityTrigger<?>> triggers) {
        this(name, triggers, null, null, null);
    }


    //GETTERS
    @Override public String name() {
        return this.name_;
    }
    @Override public Set<AbilityTrigger<?>> triggers() {
        return this.triggers_;
    }
    @Override public Optional<Ability> superAbility() {
        return Optional.ofNullable(this.super_);
    }
    @Override public void onAllowed(Event event, List<Region> regions) {
        if (this.onAllowed_ != null) {
            this.onAllowed_.accept(event, regions);
        } else {
            Ability.super.onAllowed(event, regions);
        }
    }
    @Override public void onDenied(Event event, List<Region> regions) {
        if (this.onDenied_ != null) {
            this.onDenied_.accept(event, regions);
        } else {
            Ability.super.onDenied(event, regions);
        }
    }
}
