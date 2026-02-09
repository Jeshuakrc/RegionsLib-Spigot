package com.kntrel.mc.regionLib.region.hierarchy;

import com.kntrel.mc.regionLib.region.ability.Ability;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a hierarchy of permission groups and ability access rules.
 */
public class Hierarchy {

    //FIELDS
    private Long id_;
    private String name_;
    private final TreeMap<Integer, Hierarchy.Group> groups_;
    private int lowestLevel_, highestLevel_;


    //CONSTRUCTORS
    /**
     * Creates a hierarchy with the provided id and name.
     *
     * @param id hierarchy id
     * @param name hierarchy name
     */
    public Hierarchy (Long id, String name) {
        this.groups_ = new TreeMap<>(Comparator.reverseOrder());
        this.lowestLevel_ = 0;
        this.highestLevel_ = 0;

        this.setId(id);
        this.setName(name);
    }

    //SETTERS
    private void setId(Long id) {
        this.id_ = id;
    }
    /**
     * Sets the hierarchy name.
     *
     * @param name hierarchy name
     */
    public void setName(String name) {
        this.name_ = name;
    }

    //GETTERS
    /**
     * Returns the hierarchy id.
     *
     * @return hierarchy id
     */
    public Long getId() {
        return id_;
    }
    /**
     * Returns the hierarchy name.
     *
     * @return hierarchy name
     */
    public String getName() {
        return name_;
    }
    /**
     * Returns groups in this hierarchy.
     *
     * @return list of groups
     */
    public List<Hierarchy.Group> getGroups() {
        return List.copyOf(this.groups_.values());
    }
    /**
     * Returns the group at a specific level.
     *
     * @param level group level
     * @return optional group
     */
    public Optional<Hierarchy.Group> getGroup(int level) {
        return Optional.ofNullable(this.groups_.get(level));
    }
    /**
     * Returns the nearest group at or below the level.
     *
     * @param level level to search
     * @return optional group
     */
    public Optional<Hierarchy.Group> getGroupAtOrBellow(int level) {
        return Optional.ofNullable(groups_.floorEntry(level)).map(Map.Entry::getValue);
    }
    /**
     * Returns the lowest group allowed to an ability name.
     *
     * @param ability ability name
     * @return optional group
     */
    public Optional<Hierarchy.Group> getLowestGroupAllowedTo(String ability) {
        return this.getGroups().stream()
                .filter(g -> g.allowedTo(ability))
                .sorted()
                .findFirst();
    }
    /**
     * Returns the lowest group allowed to an ability.
     *
     * @param ability ability instance
     * @return optional group
     */
    public Optional<Hierarchy.Group> getLowestGroupAllowedTo(Ability ability) {
        return this.getLowestGroupAllowedTo(ability.name());
    }
    /**
     * Returns the group with the specified name.
     *
     * @param name group name
     * @return optional group
     */
    public Optional<Hierarchy.Group> getGroup(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return this.groups_.values().stream().filter(g -> g.getName().equals(name)).findFirst();
    }
    /**
     * Returns the lowest group level in the hierarchy.
     *
     * @return lowest level
     */
    public int getLowestLever() {
        return this.lowestLevel_;
    }
    /**
     * Returns the highest group level in the hierarchy.
     *
     * @return highest level
     */
    public int getHighestLevel() {
        return this.highestLevel_;
    }


    //METHODS
    /**
     * Checks whether a group level is allowed to use an ability.
     *
     * @param ability ability to check
     * @param level group level
     * @return true if allowed
     */
    public boolean checkAbility(Ability ability, int level) {
        if (level <= this.getLowestLever()) {
            return true;
        }

        Hierarchy.Group group = this.getGroupAtOrBellow(level).orElse(null);

        if (group != null && group.allowedTo(ability)) {
            return true;
        }

        for (Hierarchy.Group g : this.groups_.subMap(level, false, this.getLowestLever(), true).values()) {
            if (g.allowedTo(ability)) { return false; }
        }

        return true;
    }
    /**
     * Checks whether the ability is allowed for the lowest group.
     *
     * @param ability ability to check
     * @return true if allowed
     */
    public boolean checkAbility(Ability ability) {
        return this.checkAbility(ability, this.getHighestLevel() + 1);
    }
    /**
     * Checks whether a group is allowed to use an ability.
     *
     * @param ability ability to check
     * @param group hierarchy group
     * @return true if allowed
     */
    public boolean checkAbility(Ability ability, Hierarchy.Group group) {
        if (!group.getHierarchy().equals(this)) { return false; }
        return checkAbility(ability, group.getLevel());
    }
    /**
     * Adds a group definition to the hierarchy.
     *
     * @param name group name
     * @param level group level
     * @param abilities ability names
     */
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

    private static final Pattern WS = Pattern.compile("\\s+");
    private static String normalizeSpace(String s) {
        return WS.matcher(s.trim()).replaceAll(" ");
    }

    //CLASSES
    /**
     * Represents a hierarchy group with a level and allowed abilities.
     */
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
            this.abilities_ = abilities.stream().map(Hierarchy::normalizeSpace).collect(Collectors.toSet());
        }

        //GETTERS
        /**
         * Returns the group level.
         *
         * @return group level
         */
        public int getLevel() {
            return level_;
        }
        /**
         * Returns the group name.
         *
         * @return group name
         */
        public String getName() {
            return name_;
        }
        /**
         * Returns the parent hierarchy.
         *
         * @return hierarchy instance
         */
        public Hierarchy getHierarchy() {
            return this.hierarchy_;
        }

        //Methods
        @Override
        /**
         * Compares groups by level.
         *
         * @param o other group
         * @return comparison result
         */
        public int compareTo(Group o) {
            return Integer.compare(level_, o.getLevel());
        }
        /**
         * Returns whether the group allows the ability name.
         *
         * @param ability ability name
         * @return true if allowed
         */
        public boolean allowedTo(String ability) {
            return this.abilities_.contains(normalizeSpace(ability.toLowerCase()));
        }
        /**
         * Returns whether the group allows the ability.
         *
         * @param ability ability instance
         * @return true if allowed
         */
        public boolean allowedTo(Ability ability) {
            return this.allowedTo(ability.name());
        }
    }
}
