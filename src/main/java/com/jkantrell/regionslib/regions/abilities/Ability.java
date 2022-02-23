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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

public class Ability<E extends Event> implements Comparable<Ability<E>> {

    private int priority_ = 0;
    private EventPriority bukkitPriority_ = EventPriority.NORMAL;
    private Ability<?>[] invalidates_ = {};
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

    //GETTERS
    public int getPriority() {
        return this.priority_;
    }
    public EventPriority getBukkitPriority() {
        return this.bukkitPriority_;
    }

    //SETTERS
    public Ability<E> setPriority(int priority) {
        this.priority_ = priority;
        return this;
    }
    public Ability<E> setBukkitPriority(EventPriority priority) {
        this.bukkitPriority_ = priority;
        if (RegionsLib.getAbilityHandler().getRegisteredAbilities().contains(this.name)) {
            this.unregister();
            this.register();
        }
        return this;
    }
    public Ability<E> invalidates(Ability<?>... toInvalidate) {
        this.invalidates_ = toInvalidate;
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
