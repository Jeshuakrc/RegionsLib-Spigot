package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.listen.Bounds;
import com.kntrel.mc.regionLib.region.listen.ReflectiveListernerRegistry;
import com.kntrel.util.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class AbilityRegistry extends ReflectiveListernerRegistry<AbilityTrigger<?>, Ability> {

    //FIELDS
    private final Permission.OverlapMode permissionOverlapMode_;
    private boolean handling_;


    //SCRATCH FIELDS (to prevent per-event allocations)
    private final IdentityHashMap<AbilityTrigger<?>, Boolean> cache_;
    private final ArrayList<Ability> superAbilityChain_;


    // CONSTRUCTORS
    public AbilityRegistry(RegionContext context, Permission.OverlapMode permissionOverlapMode) {
        super(context, Ability.class, DeclareAbility.class);
        this.permissionOverlapMode_ = permissionOverlapMode;
        this.cache_ = new IdentityHashMap<>();
        this.superAbilityChain_ = new ArrayList<>();
        this.handling_ = false;
    }


    //IMPLEMENTATION
    @Override
    protected void handle(SequencedCollection<? extends Pair<Ability, AbilityTrigger<?>>> triggerEntries, Event event, EventPriority priority) {
        // Re-routing async event to run on the main thread -- Not expected
        if (event.isAsynchronous()) {
            this.plugin_.getServer().getScheduler().runTask(this.plugin_, () -> this.handle(triggerEntries, event, priority));
            return;
        }

        //Re-entrance guard
        if (this.handling_) {
            IdentityHashMap<AbilityTrigger<?>, Boolean> cache = new IdentityHashMap<>();
            ArrayList<Ability> chain = new ArrayList<>();
            handleInner(triggerEntries, event, cache, chain);
            return;
        }

        //Normal path
        this.handling_ = true;
        try {
            handleInner(triggerEntries, event, this.cache_, this.superAbilityChain_);
        } finally {
            this.handling_ = false;
        }
    }

    @Override
    protected void handle(List<Region> regions, Event event, Ability ability, AbilityTrigger<?> trigger) {
        if (regions.isEmpty()) { return; }

        Player causer = tryAttribute(ability.name(), trigger, event);
        if (causer == null) { return; }

        boolean allowed = switch (this.permissionOverlapMode_) {
            case OLDEST -> regions.getFirst().checkAbility(causer, ability);
            case NEWEST -> regions.getLast().checkAbility(causer, ability);
            case ALL -> {
                for (Region r : regions) {
                    if (!r.checkAbility(causer, ability)) {
                        yield false;
                    }
                }
                yield true;
            }
            case ANY -> {
                for (Region r : regions) {
                    if (r.checkAbility(causer, ability)) {
                        yield true;
                    }
                }
                yield false;
            }
        };

        try {
            if (allowed) { ability.onAllowed(event, regions); }
            else { ability.onDenied(event, regions); }
        } catch (Throwable e) {
            this.log(Level.SEVERE,
                    "Ability '%1$s' failed to execute on%2$s callback. Event: %3$s.\nCaused by: %4$s",
                    ability.name(),
                    allowed ? "onAllowed" : "onDenied",
                    event.getClass().getSimpleName(),
                    e
            );
        }
    }

    @Override
    public void register(Ability ability) {
        ensureDependenciesCorrectness(ability);
        super.register(ability);
    }


    //HELPERS
    private void handleInner(
            SequencedCollection<? extends Pair<Ability, AbilityTrigger<?>>> triggerEntries,
            Event event,
            IdentityHashMap<AbilityTrigger<?>, Boolean> cache,
            ArrayList<Ability> superChain
    ) {
        cache.clear();
        final Class<? extends Event> eventClass = event.getClass();
        Pair<Ability, AbilityTrigger<?>> definitive = findEffectiveTrigger(triggerEntries, event, eventClass, cache, superChain);
        if (definitive == null) { return; }

        Bounds loc = tryLocalize(definitive.second(), event, definitive.first().name(), eventClass);
        if (loc == null) { return; }

        Condition cond = (loc.isArea())
                ? Condition.in(loc.getArea())
                : Condition.at(loc.getPoint());
        List<Region> regions = this.context_.getHotRegionRepository().where(cond).orderBy(RegionField.ID).get();

        if (regions.isEmpty()) { return; }

        this.handle(regions, event, definitive.first(), definitive.second());
    }

    private Pair<Ability, AbilityTrigger<?>> findEffectiveTrigger(
            SequencedCollection<? extends Pair<Ability, AbilityTrigger<?>>> triggerEntries,
            Event event,
            Class<? extends Event> eventClass,
            IdentityHashMap<AbilityTrigger<?>, Boolean> cache,
            ArrayList<Ability> superChain
        ) {

        for (Pair<Ability, AbilityTrigger<?>> current : triggerEntries) {
            final Ability ability = current.first();
            final AbilityTrigger<?> trigger = current.second();

            if (!validateSuperAbility(ability, event, eventClass, cache, superChain)) {
                continue;
            }

            Boolean applies = cache.get(trigger);
            if (applies == null) {
                applies = tryAppliesOn(trigger, event, ability.name(), eventClass);
                cache.put(trigger, applies);
            }

            if (!applies) { continue; }
            return current;
        }

        return null;
    }

    private boolean validateSuperAbility(
            Ability ability,
            Event event,
            Class<? extends Event> eventClass,
            IdentityHashMap<AbilityTrigger<?>, Boolean> cache,
            ArrayList<Ability> superChain
    ) {
        Ability superAbility = ability.superAbility().orElse(null);
        if (superAbility == null) { return true; }

        superChain.clear();
        for (Ability a = superAbility; a != null; a = a.superAbility().orElse(null)) {
            superChain.add(a);
        }

        for (int i = superChain.size() - 1; i >= 0; i--) {
            Ability sup = superChain.get(i);

            boolean pass = false;
            for (AbilityTrigger<?> st : sup.triggers()) {
                if (!st.eventClass().equals(eventClass)) { continue; }

                Boolean applies = cache.get(st);
                if (applies == null) {
                    applies = tryAppliesOn(st, event, sup.name(), eventClass);
                    cache.put(st, applies);
                }

                if (applies) { pass = true; break; }  // ANY trigger passing is enough
            }

            if (!pass) { return false; }
        }

        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean tryAppliesOn(AbilityTrigger trigger, Event event, String abilityName, Class<? extends Event> eventClass) {
        try {
            return trigger.appliesTo(event);
        } catch (Throwable e) {
            this.context_.getServer().getLogger().severe(
                "Trigger in listener '" + abilityName + "' failed to validate. Event: " + eventClass.getSimpleName() + ". Falling back as non-applicable."
            );
            this.context_.getServer().getLogger().severe("Caused by: " + e);
            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Bounds tryLocalize(AbilityTrigger trigger, Event event, String abilityName, Class<? extends Event> eventClass) {
        try {
            return trigger.localize(event);
        } catch (Throwable e) {
            this.log(Level.SEVERE,
                    "Trigger in listener '%1$s' failed to localize. Event: %2$s. Aborting handling.",
                    abilityName,
                    eventClass.getSimpleName()
            );
            this.log(Level.SEVERE, "Caused by: %1$s", e);
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Player tryAttribute(String abilityName, AbilityTrigger trigger, Event event) {
        try {
            return trigger.attribute(event);
        } catch (Throwable e) {
            this.log(Level.SEVERE, "Ability '%1$s' failed to attribute player from event: %2$s. Falling back as not allowed.\nCaused by: %3$s", abilityName, event.getClass().getSimpleName(), e);
            return null;
        }
    }

    private static void ensureDependenciesCorrectness(Ability ability) {
        Ability superAbility = ability.superAbility().orElse(null);
        if (superAbility == null) { return; }

        Set<Ability> visited = new HashSet<>();
        visited.add(ability);
        Set<Class<? extends Event>> triggerEvents = ability.triggers().stream().map(AbilityTrigger::eventClass).collect(Collectors.toSet());
        Ability current = superAbility;

        while (current != null) {
            if (visited.contains(current)) {
                throw new IllegalArgumentException("Circular dependency in ability '" + ability.name() + "', at '" + current.name() + "'.");
            }

            boolean sharesAny = current.triggers().stream()
                    .map(AbilityTrigger::eventClass)
                    .anyMatch(triggerEvents::contains);

            if (!sharesAny) {
                throw new IllegalStateException(
                    "Ability '" + ability.name() + "' depends on '" + current.name() + "', yet they share no triggers on the same event. "
                    + "'" + ability.name() + "' will never trigger."
                );
            }

            visited.add(current);
            current = current.superAbility().orElse(null);
        }
    }

    private static boolean resolveOverlappingPermissions(boolean[] bools, Permission.OverlapMode mode) {
        return switch (mode) {
            case NEWEST -> bools[0];
            case OLDEST -> bools[bools.length - 1];
            case ALL -> {
                for (boolean b : bools) {
                    if (!b) {
                        yield false;
                    }
                }
                yield true;
            }
            case ANY -> {
                for (boolean b : bools) {
                    if (b) {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }

    private void log(Level level, String formattedLog, Object... args) {
        this.plugin_.getLogger().log(level, String.format(formattedLog, args));
    }
}