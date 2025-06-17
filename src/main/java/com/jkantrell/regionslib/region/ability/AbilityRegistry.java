package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.util.AreaGetter;
import com.jkantrell.regionslib.util.PointGetter;
import io.avaje.lang.Nullable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbilityRegistry {

    //ASSETS
    private static final Listener VOID_LISTENER = new Listener(){};


    //FIELDS
    private final RegionContext context_;
    private final Plugin plugin_;
    private final Map<String, Ability> keyMap_;
    private final Map<EventKey, Set<Ability>> eventMap_;


    // CONSTRUCTORS
    public AbilityRegistry(RegionContext context) {
        this.context_ = context;
        this.plugin_ = this.context_.getPlugin();
        this.keyMap_ = new HashMap<>();
        this.eventMap_ = new HashMap<>();
    }


    // UTILITY
    public Map<Class<? extends Event>, List<Ability>> getAllByEvent() {
        return this.keyMap_.values().stream().collect(Collectors.groupingBy(Ability::getEventClass));
    }
    public Map<String, Ability> getAllByKey() {
        return Map.copyOf(this.keyMap_);
    }
    public Optional<Ability> get(String key) {
        return Optional.ofNullable(this.keyMap_.get(key));
    }
    public List<Ability> get(Class<? extends Event> eventClass) {
        return this.eventMap_.entrySet().stream()
                .filter(e -> e.getKey().eventClass().equals(eventClass))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }
    public List<Ability> get(Class<? extends Event> eventClass, EventPriority priority) {
        Set<Ability> l = this.eventMap_.get(new EventKey(eventClass, priority));
        if (l == null) { return Collections.emptyList(); }
        return List.copyOf(l);
    }
    public boolean exists(String key) {
        return this.keyMap_.containsKey(key);
    }
    public void register(Ability ability) {
        if (this.keyMap_.containsKey(ability.getName())) {
            throw new IllegalStateException("Ability '" + ability.getName() + "' is already registered.");
        }
        this.keyMap_.put(ability.getName(), ability);

        Class<? extends Event> eventClass = ability.getEventClass();;
        if (!Cancellable.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("Ability '" + ability.getName() + "' is based on a non-cancellable event: '" + eventClass.getName() + "'");
        }

        EventKey eventKey = new EventKey(eventClass, ability.getBukkitPriority());
        if (!this.eventMap_.containsKey(eventKey)) {
            this.eventMap_.put(eventKey, new TreeSet<>());  // Keeps the abilities sorted by priority

            this.plugin_.getServer().getPluginManager().registerEvent(
                    eventKey.eventClass(),
                    VOID_LISTENER,
                    eventKey.priority(),
                    (l,e) -> { try { this.onEvent(e, eventKey); } catch (ClassCastException ignored) {} },
                    this.plugin_,
                    true
            );
        }

        this.eventMap_.get(eventKey).add(ability);
    }
    public void registerFrom(Class<?> clazz) {
        this.extract(clazz, null).forEach(this::register);
    }
    public void registerFrom(Object obj) {
        this.extract(obj.getClass(), obj).forEach(this::register);
    }
    public <E extends Event> AbilityBuilder<E> registerOn(Class<E> eventClass) {
        return new InnerAbilityBuilder<>(eventClass, this);
    }


    //IMPLEMENTATION
    protected void onEvent(Event event, EventKey eventKey) {
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


    //PRIVATE
    private List<Ability> extract(Class<?> clazz, @Nullable Object obj) {
        var members = Stream.concat(Arrays.stream(clazz.getFields()), Arrays.stream(clazz.getMethods())).toList();

        List<Ability> abilities = new ArrayList<>();

        for (var member : members) {
            if (!member.isAnnotationPresent(DeclareAbility.class)) { continue; }

            Class<?> type = (member instanceof Field f) ? f.getType() : ((Method) member).getReturnType();
            if (!Ability.class.isAssignableFrom(type)) {
                this.plugin_.getServer().getLogger().warning(
                    "Member '" + member.getName() + "' of class '" + clazz.getName() + "' annotated as @DeclaredAbility, but is not of type 'Ability'. Ignored."
                );
                continue;
            }

            boolean isStatic = Modifier.isStatic(member.getModifiers());
            if ((obj == null && !isStatic) || (obj != null && isStatic)) { continue; }

            Ability a;
            try {
                a = (Ability) ((member instanceof Field f) ? f.get(obj) : ((Method) member).invoke(obj));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (a == null) { continue; }
            if (a.getName().equals("<unnamed>")) {
                a = a.isPointBased()
                    ? new Ability(member.getName(), a.getEventClass(), a.getValidator(), a.getPlayerGetter(), (PointGetter) a.getPointGetter().get(), a.getPriority(), a.getBukkitPriority(), a.getSupperAbility().orElse(null))
                    : new Ability(member.getName(), a.getEventClass(), a.getValidator(), a.getPlayerGetter(), (AreaGetter) a.getAreaGetter().get(), a.getPriority(), a.getBukkitPriority(), a.getSupperAbility().orElse(null));

                member.setAccessible(true);
                if (member instanceof Field f) {
                    try {
                        f.set(obj, a);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            abilities.add(a);
        }

        return abilities;
    }


    //CLASSES
    protected record EventKey(Class<? extends Event> eventClass, EventPriority priority) {
        @Override public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o == this) { return true; }
            if (!(o instanceof EventKey other)) { return false; }
            return other.eventClass.equals(this.eventClass) && other.priority.equals(this.priority);
        }
        @Override public int hashCode() {
            return (this.eventClass.hashCode() * 10) + this.priority.hashCode();
        }
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
