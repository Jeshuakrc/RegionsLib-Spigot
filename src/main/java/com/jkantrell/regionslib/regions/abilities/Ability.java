package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Ability<E extends Event> implements Comparable<Ability<E>> {

    //STATIC FIELDS
    private static final HashMap<Ability<? extends Event>, AbilityList<? extends Event>> superMap_ = new HashMap<>();

    //FIELDS
    private int priority_ = 0;
    private EventPriority bukkitPriority_ = EventPriority.NORMAL;
    private Ability<?>[] invalidates_ = {};
    private Ability<E> superAbility_ = null;
    private final ArrayList<Player> invalidated_ = new ArrayList<>();
    boolean registrable = true;
    public final Class<E> eventClass;
    public final String name;
    public final Predicate<E> validator;
    public final Function<E,Player> playerGetter;
    public final Function<E,Location> locationGetter;

    //CONSTRUCTORS
    public Ability(@Nonnull Class<E> eventClass, String name, @Nonnull Predicate<E> validation, Function<E,Player> playerGetter, Function<E,Location> locationGetter) {
        this.eventClass = eventClass;
        this.name = name;
        this.validator = validation;
        this.playerGetter = (playerGetter == null) ? this.getPlayerGetter_() : playerGetter;
        this.locationGetter = (locationGetter == null) ? this.getLocationGetter_() : locationGetter;
    }
    public Ability(@Nonnull Class<E> eventClass, String name,@Nonnull Predicate<E> validation, Function<E,Location> locationGetter) {
        this(eventClass,name,validation,null,locationGetter);
    }
    public Ability(@Nonnull Class<E> eventClass, String name,@Nonnull Predicate<E> validation) {
        this(eventClass,name,validation,null,null);
    }

    //STATIC METHODS
    public static <E extends Event> AbilityList<E> getSubAbilities(Ability<E> ability) {
        return (AbilityList<E>) Ability.superMap_.get(ability);
    }
    private static <E extends Event> void addToSupperMap_(Ability<E> ability) {
        Ability<E> superAbility = ability.getSuperAbility();
        if (superAbility != null) {
            AbilityList<E> subAbilities = Ability.getSubAbilities(superAbility);
            if (subAbilities == null) {
                subAbilities = new AbilityList<>();
                Ability.superMap_.put(superAbility,subAbilities);
            }
            subAbilities.add(ability);
        }
    }
    private static <E extends Event> void removeFromSupperMap_(Ability<E> ability) {
        Ability<E> superAbility = ability.getSuperAbility();
        if (superAbility != null) {
            AbilityList<E> subAbilities = Ability.getSubAbilities(superAbility);
            if (subAbilities != null) {
                subAbilities.remove(ability);
                if (subAbilities.isEmpty()) {
                    Ability.superMap_.remove(superAbility);
                }
            }
        }
    }

    //GETTERS
    public int getPriority() {
        return this.priority_;
    }
    public EventPriority getBukkitPriority() {
        return this.bukkitPriority_;
    }
    public Ability<E> getSuperAbility() {
        return this.superAbility_;
    }
    public boolean isRegistered() {
        return RegionsLib.getAbilityHandler().isRegistered(this);
    }

    //SETTERS
    public Ability<E> setPriority(int priority) {
        if (this.superAbility_ != null) {
            if (priority <= this.superAbility_.getPriority()) {
                throw new IllegalArgumentException(
                    this.name+"'s priority cannot be lower than "+this.superAbility_.name+"'s."
                );
            }
        }

        this.priority_ = priority;

        AbilityList<E> subAbilities = Ability.getSubAbilities(this);
        if (subAbilities != null) {
            for (Ability<E> subAbility : subAbilities.toList()) {
                subAbility.recalculatePriority_();
            }
        }
        return this;
    }
    public Ability<E> setBukkitPriority(EventPriority priority) {
        this.bukkitPriority_ = priority;
        this.refreshRegistration_();
        return this;
    }
    public Ability<E> invalidates(Ability<?>... toInvalidate) {
        this.invalidates_ = toInvalidate;
        this.refreshRegistration_();
        return this;
    }
    public Ability<E> extend(@Nullable Ability<E> superAbility){
        Ability.removeFromSupperMap_(this);
        this.superAbility_ = superAbility;
        Ability.addToSupperMap_(this);
        this.recalculatePriority_();
        return this;
    }
    private void invalidate(Player player) {
        this.invalidated_.add(player);
        new BukkitRunnable(){
            @Override
            public void run() {
                Ability.this.invalidated_.remove(player);
            }
        }.runTaskLater(RegionsLib.getMain(),1);
    }

    //REGISTRATION METHODS
    public void register() {
        RegionsLib.getAbilityHandler().register(this);
    }
    public void unregister() {
        RegionsLib.getAbilityHandler().unregister(this);
    }

    //EVENT_HANDLING METHODS
    public void invalidateTargets(E event) {
        for (Ability<?> ability : invalidates_) {
            ability.invalidate(playerGetter.apply(event));
        }
    }
    public boolean isValid(E event) {
        if (invalidated_.contains(playerGetter.apply(event))) { return false; }
        try {
            return this.validator.test(event);
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                RegionsLib.getMain().getLogger().warning(
                "Ability " + name + " failed validation due to " + e.getClass().getName()
                );
            }
            return false;
        }
    }
    public boolean isAllowed(E event) {
        return Region.checkAbilityAt(
            this.playerGetter.apply(event),
            this,
            this.locationGetter.apply(event)
        );
    }

    //INFERRING METHODS
    private Function<E,Player> getPlayerGetter_() {
        Function<E,Player> getter;
        try {
            Method getPlayer = eventClass.getMethod("getPlayer");
            getter = new GenericPlayerGetter(getPlayer);
        } catch (NoSuchMethodException e) {
            RegionsLib.getMain().getLogger().warning(this.name + " ability is unregistrable!");
            RegionsLib.getMain().getLogger().warning(eventClass.getName() + " doesn't have a 'getPlayer' method, please define a playerGetter lambda directly.");
            getter = null;
            registrable = false;
        }
        return getter;
    }
    public Function<E,Location> getLocationGetter_() {
        Function<E, Location> locGetter;
        if (BlockEvent.class.isAssignableFrom(eventClass)) {
            locGetter = event -> ((BlockEvent) event).getBlock().getLocation().add(.5, .5, .5);
        } else if (PlayerInteractEvent.class.isAssignableFrom(eventClass)) {
            locGetter = e -> {
                try{
                    return ((PlayerInteractEvent) e).getClickedBlock().getLocation().add(.5,.5,.5);
                } catch (NullPointerException ex) {
                    return null;
                }
            };
        } else {
            RegionsLib.getMain().getLogger().warning(this.name + " ability is unregistrable!");
            RegionsLib.getMain().getLogger().warning(eventClass.getName() + " is not a BlockEvent. Unable to infer location. Please define a locationGetter lambda directly.");
            locGetter = null;
            registrable = false;
        }
        return locGetter;
    }

    //PRIVATE METHODS
    private void refreshRegistration_() {
        if (this.isRegistered()) {
            this.unregister();
            this.register();
        }
    }
    private void recalculatePriority_() {
        if (this.superAbility_ != null) {
            int superPriority = this.superAbility_.getPriority();
            if (this.getPriority() <= superPriority) {
                this.setPriority(superPriority + 1);
            }
        }
    }

    //COMPARING
    @Override
    public int compareTo(Ability<E> ability) {
        return Integer.compare(this.priority_,ability.getPriority());
    }

    //IMPLEMENTATIONS
    private class GenericPlayerGetter implements Function<E,Player> {

        private final Method getter_;

        private GenericPlayerGetter(Method getter) {
            this.getter_ = getter;
        }

        @Override
        public Player apply(E e) {
            try {
                return (Player) getter_.invoke(e);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                RegionsLib.getMain().getLogger().warning(
                "Unable to execute getPlayer method in " + getter_.getDeclaringClass().getName() + " in " + name + " ability!"
                );
                ex.printStackTrace();
                RegionsLib.getMain().getLogger().warning("Unregistering ability. Please declare the playerGetter directly.");
                Ability.this.unregister();
                return null;
            }
        }
    }
}
