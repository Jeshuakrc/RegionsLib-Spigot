package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.util.Area;
import io.avaje.lang.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityBuilder<E extends Event> {

    // Fields
    private final Class<E> eventClass_;
    private String name_;
    private Predicate<E> validator_;
    private Function<E, Player> playerGetter_;
    private Function<E, Location> pointGetter_;
    private Function<E, Area> areaGetter_;
    private Consumer<Ability> consumer_;
    private EventPriority bukkitPriority_;
    private Integer priority_;
    private Ability extends_;


    // CONSTRUCTORS
    // Fabric static method for readability
    public static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return new AbilityBuilder<>(eventClass);
    }
    protected AbilityBuilder(Class<E> eventClass) {
        this.eventClass_ = eventClass;
    }


    // CHAINED CONFIG
    public AbilityBuilder<E> copyOf(AbilityBuilder<E> other) {
        this.name_ = other.name_;
        this.validator_ = other.validator_;
        this.playerGetter_ = other.playerGetter_;
        this.pointGetter_ = other.pointGetter_;
        this.areaGetter_ = other.areaGetter_;
        this.consumer_ = other.consumer_;
        this.bukkitPriority_ = other.bukkitPriority_;
        this.priority_ = other.priority_;
        this.extends_ = other.extends_;

        return this;
    }
    public AbilityBuilder<E> called(String name) {
        this.name_ = name;
        return this;
    }
    public AbilityBuilder<E> when(Predicate<E> validator) {
        this.validator_ = validator;
        return this;
    }
    public AbilityBuilder<E> at(Function<E, Location> pointGetter) {
        this.pointGetter_ = pointGetter;
        this.areaGetter_ = null;
        return this;
    }
    public AbilityBuilder<E> in(Function<E, Area> areaGetter) {
        this.areaGetter_ = areaGetter;
        this.pointGetter_ = null;
        return this;
    }
    public AbilityBuilder<E> by(Function<E, Player> playerGetter) {
        this.playerGetter_ = playerGetter;
        return this;
    }
    public AbilityBuilder<E> extend(Ability extendAbility) {
        this.extends_ = extendAbility;
        return this;
    }
    public AbilityBuilder<E> prioritize(int priority) {
        this.priority_ = priority;
        return this;
    }
    public AbilityBuilder<E> prioritize(EventPriority priority) {
        this.bukkitPriority_ = priority;
        return this;
    }
    public AbilityBuilder<E> prioritize(int priority, EventPriority eventPriority) {
        this.priority_ = priority;
        this.bukkitPriority_ = eventPriority;
        return this;
    }


    //GETTERS
    protected String getName() {
        return this.name_;
    }
    protected Predicate<Event> getValidator() {
        if (this.validator_ == null) {
            this.validator_ = e -> true;
        }
        return e -> {
            if (!this.eventClass_.isAssignableFrom(e.getClass())) { return false; }
            E cast = this.eventClass_.cast(e);
            return this.validator_.test(cast);
        };
    }
    protected Ability.PointGetter getPointGetter() {
        if (this.areaGetter_ != null) { return null; }
        if (this.pointGetter_ != null) {
            return e -> this.pointGetter_.apply(this.eventClass_.cast(e));
        }
        if (EntityEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> ((EntityEvent) e).getEntity().getLocation();
        }
        Function<Event, Player> playerGetter = this.getPlayerGetter();
        return e -> playerGetter.apply(e).getLocation();
    }
    protected Ability.AreaGetter getAreaGetter() {
        if (this.pointGetter_ != null) { return null; }
        if (this.areaGetter_ != null) {
            return e -> this.areaGetter_.apply(this.eventClass_.cast(e));
        }
        if (BlockEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> {
                BlockEvent be = (BlockEvent) e;
                return new Area(be.getBlock().getBoundingBox(), be.getBlock().getWorld());
            };
        }
        if (PlayerInteractEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> {
                Block b = ((PlayerInteractEvent) e).getClickedBlock();
                if (b == null) return null;
                return new Area(b.getBoundingBox(), b.getWorld());
            };
        }
        throw new IllegalStateException("Cannot build ability. Unable to infer location");
    }
    protected Function<Event, Player> getPlayerGetter() {
        // If explicitly provided, return right away
        if (playerGetter_ != null) {
            return e -> this.playerGetter_.apply(this.eventClass_.cast(e));
        }

        // Else, try to cast to PlayerEvent
        if (PlayerEvent.class.isAssignableFrom(this.eventClass_)) {
            return e -> ((PlayerEvent) e).getPlayer();
        }

        // Else, try to find a 'getPlayer' Method that returns a player
        Method playerGetterMethod = null;
        try {
            playerGetterMethod = this.eventClass_.getMethod("getPlayer");
        } catch (NoSuchMethodException ignored) {}
        if (!(playerGetterMethod != null && playerGetterMethod.getReturnType().equals(Player.class))) {
            throw new IllegalStateException("Cannot build Ability. Unable de infer player from event.");
        }

        final Method finalPlayerGetterMethod = playerGetterMethod;
        return e -> {
            try {
                return (Player) finalPlayerGetterMethod.invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        };
    }


    //FINAL OPERATIONS
    public <T extends Enum<T>> AbilityBuilder.EnumBuilder<E, T> withEnum(Function<E, T> instanceGetter) {
        return new EnumBuilder<>(this, instanceGetter);
    }
    public Ability build() {
        return this.build(null);
    }
    protected Ability build(@Nullable Predicate<E> extraCheck) {

        Ability.PointGetter pg = null; Ability.AreaGetter ag = null; Exception exception = null;
        try {
            ag = this.getAreaGetter();
        } catch (Exception e) { exception = e; }
        try {
            pg = this.getPointGetter();
        } catch (Exception e) { exception = e; }
        if (ag == null && pg == null) {
            throw (exception != null) ? new RuntimeException(exception) : new IllegalStateException("Unable to build ability. Cannot infer location");
        }

        String name = this.getName();
        Predicate<Event> validator = this.getValidator();
        int order = (this.priority_ != null) ? this.priority_ : 0;
        if (extraCheck != null) {
            validator = validator.and(e -> extraCheck.test(this.eventClass_.cast(e)));
        }

        if (pg != null) {
            if (name == null) {
                return new UnNamedAbility(this.eventClass_, validator, this.getPlayerGetter(), pg, order, this.bukkitPriority_, this.extends_);
            }
            return new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), pg, order, this.bukkitPriority_, this.extends_);
        }
        if (name == null) {
            return new UnNamedAbility(this.eventClass_, validator, this.getPlayerGetter(), ag, order, this.bukkitPriority_, this.extends_);
        }
        Ability ability = new Ability(name, this.eventClass_, validator, this.getPlayerGetter(), ag, order, this.bukkitPriority_, this.extends_);

        if (this.consumer_ != null) { this.consumer_.accept(ability); }
        return ability;
    }


    //CLASSES
    public static class EnumBuilder<E extends Event, T extends Enum<T>> {

        private final AbilityBuilder<E> father_;
        private final Function<E, T> supplier_;

        private EnumBuilder(AbilityBuilder<E> father, Function<E, T> supplier) {
            this.father_ = father;
            this.supplier_ = supplier;
        }

        public Ability build(T instance) {
            return this.father_.build(e -> this.supplier_.apply(e).equals(instance));
        }
        public Ability build(Predicate<T> check) {
            return this.father_.build(e -> check.test(this.supplier_.apply(e)));
        }
        public Ability buildOr(T... instances) {
            return this.father_.build(e -> Arrays.stream(instances).anyMatch(i -> this.supplier_.apply(e).equals(i)));
        }
        public Ability buildAnd(T... instances) {
            return this.father_.build(e -> Arrays.stream(instances).allMatch(i -> this.supplier_.apply(e).equals(i)));
        }

    }

    public static class UnNamedAbility extends Ability {

        private String renamed_;

        public UnNamedAbility(Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, PointGetter pointGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
            super("", eventClass, validator, playerGetter, pointGetter, order, priority, dependsOn);
            this.renamed_ = null;
        }
        public UnNamedAbility(Class<? extends Event> eventClass, Predicate<Event> validator, Function<Event, Player> playerGetter, AreaGetter areaGetter, int order, @Nullable EventPriority priority, @Nullable Ability dependsOn) {
            super("", eventClass, validator, playerGetter, areaGetter, order, priority, dependsOn);
            this.renamed_ = null;
        }

        @Override public String getName() {
            if (this.renamed_ == null) {
                throw new IllegalStateException("Trying to access an unnamed ability's name");
            }
            return this.renamed_;
        }

        void setName(String name) {
            if (this.renamed_ != null) {
                throw new IllegalStateException("Cannot name an ability twice");
            }
            this.renamed_ = name;
        }
    }
}
