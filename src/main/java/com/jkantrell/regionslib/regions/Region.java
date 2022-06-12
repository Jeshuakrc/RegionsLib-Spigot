package com.jkantrell.regionslib.regions;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.RegionsLibEventListener;
import com.jkantrell.regionslib.events.PlayerEnterRegionEvent;
import com.jkantrell.regionslib.events.PlayerLeaveRegionEvent;
import com.jkantrell.regionslib.events.RegionCreateEvent;
import com.jkantrell.regionslib.events.RegionDestroyEvent;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.regions.abilities.Ability;
import com.jkantrell.regionslib.regions.dataContainers.RegionDataContainer;
import com.jkantrell.regionslib.io.Serializer;
import com.jkantrell.regionslib.regions.rules.Rule;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import com.jkantrell.regionslib.regions.rules.RuleKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;

public class Region implements Comparable<Region> {

    //FIELDS
    private int id_;
    private double[] vertex_ = null;
    private World world_ = null;
    private final ArrayList<Permission> permissions_ = new ArrayList<>();
    private String name_;
    private boolean enabled_ = true;
    private boolean isDestroyed_ = false;
    private RegionBoundary boundary_ = null;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private RegionBoundingBox boundingBox_;
    private BoundaryDisplayer boundaryDisplayer_ = null;
    private Hierarchy hierarchy_;
    private final List<Rule> rules_ = new ArrayList<>();
    private final Config.ParticleData boundaryParticle_ = RegionsLib.CONFIG.regionBorderParticle;
    private final Region self_ = this;
    private final LinkedList<Player> insidePlayers_ = new LinkedList<>();

    //CONSTRUCTORS
    public Region(double[] vertex, World world, String name, Hierarchy hierarchy, @Nullable Entity creator) {
        this.setId(Region.getHighestId() + 1);
        this.setWorld(world);
        this.setVertex(vertex);
        this.setName(name);
        this.setHierarchy(hierarchy);

        if (creator instanceof Player player) {
            this.addPermission(player,1);
        }

        RegionCreateEvent event = new RegionCreateEvent(this, creator);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { this.destroy(); }
    }
    public Region(double[] vertex, World world, String name, Hierarchy hierarchy) {
        this(vertex, world, name, hierarchy, null);
    }

