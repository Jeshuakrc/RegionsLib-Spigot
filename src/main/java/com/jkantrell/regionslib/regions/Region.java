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
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

public class Region implements Comparable<Region> {

    //STATIC METHODS
    public static void setPlayerSampling(long rate) {
        if (Region.playerSampler_ != null) { Region.playerSampler_.cancel(); }
        Region.playerSampler_ = new BukkitRunnable() {
            @Override
            public void run() {
                Regions.regions_.forEach(
                        r -> r.setInsidePlayers_(
                                Bukkit.getOnlinePlayers().stream().filter(p -> r.contains(p.getLocation())).toList()
                        )
                );
            }
        };
        Region.playerSampler_.runTaskTimer(RegionsLib.getMain(),0, rate);

    }

    //STATIC FIELDS
    private static BukkitRunnable playerSampler_ = null;

    //FIELDS
    private int id_;
    private World world_ = null;
    private final ArrayList<Permission> permissions_ = new ArrayList<>();
    private String name_;
    private boolean enabled_ = true;
    private boolean isDestroyed_ = false;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private BoundingBox boundingBox_ = new BoundingBox();
    private BoundaryDisplayer boundaryDisplayer_ = null;
    private Hierarchy hierarchy_;
    private final List<Rule> rules_ = new ArrayList<>();
    private final Config.ParticleData boundaryParticle_ = RegionsLib.CONFIG.regionBorderParticle;
    private final Region self_ = this;
    private final LinkedList<Player> insidePlayers_ = new LinkedList<>();

