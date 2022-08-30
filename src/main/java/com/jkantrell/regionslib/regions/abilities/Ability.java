package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.events.AbilityTriggeredEvent;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.Regions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class Ability<E extends Event> implements Comparable<Ability<E>> {

    //STATIC METHODS
    public static <E extends Event,T extends Enum<?>> AbilityList<E> enumBuilder(
            Class<E> eventClass,
            Function<E,T> enumGetter,
            Map<String,T> map,
            Function<E,Player> playerGetter,
            Function<E,Location> locationGetter
    ) {
        AbilityList<E> list = new AbilityList<>();
        for (Map.Entry<String,T> entry : map.entrySet()) {
            list.add(new Ability<E>(
                    eventClass,
                    entry.getKey(),
                    e -> enumGetter.apply(e).equals(entry.getValue()),
                    playerGetter,
                    locationGetter
            ));
        }
        return list;
    }

    private static boolean overlappingPermissionCheck_(boolean[] bools) {
        switch (RegionsLib.CONFIG.overlappingPermissionsMode) {
            case newest: return bools[0];
            case oldest: return bools[bools.length - 1];
            case all: {
                for (boolean b : bools) {
                    if (!b) { return false; }
                }
                return true;
            }
            case any: {
                for (boolean b : bools) {
                    if (b) { return true; }
                }
                return false;
            }
        }
        return true;
    }

    //FIELDS
    private int priority_ = 0;
    private EventPriority bukkitPriority_ = EventPriority.NORMAL;
    private Ability<?>[] invalidates_ = {};
    private Ability<E> superAbility_ = null;
    private final AbilityList<E> subAbilities_ = new AbilityList<>();
    private final ArrayList<Player> invalidated_ = new ArrayList<>();
    private String name_;
    boolean registrable = true;
    public final Class<E> eventClass;
    public final Predicate<E> validator;
    public final Function<E,Player> playerGetter;
    public final Function<E,Location> locationGetter;

    //CONSTRUCTORS
    public Ability(@Nonnull Class<E> eventClass, String name, @Nonnull Predicate<E> validation, Function<E,Player> playerGetter, Function<E,Location> locationGetter) {
        this.eventClass = eventClass;
        this.name_ = name;
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
    public Ability(@Nonnull Ability<E> baseAbility, String name, Predicate<E> validator) {
        this(baseAbility.eventClass, name,validator, baseAbility.playerGetter, baseAbility.locationGetter);
    }
    public Ability(@Nonnull Class<E> eventClass, @Nonnull Predicate<E> validation, Function<E,Player> playerGetter, Function<E,Location> locationGetter) {
        this(eventClass,null,validation,playerGetter,locationGetter);
    }
    public Ability(@Nonnull Class<E> eventClass, @Nonnull Predicate<E> validation, Function<E,Location> locationGetter) {
        this(eventClass,null,validation,null,locationGetter);
    }
    public Ability(@Nonnull Class<E> eventClass, @Nonnull Predicate<E> validation) {
        this(eventClass,null, validation,null,null);
    }
    public Ability(@Nonnull Ability<E> baseAbility, Predicate<E> validator) {
        this(baseAbility.eventClass, null, validator, baseAbility.playerGetter, baseAbility.locationGetter);
    }

    //GETTERS
    public String getName() {
        return this.name_;
    }
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
    public AbilityList<E> getSubAbilities() {
        return this.subAbilities_.clone();
    }
    public boolean isAllowedIn(Region[] regions, Player player) {
        boolean[] results = new boolean[regions.length];
        for (int i = 0; i < regions.length; i++) {
            results[i] = regions[i].checkAbility(player,this);
        }
        return Ability.overlappingPermissionCheck_(results);
    }
    public boolean isAllowedIn(Region region, Player player) {
        return this.isAllowedIn(new Region[] {region},player);
    }
    public boolean isAllowedAt(double x, double y, double z, World world, Player player) {
        return this.isAllowedIn(Regions.getAt(x,y,z,world),player);
    }
    public boolean isAllowedAt(Location location, Player player) {
        return this.isAllowedIn(Regions.getAt(location),player);
    }

    //SETTERS
    void setName(String name) {
        this.name_ = name;
    }
    public Ability<E> setPriority(int priority) {
        if (this.superAbility_ != null) {
            if (priority <= this.superAbility_.getPriority()) {
                throw new IllegalArgumentException(
                    this.name_ +"'s priority cannot be lower than "+this.superAbility_.name_ +"'s."
                );
            }
        }

        this.priority_ = priority;

        AbilityList<E> subAbilities = this.getSubAbilities();
        if (subAbilities != null) {
            for (Ability<E> subAbility : subAbilities.toList()) {
                subAbility.extend(this);
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
        if (superAbility == null) {
            if (this.superAbility_ != null) {
                this.superAbility_.subAbilities_.remove(this);
            }
        } else {
            superAbility.subAbilities_.add(this);
        }
        this.superAbility_ = superAbility;

        if (this.superAbility_ != null) {
            int superPriority = this.superAbility_.getPriority();
            if (this.getPriority() <= superPriority) {
                this.setPriority(superPriority + 1);
            }
        }
        return this;
    }
    private void invalidate(Player player) {
        this.invalidated_.add(player);
        new BukkitRunnable(){
            @Override
            public void run() {
                Ability.this.invalidated_.remove(player);
            }
        }.runTaskLater(RegionsLib.getMain(),0);
    }

    //REGISTRATION METHODS
    public boolean isTriggeredBy(Class<? extends Event> event) {
        return eventClass.equals(event);
    }
    public boolean isTriggeredBy(Event event) {
        return this.isTriggeredBy(event.getClass());
    }
    public void register() {
        RegionsLib.getAbilityHandler().register(this);
    }
    public void unregister() {
        RegionsLib.getAbilityHandler().unregister(this);
    }

    //EVENT_HANDLING METHODS
    public void invalidateTargets(E event) {
        for (Ability<?> ability : this.invalidates_) {
            ability.invalidate(this.playerGetter.apply(event));
        }
    }
    public boolean isValid(E event) {
        if (invalidated_.contains(playerGetter.apply(event))) { return false; }
        try {
            Player player = this.playerGetter.apply(event);
            Location location = this.locationGetter.apply(event);
            if (player == null || location == null) { return false; }

            return this.validator.test(event);
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                RegionsLib.getMain().getLogger().warning(
                "Ability " + name_ + " failed validation due to " + e.getClass().getName()
                );
            }
            return false;
        }
    }
    public boolean fire(E trigger) {
        if (!(trigger instanceof Cancellable cancellable)) { throw new ClassCastException("The event must be cancellable"); }

        Player player = this.playerGetter.apply(trigger);
        Location location = this.locationGetter.apply(trigger);
        if (player == null || location == null) { return true; }

        Region[] regions = Regions.getAt(location);
        if (regions.length < 1) { return true; }

        boolean[] results = new boolean[regions.length];

        int i = 0;
        cancellable.setCancelled(false);
        for (Region region : regions) {
            AbilityTriggeredEvent event = new AbilityTriggeredEvent(
                    this,
                    player,
                    region.checkAbility(player,this),
                    region,
                    location,
                    trigger
            );
            RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
            results[i] = event.isAllowed();
            i++;
        }

        boolean r = Ability.overlappingPermissionCheck_(results);
        if (!cancellable.isCancelled()) { cancellable.setCancelled(!r); }
        return r;
    }

    //INFERRING METHODS
    private Function<E,Player> getPlayerGetter_() {
        Function<E,Player> getter;
        try {
            Method getPlayer = eventClass.getMethod("getPlayer");
            getter = new GenericPlayerGetter(getPlayer);
        } catch (NoSuchMethodException e) {
            RegionsLib.getMain().getLogger().warning(this.name_ + " ability is unregistrable!");
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
        } else if (EntityEvent.class.isAssignableFrom(eventClass)) {
            locGetter = e -> ((EntityEvent) e).getEntity().getLocation();
        } else {
            RegionsLib.getMain().getLogger().warning(this.name_ + " ability is unregistrable!");
            RegionsLib.getMain().getLogger().warning(eventClass.getName() + " is not a BlockEvent or an EntityEvent. Unable to infer location. Please define a locationGetter lambda directly.");
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
                "Unable to execute getPlayer method in " + getter_.getDeclaringClass().getName() + " in " + name_ + " ability!"
                );
                ex.printStackTrace();
                RegionsLib.getMain().getLogger().warning("Unregistering ability. Please declare the playerGetter directly.");
                Ability.this.unregister();
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
    }
    @Override
    public String toString(){
        return this.getName();
    }
}