package com.jkantrell.regionslib.abilities;

import org.bukkit.event.Event;
import java.util.*;
import java.util.function.Predicate;

public class AbilityList<E extends Event> {

    private final HashMap<String, Ability<E>> abilities_ = new HashMap<>();

    public void add(Ability<E> ability) {
        if (this.abilities_.containsKey(ability.name)) {
            throw new IllegalArgumentException("An ability with this name already exists!");
        }
        this.abilities_.put(ability.name, ability);
    }
    public void addAll(Collection<Ability<E>> abilities) {
        for (Ability<E> ability : abilities) {
            this.add(ability);
        }
    }

    public boolean contains(Ability<E> ability) {
        return this.abilities_.containsValue(ability);
    }
    public boolean contains(String name) {
        return this.abilities_.containsKey(name);
    }

    private boolean remove(String name, Predicate<HashMap<String ,Ability<E>>> checker) {
        if (checker.test(this.abilities_)) {
            this.abilities_.remove(name);
            return true;
        }
        return false;
    }
    public boolean remove(Ability<E> ability) {
        return this.remove(ability.name,m -> m.containsValue(ability));
    }
    public boolean remove(String name) {
        return this.remove(name,m -> m.containsKey(name));
    }
    public boolean removeIf(Predicate<Ability<E>> conditional) {
        boolean removed = false;
        for (Ability<E> ability : this.abilities_.values()) {
            if (conditional.test(ability)) {
                this.abilities_.remove(ability.name);
                removed = true;
            }
        }
        return removed;
    }

    public Ability<E> get(String name) {
        return this.abilities_.get(name);
    }
    public List<Ability<E>> getAll(Collection<String> names) {
        ArrayList<Ability<E>> r = new ArrayList<>();
        for (String name : names) {
            if (this.contains(name)) {
                r.add(this.get(name));
            }
        }
        return r;
    }
    public List<Ability<E>> toList() {
        return new ArrayList<Ability<E>>(this.abilities_.values());
    }
    public List<String> getNames() { return new ArrayList<>(this.abilities_.keySet()); }
    public boolean isEmpty() {
        return this.abilities_.isEmpty();
    }

    @Override
    protected AbilityList<E> clone() {
        AbilityList<E> list = new AbilityList<>();
        list.addAll(this.abilities_.values());
        return list;
    }
}