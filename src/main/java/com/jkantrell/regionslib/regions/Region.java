package com.jkantrell.regionslib.regions;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.regions.abilities.Ability;
import com.jkantrell.regionslib.regions.dataContainers.RegionDataContainer;
import com.jkantrell.regionslib.io.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;

public class Region {

    //FIELDS
    private int id_;
    private double[] vertex_ = null;
    private World world_ = null;
    private Permission[] permissions_;
    private String name_;
    private boolean enabled_ = true;
    private RegionBoundary boundary_ = null;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private RegionBoundingBox boundingBox_;
    private BoundaryDisplayer boundaryDisplayer_ = null;
    private Hierarchy hierarchy_;
    private final List<Rule> rules_ = new ArrayList<>();
    private final Config.ParticleData boundaryParticle_ = RegionsLib.CONFIG.regionBorderParticle;
    private final Region self_ = this;

    //CONSTRUCTORS
    public Region(double[] vertex, World world, Permission[] permissions, String name, Hierarchy hierarchy) {
        this.setId(getHighestId(regions_.toArray(Region[]::new)) + 1);
        this.setWorld(world);
        this.setVertex(vertex);
        this.setPermissions(permissions);
        this.setName(name);
        this.setHierarchy(hierarchy);
    }

    //STATIC FIELDS
    private static List<Region> regions_ = new ArrayList<>();

