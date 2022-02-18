package com.jkantrell.regionslib.regions;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.abilities.Ability;
import com.jkantrell.regionslib.abilities.AbilityHandler;
import com.jkantrell.regionslib.abilities.AbilityList;
import com.jkantrell.regionslib.io.Serializer;
import org.bukkit.event.Event;
import org.checkerframework.checker.units.qual.A;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hierarchy {

    //FIELDS
    private int id_;
    private String name_;
    private final Groups groups_ = new Groups(this);

    //STATIC FIELDS
    private static final ArrayList<Hierarchy> hierarchies_ = new ArrayList<>();

    //CONSTRUCTORS
    public Hierarchy (int id, String name) {
        this.setId(id);
        this.setName(name);

        hierarchies_.add(this);
    }

    //SETTERS
    private void setId(int id) {
        this.id_ = id;
    }
    public void setName(String name) {
        this.name_ = name;
    }

    //GETTERS
    public int getId() {
        return id_;
    }
    public String getName() {
        return name_;
    }
    public Hierarchy.Groups getGroups() {
        return groups_;
    }
    public Hierarchy.Group getGroup(int level) {
        Group g = null;
        for (Group group : groups_) {
            if (group.getLevel() == level) {
                g = group;
                break;
            }
        }
        return g;
    }
    public Hierarchy.Group getGroup(String name) {
        Group g = null;
        for (Group group : groups_) {
            if (group.getName().equals(name)) {
                g = group;
                break;
            }
        }
        return g;
    }
    public int getHighestLevel() {
        Collections.reverse(groups_);
        return groups_.get(0).level_;
    }

    //STATIC METHODS
    public static ArrayList<Hierarchy> loadAll() {
        Serializer.deserializeFileList(Serializer.FILES.HIERARCHIES,Hierarchy.class);
        return hierarchies_;
    }
    public static Hierarchy get(int id) {
        for (Hierarchy hierarchy : hierarchies_) {
            if (hierarchy.getId() == id) {
                return hierarchy;
            }
        }
        return null;
    }

    //METHODS
    public boolean checkAbility(Ability<?> ability, int level) {

        boolean r;
        if(level > 1) {
            Hierarchy.Group group = getGroup(level);
            if (group == null) {
                r = false;
            } else {
                r = this.getGroup(level).allowedTo(ability);
            }
            if (!r){
                for (int i = level-1; i>0 && !r; i--) {
                    group = this.getGroup(i);
                    if (group != null) {
                        r = group.allowedTo(ability);
                    }
                }
                r = !r;
            }
        } else {
            r = true;
        }
        return r;
    }
    public boolean checkAbility(Ability<?> ability) {
        return this.checkAbility(ability, groups_.get(0).getLevel()+1);
    }
    public boolean checkAbility(Ability<?> ability, Hierarchy.Group group) {
        if (!group.getHierarchy().equals(this)) { return false; }
        return checkAbility(ability,group.getLevel());
    }

    //CLASSES
    public class Group implements Comparable<Group> {

        //FIELDS
        private int level_;
        private String name_;
        private List<String> abilities_;
        private final Hierarchy hierarchy_;

        //CONSTRUCTORS
        private Group(int level, String name, List<String> abilities, Hierarchy hierarchy) {
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
        protected void setAbilities(List<String> abilities) {
            this.abilities_ = abilities;
        }

        //GETTERS
        public int getLevel() {
            return level_;
        }
        public String getName() {
            return name_;
        }
        public List<Ability<?>> getAbilities() {
            return new ArrayList<>(RegionsLib.getAbilityHandler().getRegisteredAbilities().getAll(this.abilities_));
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
            return this.abilities_.contains(ability);
        }
        public boolean allowedTo(Ability<?> ability) {
            return this.allowedTo(ability.name);
        }
    }
    public class Groups extends ArrayList<Group> {

        //FIELDS
        private final Hierarchy hierarchy_;

        private Groups(Hierarchy hierarchy) {
            super();
            this.hierarchy_ = hierarchy;
        }

        @Override
        @Deprecated
        public boolean add(Group group) {
            if (!group.getHierarchy().equals(this.hierarchy_)) {
                return false;
            }
            boolean r = super.add(group);
            Collections.reverse(this);
            return r;
        }
        public boolean add(String name, int level, List<String> abilities){
            boolean r = super.add(new Group(
                    level, name, abilities, this.hierarchy_
            ));
            Collections.reverse(this);
            return r;
        }
        public boolean add(String name, int level, AbilityList abilities){
            return this.add(name,level,abilities.getNames());
        }
    }

    public static class JDeserializer implements JsonDeserializer<Hierarchy> {

        @Override
        public Hierarchy deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            Gson gson = Serializer.GSON;
            JsonObject jsonHierarchy = json.getAsJsonObject();

            Hierarchy hierarchy = new Hierarchy(
                    jsonHierarchy.get("id").getAsInt(),
                    jsonHierarchy.get("name").getAsString()
            );

            JsonObject jsonGroup; ArrayList<String> abilities;
            for (JsonElement element : jsonHierarchy.get("groups").getAsJsonArray()) {
                jsonGroup = element.getAsJsonObject();
                abilities = new ArrayList<>();
                for (JsonElement element1 : jsonGroup.get("abilities").getAsJsonArray()) {
                    abilities.add(element1.getAsString());
                }
                hierarchy.getGroups().add(
                        jsonGroup.get("name").getAsString(),
                        jsonGroup.get("level").getAsInt(),
                        abilities
                );
            }

            return hierarchy;
        }
    }
}
