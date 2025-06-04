package com.jkantrell.regionslib.region.hierarchy;

import com.jkantrell.regionslib.region.ability.Ability;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.stream.Collectors;

public class Hierarchy {

    //FIELDS
    private Long id_;
    private String name_;
    private final TreeMap<Integer, Hierarchy.Group> groups_;
    private int lowestLevel_, highestLevel_;

    //STATIC FIELDS
    private static final ArrayList<Hierarchy> hierarchies_ = new ArrayList<>();

    //CONSTRUCTORS
    public Hierarchy (Long id, String name) {
        this.groups_ = new TreeMap<>(Comparator.reverseOrder());
        this.lowestLevel_ = 0;
        this.highestLevel_ = 0;

        this.setId(id);
        this.setName(name);
        hierarchies_.add(this);
    }

    //SETTERS
    private void setId(Long id) {
        this.id_ = id;
    }
    public void setName(String name) {
        this.name_ = name;
    }

    //GETTERS
    public Long getId() {
        return id_;
    }
    public String getName() {
        return name_;
    }
    public List<Hierarchy.Group> getGroups() {
        return List.copyOf(this.groups_.values());
    }
    public Optional<Hierarchy.Group> getGroup(int level) {
        return Optional.ofNullable(this.groups_.get(level));
    }
    public Optional<Hierarchy.Group> getGroupAtOrAbove(int level) {
        return Optional.ofNullable(groups_.ceilingEntry(level)).map(Map.Entry::getValue);
    }
    public Optional<Hierarchy.Group> getGroup(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return this.groups_.values().stream().filter(g -> g.getName().equals(name)).findFirst();
    }
    public int getLowestLever() {
        return this.lowestLevel_;
    }
    public int getHighestLevel() {
        return this.highestLevel_;
    }


    //METHODS
    public boolean checkAbility(Ability ability, int level) {
        if (level <= this.getLowestLever()) {
            return true;
        }

        Hierarchy.Group group = this.getGroupAtOrAbove(level).orElse(null);

        if (group != null && group.allowedTo(ability)) {
            return true;
        }

        for (Hierarchy.Group g : this.groups_.subMap(level, false, this.getLowestLever(), true).values()) {
            if (g.allowedTo(ability)) { return false; }
        }

        return true;
    }
    public boolean checkAbility(Ability ability) {
        return this.checkAbility(ability, this.getHighestLevel() + 1);
    }
    public boolean checkAbility(Ability ability, Hierarchy.Group group) {
        if (!group.getHierarchy().equals(this)) { return false; }
        return checkAbility(ability, group.getLevel());
    }
    public void addGroup(String name, int level, Collection<String> abilities) {
        this.groups_.put(level, new Group(level, name, abilities, this));

        if (this.groups_.size() < 2) {
            this.highestLevel_ = level;
            this.lowestLevel_ = level;
            return;
        }

        if (level < this.lowestLevel_) {
            this.lowestLevel_ = level;
        }

        if (level > this.highestLevel_) {
            this.highestLevel_ = level;
        }
    }

    //CLASSES
    public static class Group implements Comparable<Group> {

        //FIELDS
        private int level_;
        private String name_;
        private Set<String> abilities_;
        private final Hierarchy hierarchy_;

        //CONSTRUCTORS
        private Group(int level, String name, Collection<String> abilities, Hierarchy hierarchy) {
            this.setLevel(level);
            this.setName(name);
            this.setAbilities(abilities);
            this.hierarchy_ = hierarchy;
        }

        //SETTERS
        protected void setLevel(int id_) {
            this.level_ = id_;
        }
        protected void setName(String name_) {
            this.name_ = name_;
        }
        protected void setAbilities(Collection<String> abilities) {
            this.abilities_ = abilities.stream().map(StringUtils::normalizeSpace).collect(Collectors.toSet());
        }

        //GETTERS
        public int getLevel() {
            return level_;
        }
        public String getName() {
            return name_;
        }
        public Hierarchy getHierarchy() {
            return this.hierarchy_;
        }

        //Methods
        @Override
        public int compareTo(Group o) {
            return Integer.compare(level_, o.getLevel());
        }
        public boolean allowedTo(String ability) {
            return this.abilities_.contains(StringUtils.normalizeSpace(ability.toLowerCase()));
        }
        public boolean allowedTo(Ability ability) {
            return this.allowedTo(ability.getName());
        }
    }
}