    //SETTERS
    public void setVertex(double[] vertex) {

        double min, max;
        for (int i = 0; i < 3; i++) {
            min = Math.min(vertex[i], vertex[i + 3]);
            max = Math.max(vertex[i], vertex[i + 3]);
            vertex[i] = min; vertex[i+3] = max;
        }

        vertex_ = vertex;
        this.setBoundingBox_();
        this.attemptCalculateBoundaries_();
    }
    public void setWorld(World world) {
        world_ = world;
        this.attemptCalculateBoundaries_();
    }
    public void setPermissions(Permission[] permissions) {
        permissions_ = permissions;
    }
    public void setId(int id) {
        id_ = id;
    }
    public void setName(String name) {
        name_ = name;
    }
    public void enabled(boolean bool) {
        enabled_ = bool;
    }
    public void setDataContainer(RegionDataContainer dataContainer){
        dataContainer_ = dataContainer;
    }
    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy_ = hierarchy;
    }

    //GETTERS
    public Permission[] getPermissions() {
        return this.permissions_;
    }
    public int getId() {
        return this.id_;
    }
    public World.Environment getDimension() {
        return this.world_.getEnvironment();
    }
    public String getName() {
        return this.name_;
    }
    public double[] getVertex() {
        return this.vertex_;
    }
    public boolean isEnabled() {
        return this.enabled_;
    }
    public RegionBoundary getBoundary(){ return boundary_; }
    public RegionDataContainer getDataContainer(){ return dataContainer_; }
    public RegionBoundingBox getBoundingBox(){
        return boundingBox_;
    }
    public double getHeight(){
        return this.getBoundingBox().getHeight();
    }
    public double getWidthX(){
        return this.getBoundingBox().getWidthX();
    }
    public double getWidthZ(){
        return this.getBoundingBox().getWidthZ();
    }
    public World getWorld() {
        return this.world_;
    }
    public List<Player> getGroupLevelRagePlayers(int min, int max) {
       List <Player> r = new ArrayList<>();
        for (Permission p : this.getPermissions()) {
            int lvl = p.getGroup().getLevel();
            if (lvl <= max && lvl >= min) {
                r.add(p.getPlayer());
            }
        }
        return r;
    }
    public Hierarchy getHierarchy() {
        return this.hierarchy_;
    }
    public Rule[] getRules() {
        return this.rules_.toArray(new Rule[0]);
    }
    public <T> Rule getRule(String name, Rule.DataType<T> dataType) {
        for (Rule rule : this.rules_) {
            if (rule.name.equals(name) && rule.getKey().dataType.equals(dataType)) {
               return rule;
            }
        }
        return null;
    }
    public <T> T getRuleValue(String name, Rule.DataType<T> dataType) {
        try {
            return this.getRule(name, dataType).getValue(dataType);
        } catch (NullPointerException e) {
            return null;
        }
    }

    //STATIC METHODS
    public static List<Region> loadAll() {
        regions_ = Serializer.deserializeFileList(Serializer.FILES.REGIONS, Region.class);
        return regions_;
    }
    public static List<Region> getAll() {
        return regions_;
    }
    public static Region[] getAt(double x, double y, double z, World world, Predicate<Region> checker){
        List<Region> l = new ArrayList<>(Collections.emptyList());
        for (Region r : regions_) {
            if (r.contains(x, y, z, world) && checker.test(r)) {
                l.add(r);
            }
        }
        return l.toArray(new Region[0]);
    }
    public static Region[] getAt(Location location, Predicate<Region> checker){
        return getAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()),checker);
    }
    public static Region[] getAt(double x, double y, double z, World world) {
        return getAt(x, y, z, world, Region::isEnabled);
    }
    public static Region[] getAt(Location location) {
        return getAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()));
    }
    public static Region[] getAllAt(double x, double y, double z, World world) {
        return getAt(x,y,z,world, r -> true);
    }
    public static Region[] getAllAt(Location location) {
        return getAllAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()));
    }
    public static Region[] getRuleContainersAt(String ruleName, Location location) {
        return getAt(location, region -> region.hasRule(ruleName));
    }
    public static boolean checkAbilityIn(Region[] regions, Player player, Ability<?> ability) {

        boolean r = true;
        List<Region> list = new ArrayList<>();
        for(Region i : regions){
            if(i.isEnabled()){
                list.add(i);
            }
        }
        regions=list.toArray(new Region[0]);

        if (regions.length > 0) {
            int pos = 0;
            switch (RegionsLib.CONFIG.overlappingPermissionsMode) {
                case all -> {
                    r = true;
                    for (Region i : regions) {
                        if (!i.checkAbility(player, ability)) {
                            r = false;
                            break;
                        }
                    }
                }
                case any -> {
                    r = false;
                    for (Region i : regions) {
                        if (i.checkAbility(player, ability)) {
                            r = true;
                            break;
                        }
                    }
                }
                case oldest -> {
                    int min = getHighestId(regions);
                    for (int i = 0; i < regions.length; i++) {
                        if (regions[i].getId() < min) {
                            min = regions[i].getId();
                            pos = i;
                        }
                    }
                    r = regions[pos].checkAbility(player, ability);
                }
                case newest -> {
                    int max = 0;
                    for (int i = 0; i < regions.length; i++) {
                        if (regions[i].getId() > max) {
                            max = regions[i].getId();
                            pos = i;
                        }
                    }
                    r = regions[pos].checkAbility(player, ability);
                }
                default -> {
                }
            }
        }
        return r;
    }
    public static boolean checkAbilityAt(Player player, Ability<?> ability, double x, double y, double z, World world) {
        Region[] regions = getAt(x, y, z, world);
        return checkAbilityIn(regions, player, ability);
    }
    public static boolean checkAbilityAt(Player player, Ability<?> ability, Location location) {
        if (location == null) { return true; }
        return checkAbilityAt(player,ability,location.getX(),location.getY(),location.getZ(),location.getWorld());
    }
    public static int getHighestId(Region[] regions) {

        int max = 0;
        for (Region i : regions) {
            max = Math.max(max, i.getId());
        }
        return max;
    }
    public static Region getFromId(int id) {

        Region r = null;
        for (Region i : getAll()) {
            if (i.getId() == id) {
                r = i;
                break;
            }
        }
        return r;
    }
    public static void addRegion(Region region) {
        regions_.add(region);
    }
    public static List<Region> getRegionsOverlapping(double[] vertex) {

        List<Region> r = new ArrayList<>();
        BoundingBox box1 = new BoundingBox(vertex[0],vertex[1],vertex[2],vertex[3],vertex[4],vertex[5]);
        for (Region i : Region.getAll()) {
            RegionBoundingBox box2 = i.getBoundingBox();
            if(box2.overlaps(box1)){
                r.add(i);
            }
        }
        return r;
    }

    //PUBLIC METHODS
    public boolean checkAbility( Player player, Ability<?> ability) {

        if(!this.isEnabled()) { return true; }
        Permission perm = null;
        if (player != null) {
            for (Permission i : permissions_) {
                if (i.getPlayerName().equals(player.getName())) {
                    perm = i;
                    break;
                }
            }
        }
        return (perm == null) ? this.getHierarchy().checkAbility(ability) : this.getHierarchy().checkAbility(ability,perm.getGroup());
    }
    public boolean contains(double x, double y, double z, World dimension) {
        if(!this.getWorld().equals(dimension)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    public void save() {
        if (!regions_.contains(this)) {
            Region.addRegion(this);
        }
        Serializer.serializeToFile(Serializer.FILES.REGIONS,regions_);
    }
    public List<Region> getOverlappingRegions() {

        List<Region> l = Region.getRegionsOverlapping(this.getVertex());
        l.remove(this);
        return l;
    }
    public void destroy() {
        if(boundaryDisplayer_ != null) {
            if (!boundaryDisplayer_.displayer.isCancelled()) {
                this.boundaryDisplayer_.cancel();
            }
        }
        regions_.remove(this);
        Serializer.serializeToFile(Serializer.FILES.REGIONS, regions_);
    }
    public void displayBoundaries(int frequency ,long persistence) {
        if(boundaryDisplayer_ != null) { boundaryDisplayer_.cancel(); }

        boundaryDisplayer_ = new BoundaryDisplayer();
        boundaryDisplayer_.displayBoundaries(frequency,persistence);
    }
    public void broadCastToMembers(String message, int maxLevel) {

    }
    public void broadCastToMembersLang(String path, String[] args, int maxLevel) {
        //MUST BE DECOUPLED

        //Landlords.Utils.broadcastMessageLang(path,args,this.getGroupLevelRagePlayers(0,maxLevel));
    }
    public void clearRules() {
        this.rules_.clear();
    }
    public boolean removeRule(String name) {
        for (int i = 0; i < this.rules_.size(); i++) {
            Rule rule = this.rules_.get(i);
            if (rule.name.equals(name)) {
                this.rules_.remove(rule);
                return true;
            }
        }
        return false;
    }
    public void addRule(Rule rule) {
        this.rules_.add(rule);
    }
    public <T> void  addRule(String name, Rule.DataType<T> dataType, T value) {
        this.rules_.add(new Rule(name,dataType,value));
    }
    public Boolean hasRule(String name) {
        for (Rule rule : this.rules_) {
            if (rule.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    //PRIVATE METHODS
    private void attemptCalculateBoundaries_() {

        if(vertex_ != null && world_ != null){
            if (boundary_ == null) {
                boundary_ = new RegionBoundary(this);
            }else{
                boundary_.recalculate();
            }
        }
    }
    private void setBoundingBox_() {

        boolean cond;
        RegionBoundingBox box = this.getBoundingBox();
        if(box == null) { cond = true; } else { cond = box.getCorners() != this.getVertex(); }
        if (cond) {
            double[] v = this.getVertex();
            boundingBox_ = new RegionBoundingBox(v[0], v[1], v[2], v[3], v[4], v[5], this);
        }
    }

    //PRIVATE CLASSES
    private class BoundaryDisplayer {

        //FIELDS
        boolean ran = false;

        //RUNNABLES
        private final BukkitRunnable displayer = new BukkitRunnable() {
            @Override
            public void run() {
                for (Location l : self_.getBoundary().getFullBoundaries()) {
                    Objects.requireNonNull(l.getWorld()).spawnParticle(
                            boundaryParticle_.particle(),
                            l,
                            boundaryParticle_.count(),
                            boundaryParticle_.delta()[0], boundaryParticle_.delta()[1], boundaryParticle_.delta()[2],
                            0, null, true
                    );
                }
            }
        };

        private final BukkitRunnable canceller = new BukkitRunnable() {
            @Override
            public void run() {
                displayer.cancel();
            }
        };

        //METHODS
        protected void displayBoundaries(int frequency, long persistence) {
            displayer.runTaskTimerAsynchronously(RegionsLib.getMain(), 0, frequency);
            canceller.runTaskLater(RegionsLib.getMain(), persistence);
            ran = true;
        }

        protected void cancel() {
            if (!ran) { return; }

            if (!displayer.isCancelled()) {
                canceller.cancel(); displayer.cancel();
            }
        }
    }

    public static class JSerializer implements JsonSerializer<Region> {

        @Override
        public JsonElement serialize(Region src, Type typeOfSrc, JsonSerializationContext context) {

            JsonObject jsonRegion = new JsonObject();
            jsonRegion.addProperty("id",src.getId());
            jsonRegion.addProperty("name",src.getName());
            jsonRegion.addProperty("hierarchy",src.getHierarchy().getId());
            jsonRegion.addProperty("world",src.getWorld().getName());
            jsonRegion.addProperty("enabled",src.isEnabled());
            jsonRegion.add("vertex", new Gson().toJsonTree(src.getVertex()));

            JsonArray jsonPermissions = new JsonArray();
            for (Permission permission : src.getPermissions()) {
                jsonPermissions.add(Serializer.GSON.toJsonTree(permission));
            }
            jsonRegion.add("permissions",jsonPermissions);

            if (src.getRules().length > 0) {
                JsonObject jsonRules = new JsonObject(); JsonObject jsonRule;
                for (Rule rule : src.getRules()) {
                     jsonRule = Serializer.GSON.toJsonTree(rule).getAsJsonObject();
                     String name = jsonRule.keySet().iterator().next();
                     jsonRules.add(name,jsonRule.get(name));
                }
                jsonRegion.add("rules",jsonRules);
            }

            if (!src.getDataContainer().isEmpty()) {
                 jsonRegion.add("data_container", Serializer.GSON.toJsonTree(src.getDataContainer()));
            }

            return jsonRegion;
        }
    }
    public static class JDeserializer implements JsonDeserializer<Region> {

        @Override
        public Region deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            Gson gson = Serializer.GSON;
            JsonObject jsonRegion = json.getAsJsonObject();
            JsonArray jsonPermissions = jsonRegion.getAsJsonArray("permissions");
            Permission[] permissions = new Permission[jsonPermissions.size()];
            Hierarchy hierarchy = Hierarchy.get(jsonRegion.get("hierarchy").getAsInt());
            JsonObject jsonPermission;
            for (int i = 0; i < permissions.length; i++) {
                jsonPermission = jsonPermissions.get(i).getAsJsonObject();
                permissions[i] = new Permission(
                        jsonPermission.get("player_name").getAsString(),
                        hierarchy,
                        jsonPermission.get("level").getAsInt()
                );
            }

            Region region = new Region(
                    gson.fromJson(jsonRegion.get("vertex"),double[].class),
                    Bukkit.getWorld(jsonRegion.get("world").getAsString()),
                    permissions,
                    jsonRegion.get("name").getAsString(),
                    hierarchy
            );
            region.setId(jsonRegion.get("id").getAsInt());
            region.enabled(jsonRegion.get("enabled").getAsBoolean());

            if (jsonRegion.has("data_container")) {
                region.setDataContainer(gson.fromJson(jsonRegion.get("data_container"),RegionDataContainer.class));
            }

            if (jsonRegion.has("rules")) {
                JsonObject jsonRules = jsonRegion.get("rules").getAsJsonObject();
                for (Map.Entry<String,JsonElement> entry : jsonRules.entrySet()) {
                    JsonObject jsonRule = new JsonObject();
                    jsonRule.add(entry.getKey(),entry.getValue());
                    region.addRule(gson.fromJson(jsonRule,Rule.class));
                }
            }
            return region;
        }
    }
}