    //STATIC FIELDS
    private static List<Region> regions_ = new ArrayList<>();
    private static BukkitRunnable playerSampler_ = null;

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
        this.permissions_.clear();
        Arrays.stream(permissions).forEach(this::addPermission);
    }
    public void setId(int id) {
        if (Region.regions_.stream().anyMatch(r -> r.getId() == id)) {
            throw new IllegalArgumentException("A region with Id " + id + " already exists." );
        }
        this.id_ = id;
    }
    public void setName(String name) {
        int l = name.length();

        int compare = RegionsLib.CONFIG.minNameLength;
        if (l < compare) { throw new IllegalArgumentException("The name is too short! Regions names may have a minimum of " + compare + " characters."); }

        compare = RegionsLib.CONFIG.maxNameLength;
        if (l > compare && compare > 0) { throw new IllegalArgumentException("The name is too long! Regions names may have a maximum of " + compare + " characters."); }

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
    public boolean isDestroyed() {
        return this.isDestroyed_;
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
    public Rule getRule(String name) {
        return this.rules_.stream().filter(r -> r.getLabel().equals(name)).findFirst().orElse(null);
    }
    public <T> T getRuleValue(String name, RuleDataType<T> dataType) {
        Rule rule = this.getRule(name);
        if (rule == null) return null;
        if (!rule.getDatatype().equals(dataType)) { return null; }
        return rule.getValue(dataType);
    }
    public List<Player> getInsidePlayers() {
        LinkedList<Player> r = new LinkedList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.contains(player.getLocation())) {
                r.add(player);
            }
        }
        return r;
    }
    public Permission[] getPermissions() {
        return this.permissions_.toArray(new Permission[0]);
    }
    public Permission[] getPermissions(Player player) {
        return this.permissions_.stream().filter(p -> p.getPlayerName().equals(player.getName())).toList().toArray(new Permission[0]);
    }
    public Permission getPermission(Player player) {
        return this.permissions_.stream().filter(p -> p.getPlayerName().equals(player.getName())).findFirst().orElse(null);
    }
    public Hierarchy.Group getGroup(Player player) {
        return this.getPermission(player).getGroup();
    }

    //STATIC METHODS
    public static Region get(int id) {
        return Region.regions_.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }
    public static Region[] get(String name) {
        return Region.getAll(r -> r.getName().equals(name));
    }
    public static Region[] loadAll() {
        Region.regions_ = Serializer.deserializeFileList(Serializer.FILES.REGIONS, Region.class);
        return Region.regions_.toArray(new Region[0]);
    }
    public static Region[] getAll() {
        return Region.getAll(r -> true);
    }
    public static Region[] getAll(Predicate<Region> condition) {
        return Region.regions_.stream().filter(condition).toArray(Region[]::new);
    }
    public static Region[] getAt(double x, double y, double z, World world, Predicate<Region> condition){
        return Region.getAllAt(x,y,z,world, r -> r.isEnabled() && condition.test(r));
    }
    public static Region[] getAt(Location location, Predicate<Region> condition){
        return Region.getAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()),condition);
    }
    public static Region[] getAt(double x, double y, double z, World world) {
        return Region.getAt(x, y, z, world, r -> true);
    }
    public static Region[] getAt(Location location) {
        return Region.getAt(location, r -> true);
    }
    public static Region[] getAllAt(double x, double y, double z, World world) {
        return Region.getAllAt(x,y,z,world, r -> true);
    }
    public static Region[] getAllAt(Location location) {
        return Region.getAllAt(location, r -> true);
    }
    public static Region[] getAllAt(double x, double y, double z, World world, Predicate<Region> condition) {
        return Region.getAll(r -> r.contains(x,y,z,world) && condition.test(r));
    }
    public static Region[] getAllAt(Location location, Predicate<Region> condition) {
        return Region.getAllAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()),condition);
    }
    public static Region[] getRuleContainersAt(String ruleName, RuleDataType dataType, Location location) {
        return getAt(location, region -> region.hasRule(ruleName, dataType));
    }
    public static int getHighestId() {
        return Region.regions_.stream().map(Region::getId).max(Integer::compare).orElse(0);
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
    public static void setPlayerSampling(long rate) {
        if (Region.playerSampler_ != null) { Region.playerSampler_.cancel(); }
        Region.playerSampler_ = new BukkitRunnable() {
            @Override
            public void run() {
                Region.regions_.forEach(
                    r -> r.setInsidePlayers_(
                        Bukkit.getOnlinePlayers().stream().filter(p -> r.contains(p.getLocation())).toList()
                    )
                );
            }
        };
        Region.playerSampler_.runTaskTimer(RegionsLib.getMain(),0, rate);

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
    public boolean contains(Location location) {
        return this.contains(location.getX(),location.getY(),location.getZ(),location.getWorld());
    }
    public boolean contains(double x, double y, double z, World dimension) {
        if(!this.getWorld().equals(dimension)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    public void save() {
        if (this.isDestroyed_) {
            RegionsLib.getMain().getLogger().fine("Region '" + this.getName() + "' cannot be saved as it has been destroyed.");
            return;
        }
        Region.regions_.add(this);
        Serializer.serializeToFile(Serializer.FILES.REGIONS,regions_);
    }
    public List<Region> getOverlappingRegions() {

        List<Region> l = Region.getRegionsOverlapping(this.getVertex());
        l.remove(this);
        return l;
    }
    public void destroy(@Nullable Entity destructor) {
        RegionDestroyEvent event = new RegionDestroyEvent(this, destructor);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        if(boundaryDisplayer_ != null && !boundaryDisplayer_.displayer.isCancelled()) { this.boundaryDisplayer_.cancel(); }
        if (Region.regions_.remove(this)) {
            Serializer.serializeToFile(Serializer.FILES.REGIONS, Region.regions_);
        }
        this.insidePlayers_.clear();
        this.isDestroyed_ = true;
    }
    public void destroy() {
        this.destroy(null);
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
    public boolean removeRule(String label) {
        return this.rules_.removeIf(r -> r.getLabel().equals(label));
    }
    public void addRule(Rule rule) {
        this.rules_.add(rule);
    }
    public <T> void  addRule(String name, RuleDataType<T> dataType, T value) {
        this.rules_.add(new Rule(name,dataType,value));
    }
    public boolean hasRule(RuleKey key) {
        return this.hasRule(key.getLabel(),key.getDataType());
    }
    public boolean hasRule(String name, RuleDataType<?> dataType) {
        return this.rules_.stream().anyMatch(r -> r.getLabel().equals(name) && r.getDatatype().equals(dataType));
    }
    public void clearPermissions() {
        this.permissions_.clear();
    }
    public void addPermission(Permission permission) {
        this.permissions_.add(permission);

        if (permission.getGroup().getLevel() > 1 || !this.hasRule("localMod", RuleDataType.BOOL)) { return; }
        if (!this.getRule("localMod").getValue(RuleDataType.BOOL)) { return; }

        RegionsLibEventListener.addPermissionRegistration(permission.getPlayerName(),"regions.mod.local");
        RegionsLib.getMain().getLogger().info(
        permission.getPlayerName() + " has been marker for \"regions.mod.local\" permissions. as is local mod of " + this.getName() + "."
        );
    }
    public void addPermission(Player player, int level) {
        this.addPermission(new Permission(player.getName(),this.hierarchy_,level));
    }
    public void addPermission(Player player, Hierarchy.Group group) {
        if (!this.hierarchy_.getGroups().contains(group)) { throw new IllegalArgumentException(
                "Group " + group.getName() + " is not present in " + this.getName() + "'s hierarchy."
        ); }
        this.addPermission(player,group.getLevel());
    }
    public boolean removePermission(Permission permission) {
        boolean r = this.permissions_.remove(permission);
        if (!r) { return false; }
        if (permission.getGroup().getLevel() > 1) { return true; }
        RegionsLibEventListener.removePermissionRegistration(permission.getPlayerName(),"regions.mod.local");
        return true;
    }
    public boolean removePermissions(Player player) {
        return Arrays.stream(this.getPermissions(player)).anyMatch(this::removePermission);
    }
    @Override
    public int compareTo(@Nonnull Region otherRegion) {
        return Integer.compare(this.getId(), otherRegion.getId());
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
    private void setInsidePlayers_(List<? extends Player> players) {
        Iterator<? extends Player> i = this.insidePlayers_.iterator();
        players = new LinkedList<>(players);
        Player p;
        while (i.hasNext()) {
            p = i.next();
            if (players.remove(p)) { continue; }
            i.remove();
            RegionsLib.getMain().getServer().getPluginManager().callEvent(new PlayerLeaveRegionEvent(p,this));
        }
        players.forEach(pl -> {
            this.insidePlayers_.add(pl);
            RegionsLib.getMain().getServer().getPluginManager().callEvent(new PlayerEnterRegionEvent(pl,this));
        });
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

            region.setPermissions(permissions);

            return region;
        }
    }
}