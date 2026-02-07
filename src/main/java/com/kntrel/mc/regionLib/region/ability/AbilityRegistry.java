package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.trigger.Bounds;
import com.kntrel.mc.regionLib.trigger.ReflectiveListernerRegistry;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import com.kntrel.util.Priority;
import com.kntrel.util.SetMap;
import com.kntrel.util.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class AbilityRegistry extends ReflectiveListernerRegistry<AbilityTrigger<?>, Ability> {

    //ASSETS
    private static final Comparator<Entry> ENTRY_COMPARATOR = Comparator.<Entry>naturalOrder().reversed();


    //FIELDS
    private final Permission.OverlapMode permissionOverlapMode_;
    private final SetMap<Class<? extends Event>, Entry> planMap_;


    // CONSTRUCTORS
    public AbilityRegistry(RegionContext context, Permission.OverlapMode permissionOverlapMode) {
        super(context, Ability.class, DeclareAbility.class);
        this.permissionOverlapMode_ = permissionOverlapMode;
        this.planMap_ = new SetMap<>(() -> new TreeSet<>(ENTRY_COMPARATOR));
    }


    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void handle(SequencedCollection<? extends Pair<Ability, AbilityTrigger<?>>> triggerEntries, Event event, EventPriority priority) {
        Pair<Ability, AbilityTrigger<?>> definitive = null;
        IdentityHashMap<AbilityTrigger<?>, Boolean> cache = null;
        final Class<? extends Event> eventClass = event.getClass();

        outer: for (Pair<Ability, AbilityTrigger<?>> current : triggerEntries) {
            final Ability ability = current.first();
            final AbilityTrigger<?> trigger = current.second();

            // Walk the superAbility chain
            Ability superAbility = ability.superAbility().orElse(null);
            if (superAbility != null) {
                ArrayList<Ability> chain = new ArrayList<>(2);
                for (Ability a = superAbility; a != null; a = a.superAbility().orElse(null)) {
                    chain.add(a);
                }

                for (int i = chain.size() - 1; i >= 0; i--) {
                    Ability a = chain.get(i);
                    boolean pass = false;
                    for (AbilityTrigger<?> st : a.triggers()) {
                        if (!st.eventClass().equals(eventClass)) { continue; }

                        if (cache == null) { cache = new IdentityHashMap<>(); }
                        Boolean p = cache.get(st);
                        if (p == null) {
                            try {
                                p = ((AbilityTrigger) st).appliesTo(event);
                            } catch (Throwable e) {
                                this.context_.getServer().getLogger().severe(
                                        "Trigger in listener '" + a.name() + "' failed to validate. Event: " + eventClass.getSimpleName() + ". Falling back as non-applicable."
                                );
                                this.context_.getServer().getLogger().severe("Caused by: " + e);
                                p = false;
                            }
                            cache.put(st, p);
                        }

                        if (p) { pass = true; break; } // ANY super-trigger passing is enough
                    }

                    if (!pass) { continue outer; } // super chain failed -> this ability cannot trigger
                }
            }

            // Evaluate current trigger

            Boolean prev = (cache != null) ? cache.get(trigger) : null;
            boolean pass;
            if (prev != null) {
                pass = prev;
            } else try {
                pass = ((AbilityTrigger) trigger).appliesTo(event);
            } catch (Throwable e) {
                this.context_.getServer().getLogger().severe(
                        "Trigger in listener '" + ability.name() + "' failed to validate. Event: " + eventClass.getSimpleName() + ". Falling back as non-applicable."
                );
                this.context_.getServer().getLogger().severe("Caused by: " + e);
                pass = false;
            }

            if (cache != null) { cache.put(trigger, pass); }

            if (!pass) continue;
            definitive = current;
            break;
        }

        if (definitive == null) return;

        // Localize
        Bounds loc;
        try {
            loc = ((AbilityTrigger) definitive.second()).localize(event);
        } catch (Throwable e) {
            this.log(Level.SEVERE,
                    "Trigger in listener '%1$s' failed to localize. Event: %2$s. Aborting handling.",
                    definitive.first().name(),
                    eventClass.getSimpleName()
            );
            this.log(Level.SEVERE, "Caused by: %1$s", e);
            return;
        }
        if (loc == null) { return; }

        RegionReadRepository repo = this.context_.getHotRegionRepository();
        List<Region> regions = (loc.isArea())
                ? repo.getIn(loc.getArea())
                : repo.getAt(loc.getPoint());

        if (regions.isEmpty()) return;

        this.handle(regions, event, definitive.first(), definitive.second());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void handle(List<Region> regions, Event event, Ability ability, AbilityTrigger<?> trigger) {
        if (regions.isEmpty()) {
            return;
        }

        Player causer;
        try {
            causer = ((AbilityTrigger) trigger).attribute(event);
        } catch (Throwable e) {
            this.log(Level.SEVERE, "Ability '%1$s' failed to attribute player from event: %2$s. Falling back as not allowed.\nCaused by: %3$s", ability.name(), event.getClass().getSimpleName(), e);
            return;
        }

        boolean[] perms = new boolean[regions.size()];
        List<Region> allowedRegions = new ArrayList<>();
        List<Region> deniedRegions = new ArrayList<>();

        int i = 0;
        for (Region r : regions) {
            boolean perm = r.checkAbility(causer, ability);
            (perm ? allowedRegions : deniedRegions).add(r);
            perms[i] = perm;
            i++;
        }

        boolean allowed = resolveOverlappingPermissions(perms, this.permissionOverlapMode_);

        try {
            if (allowed) {
                ability.onAllowed(event, allowedRegions);
            } else {
                ability.onDenied(event, deniedRegions);
            }
        } catch (Throwable e) {
            this.log(Level.SEVERE, "Ability '%1$s' failed to execute on%2$s callback. Event: %3$s.\nCaused by: %4$s", ability.name(), allowed ? "Allowed" : "Denied", event.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void register(Ability ability) {
        ensureDependenciesCorrectness(ability);
        super.register(ability);
    }


    //HELPERS

    private static Priority actualPriority(Ability ability, AbilityTrigger<?> trigger) {
        Priority priority = trigger.priority();
        Ability sa = ability.superAbility().orElse(null);
        if (sa == null) {
            return priority;
        }
        Priority superPriority = sa.triggers().stream()
                .filter(t -> t.eventClass().equals(trigger.eventClass()))
                .map(RegionTrigger::priority)
                .max(Priority::compareTo)
                .orElse(Priority.LOWEST);
        return priority.compareTo(superPriority) > 0 ? priority : superPriority;
    }

    private static SetMap<Ability, AbilityTrigger<?>> resolveDependencyTriggers(Ability ability, Class<? extends Event> eventClass) {
        SetMap<Ability, AbilityTrigger<?>> map = new SetMap<>();

        Ability current = ability;
        while (current != null) {
            for (AbilityTrigger<?> trigger : current.triggers()) {
                if (trigger.eventClass().equals(eventClass)) {
                    map.putInto(current, trigger);
                }
            }
            current = current.superAbility().orElse(null);
        }

        return map;
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

            for (AbilityTrigger<?> t : current.triggers()) if (!triggerEvents.contains(t.eventClass())) {
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

    //SUBTYPES
    private record Entry(
            Ability ability,
            AbilityTrigger<?> trigger,
            Predicate<Event> predicate,
            Priority priority,
            int[] sharedChecks
    ) implements Comparable<Entry> {

        @Override public int compareTo(@NotNull AbilityRegistry.Entry o) {
            return this.priority().compareTo(o.priority());
        }
    }
}