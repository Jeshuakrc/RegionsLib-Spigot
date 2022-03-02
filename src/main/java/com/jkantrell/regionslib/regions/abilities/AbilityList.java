package com.jkantrell.regionslib.regions.abilities;

import org.bukkit.event.Event;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;

public class AbilityList<E extends Event> {

    private final HashMap<String, Ability<E>> abilities_ = new HashMap<>();

    //ADD METHODS
    public void add(Ability<E> ability) {
        if (this.abilities_.containsKey(ability.getName())) {
            throw new IllegalArgumentException("An ability with this name already exists!");
        }
        this.abilities_.put(ability.getName(), ability);
    }
    public void addAll(Collection<Ability<E>> abilities) {
        for (Ability<E> ability : abilities) {
            this.add(ability);
        }
    }

    //CONTAIN CHECK METHODS
    public boolean contains(Ability<E> ability) {
        return this.abilities_.containsValue(ability);
    }
    public boolean contains(String name) {
        return this.abilities_.containsKey(name);
    }

    //REMOVE METHODS
    private boolean remove(String name, Predicate<HashMap<String ,Ability<E>>> checker) {
        if (checker.test(this.abilities_)) {
            this.abilities_.remove(name);
            return true;
        }
        return false;
    }
    public boolean remove(Ability<E> ability) {
        return this.remove(ability.getName(),m -> m.containsValue(ability));
    }
    public boolean remove(String name) {
        return this.remove(name,m -> m.containsKey(name));
    }
    public boolean removeIf(Predicate<Ability<E>> conditional) {
        boolean removed = false;
        ArrayList<Ability<E>> list = new ArrayList<>(this.abilities_.values());
        for (int i = 0; i < list.size(); i++) {
            if (conditional.test(list.get(i))) {
                this.abilities_.remove(list.get(i).getName());
                removed = true;
            }
        }
        return removed;
    }

    //GET METHODS
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
    public List<String> getNames() { return new ArrayList<>(this.abilities_.keySet()); }
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
        return new ArrayList<Ability<E>>(this.abilities_.values());
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
        list.addAll(this.abilities_.values());
        return list;
    }
}