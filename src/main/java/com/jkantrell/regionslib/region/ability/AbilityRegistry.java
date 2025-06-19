package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.region.react.ReflectiveEventReactorRegistry;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.*;
import java.util.function.Predicate;

public class AbilityRegistry extends ReflectiveEventReactorRegistry<Ability> {



    // CONSTRUCTORS
    public AbilityRegistry(RegionContext context) {
        super(context, Ability.class, DeclareAbility.class);
    }


    @Override public void register(Ability ability) {
        Class<? extends Event> eventClass = ability.getEventClass();
        if (!Cancellable.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("Ability '" + ability.getName() + "' is based on a non-cancellable event: '" + eventClass.getName() + "'");
        }
        super.register(ability);
    }
    @Override public <E extends Event> AbilityBuilder<E> registerOn(Class<E> eventClass) {
        return new InnerAbilityBuilder<>(eventClass, this);
    }


    //IMPLEMENTATION
    @Override protected void onEvent(Event event, EventKey eventKey) {
        Set<Ability> abilities = this.eventMap_.get(eventKey);
        Set<Ability> discard = new HashSet<>();

        //Keeping the highest priority Ability
        Ability definitive = null;
        for (Ability a : abilities) {
            if (
                    a.getSupperAbility().map(discard::contains).orElse(false)
                            || !a.appliesTo(event)
            ) {
                discard.add(a);
            } else {
                definitive = a;
            }
        }
        if (definitive == null) { return; }

        //Cancelling the event if the ability is not allowed.
        boolean allow = definitive.test(event, this.context_);


        //TODO: Logging
    }


    private static class InnerAbilityBuilder<E extends Event> extends AbilityBuilder<E> {

        //FIELDS
        private final AbilityRegistry registry_;


        //CONSTRUCTORS
        protected InnerAbilityBuilder(Class<E> eventClass, AbilityRegistry registry) {
            super(eventClass);
            this.registry_ = registry;
        }


        //OVERWRITES
        @Override protected Ability build(Predicate<E> additionalCheck) {
            Ability ability = super.build(additionalCheck);
            this.registry_.register(ability);
            return ability;
        }
    }
}