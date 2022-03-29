package com.jkantrell.regionslib.regions.abilities;

import org.bukkit.event.Event;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

public class AbilityList<E extends Event> {

    private final ArrayList<Ability<E>> abilities_ = new ArrayList<>();

    //ADD METHODS
    public void add(Ability<E> ability) {
        this.abilities_.add(ability);
    }
    public void addAll(Collection<Ability<E>> abilities) {
        for (Ability<E> ability : abilities) {
            this.add(ability);
        }
    }

    //CONTAIN CHECK METHODS
    public boolean contains(Ability<E> ability) {
        return this.abilities_.contains(ability);
    }
    public boolean contains(String name) {
        for (Ability<E> ability : this.abilities_) {
            if (ability.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    //REMOVE METHODS
    private boolean remove(Ability<E> ability, Predicate<ArrayList<Ability<E>>> checker) {
        if (checker.test(this.abilities_)) {
            this.abilities_.remove(ability);
            return true;
        }
        return false;
    }
    public boolean remove(Ability<E> ability) {
        return this.remove(ability,m -> m.contains(ability));
    }
    public boolean remove(String name) {
        Ability<E> ability = this.get(name);
        return this.remove(ability,m -> m.contains(ability));
    }
    public boolean removeIf(Predicate<Ability<E>> conditional) {
        boolean removed = false;
        for (Ability<E> ability : this.abilities_) {
            if (conditional.test(ability)) {
                this.abilities_.remove(ability);
                removed = true;
            }
        }
        return removed;
    }

    //GET METHODS
    public Ability<E> get(String name) {
        for (Ability<E> ability : this.abilities_) {
            if (ability.getName().equals(name)) {
                return ability;
            }
        }
        return null;
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
    public List<String> getNames() {
        ArrayList<String> list = new ArrayList<>();
        for (Ability<E> ability : abilities_) {
            list.add(ability.getName());
        }
        return list;
    }
    public boolean isEmpty() {
        return this.abilities_.isEmpty();
    }
    public AbilityList<E> getRemovedIf(Predicate<Ability<E>> conditional) {
        AbilityList<E> newList = this.clone();
        newList.removeIf(conditional);
        return newList;
    }

    //LIST METHODS
    public int size() {
        return this.abilities_.size();
    }
    public List<Ability<E>> toList() {
        return new ArrayList<>(this.abilities_);
    }
    public List<Ability<E>> prioritize() {
        List<Ability<E>> list = this.toList();
        Collections.sort(list);
        return list;
    }

    //MISCELLANEOUS METHODS
    @Override
    protected AbilityList<E> clone() {
        AbilityList<E> list = new AbilityList<>();
        list.addAll(this.abilities_);
        return list;
    }
}