    //CONSTRUCTORS
    public Region(double[] vertex, World world, String name, Hierarchy hierarchy, @Nullable Entity creator) {
        this.setId(Regions.getHighestId() + 1);
        this.setWorld(world);
        this.resize(vertex);
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

    //SETTERS
    public void setWorld(World world) {
        world_ = world;
    }
    public void setPermissions(Permission[] permissions) {
        this.permissions_.clear();
        Arrays.stream(permissions).forEach(this::addPermission);
    }
    public void setId(int id) {
        if (Regions.regions_.stream().anyMatch(r -> r.getId() == id)) {
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
    public double[] getCorners() {
        BoundingBox box = this.boundingBox_;
        return new double[] { box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ() };
    }
    public boolean isEnabled() {
        return this.enabled_;
    }
    public boolean isDestroyed() {
        return this.isDestroyed_;
    }
    public RegionDataContainer getDataContainer(){ return dataContainer_; }
    public BoundingBox getBoundingBox(){
        return new BoundingBox().copy(this.boundingBox_);
    }
    public double getVolume() {
        return this.getHeight() * this.getWidthX() * this.getWidthZ();
    }
    public double getHeight(){
        return this.boundingBox_.getHeight();
    }
    public double getWidthX(){
        return this.boundingBox_.getWidthX();
    }
    public double getWidthZ(){
        return this.boundingBox_.getWidthZ();
    }
    public double getLength(Axis axis) {
        return switch (axis) {
            case X -> this.getWidthX();
            case Z -> this.getWidthZ();
            case Y -> this.getHeight();
        };
    }
    public double getMinX() {
        return this.boundingBox_.getMinX();
    }
    public double getMinY() {
        return this.boundingBox_.getMinY();
    }
    public double getMinZ() {
        return this.boundingBox_.getMinZ();
    }
    public double getMaxX() {
        return this.boundingBox_.getMaxX();
    }
    public double getMaxY() {
        return this.boundingBox_.getMaxY();
    }
    public double getMaxZ() {
        return this.boundingBox_.getMaxZ();
    }
    public Vector getDimensions() {
        return new Vector(this.getWidthX(), this.getHeight(), this.getWidthZ());
    }
    public double getFacePos(BlockFace face) {
        return switch (face) {
            case UP -> this.getMaxY();
            case DOWN -> this.getMinY();
            case SOUTH -> this.getMaxZ();
            case NORTH -> this.getMinZ();
            case EAST -> this.getMaxX();
            case WEST -> this.getMinX();
            default -> throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        };
    }
    public Location getCenter() {
        Vector center = this.boundingBox_.getCenter();
        return new Location(this.getWorld(), center.getX(), center.getY(), center.getZ());
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
    public boolean contains(double x, double y, double z, World world) {
        if(!this.getWorld().equals(world)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    public void save() {
        if (this.isDestroyed_) {
            RegionsLib.getMain().getLogger().fine("Region '" + this.getName() + "' cannot be saved as it has been destroyed.");
            return;
        }
        if (!Regions.regions_.contains(this)) { Regions.regions_.add(this); }
        Serializer.serializeToFile(Serializer.FILES.REGIONS, Regions.regions_);
    }
    public Region[] getOverlappingRegions() {
        return Regions.getIn(this);
    }
    public void destroy(@Nullable Entity destructor){
        RegionDestroyEvent event = new RegionDestroyEvent(this, destructor);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        if(boundaryDisplayer_ != null && !boundaryDisplayer_.displayer.isCancelled()) { this.boundaryDisplayer_.cancel(); }
        this.insidePlayers_.clear();
        if (Regions.regions_.remove(this)) {
            Serializer.serializeToFile(Serializer.FILES.REGIONS, Regions.regions_);
        }
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
    public void resize(double[] corners) {
        this.boundingBox_.resize(corners[0],corners[1],corners[2],corners[3],corners[4],corners[5]);
    }
    public void expand(double negativeX, double negativeY, double negativeZ, double positiveX, double positiveY, double positiveZ) {
        this.boundingBox_.expand(negativeX, negativeY, negativeZ, positiveX, positiveY, positiveZ);
    }
    public void expand(double x, double y, double z) {
        this.boundingBox_.expand(x, y, z);
    }
    public void expand(Vector expansion) {
        this.boundingBox_.expand(expansion);
    }
    public void expand(double expansion) {
        this.boundingBox_.expand(expansion);
    }
    public void expand(double dirX, double dirY, double dirZ, double expansion) {
        this.boundingBox_.expand(dirX, dirY, dirZ, expansion);
    }
    public void expand(Vector direction, double expansion) {
        this.boundingBox_.expand(direction, expansion);
    }
    public void expand(BlockFace blockFace, double expansion) {
        this.boundingBox_.expand(blockFace, expansion);
    }
    public void expandDirectional(double dirX, double dirY, double dirZ) {
        this.boundingBox_.expandDirectional(dirX, dirY, dirZ);
    }
    public void expandDirectional(Vector direction) {
        this.boundingBox_.expandDirectional(direction);
    }
    @Override public int compareTo(@Nonnull Region otherRegion) {
        return Integer.compare(this.getId(), otherRegion.getId());
    }

    //PRIVATE METHODS
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
                Region region = Region.this;
                double interval = region.getHeight() / Math.floor(region.getHeight()) / 2;
                double min1 = region.getMinX(), min2 = region.getMinZ(), max1 = region.getMaxX(), max2 = region.getMaxZ();
                for (double i = 0; i <= region.getHeight(); i += interval) {
                    double[][] corners = { {min1, min2}, {min1, max2}, {max1, min2}, {max1, max2} };
                    for (int j = 0; j < 4; j++) {
                        region.getWorld().spawnParticle(
                                Particle.SCRAPE,
                                corners[j][0], region.getMinY() + (i), corners[j][1],
                                1, 0, 0, 0, 0
                        );
                    }
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

            if (!displayer.isCancelled()) { displayer.cancel(); }
            try {
                if (!canceller.isCancelled()) { canceller.cancel(); }
            } catch (IllegalStateException ignored) {}
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
            jsonRegion.add("vertex", new Gson().toJsonTree(src.getCorners()));

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