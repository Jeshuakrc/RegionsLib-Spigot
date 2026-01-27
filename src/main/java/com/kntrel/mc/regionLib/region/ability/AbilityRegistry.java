package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.react.ReflectiveEventReactorRegistry;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;

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
            boolean applies = true;
            Ability superAbility = a.getSupperAbility().orElse(null);
            if (superAbility != null) {
                applies = !discard.contains(superAbility);
            }
            if (applies) {
                try {
                    applies = a.appliesTo(event);
                } catch (Throwable e) {
                    this.log(Level.SEVERE, "Ability '%1$s' failed to validate. Event: %2$s. Falling back as valid.\nCaused by: %3$s", definitive.getName(), event.getClass().getSimpleName(), e);
                }
            }
            if (applies) {
                definitive = a;
            } else {
                discard.add(a);
            }
        }
        if (definitive == null) { return; }

        //Cancelling the event if the ability is not allowed.
        boolean allowed;
        try {
            allowed = definitive.test(event, this.context_);
        } catch (Throwable e) {
            this.log(Level.SEVERE ,"Ability '%1$s' failed to test. Event: %2$s. Cáncelling event.\nCaused by: %3$s", definitive.getName(), event.getClass().getSimpleName(), e);
            if (event instanceof Cancellable c) { c.setCancelled(true); }
            allowed = false;
        }

        this.log(Level.FINE,
                "Ability '%1$s' %2$s to %3$s",
                definitive.getName(),
                allowed ? "allowed" : "not allowed",
                definitive.getPlayerGetter().apply(event).getName()
        );
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

    private void log(Level level, String formattedLog, Object... args) {
        this.plugin_.getLogger().log(level ,String.format(formattedLog, args));
    }
}