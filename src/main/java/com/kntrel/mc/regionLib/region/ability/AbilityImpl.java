package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class AbilityImpl implements Ability {

    //FIELDS
    private final String name_;
    private final Set<AbilityTrigger<?>> triggers_;
    private final Ability super_;


    //CONSTRUCTOR
    public AbilityImpl(String name, Collection<AbilityTrigger<?>> triggers, @Nullable Ability superAbility) {
        this.name_ = name;
        this.triggers_ = Set.copyOf(triggers);
        this.super_ = superAbility;
    }
    public AbilityImpl(String name, Collection<AbilityTrigger<?>> triggers) {
        this(name, triggers, null);
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
    @Override public void onAllowed(Event event, Region region) {

    }
    @Override public void onDenied(Event event, Region region) {

    }
}
