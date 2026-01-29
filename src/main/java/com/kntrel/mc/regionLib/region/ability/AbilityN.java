package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.trigger.TriggerListener;
import java.util.Collection;
import java.util.Set;

public class AbilityN implements TriggerListener<AbilityTrigger<?>> {

    //FIELDS
    private final String name_;
    private final Set<AbilityTrigger<?>> triggers_;


    //CONSTRUCTOR
    public AbilityN(String name, Collection<AbilityTrigger<?>> triggers) {
        this.name_ = name;
        this.triggers_ = Set.copyOf(triggers);
    }


    //IMPLEMENTATION
    @Override public String name() {
        return this.name_;
    }
    @Override public Set<AbilityTrigger<?>> triggers() {
        return this.triggers_;
    }
}
