package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.event.PlayerEnterRegionEvent;
import com.kntrel.mc.regionLib.event.PlayerLeaveRegionEvent;
import com.kntrel.mc.regionLib.event.RegionCreateEvent;
import com.kntrel.mc.regionLib.event.RegionDestroyEvent;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class Region implements Comparable<Region> {

    //FIELDS
    private Long id_;
    private final RegionContext ctx_;
    private World world_ = null;
    private final ArrayList<Permission> permissions_ = new ArrayList<>();
    private String name_;
    private boolean enabled_ = true;
    private boolean isDestroyed_ = false;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private BoundingBox boundingBox_;
    private Hierarchy hierarchy_;
    private final LinkedList<Player> insidePlayers_ = new LinkedList<>();
    private final Map<String, ValueHolder<?>> rulesValues_ = new HashMap<>();


    //CONSTRUCTORS
    public Region(RegionContext context, BoundingBox initialBox, World world, String name, Hierarchy hierarchy, @Nullable Entity creator) {
        this.ctx_ = context;
        this.setWorld(world);
        this.resize(initialBox);
        this.setName(name);
        this.setHierarchy(hierarchy);

        if (creator instanceof Player player) {
            this.addPermission(player,1);
        }

        RegionCreateEvent event = new RegionCreateEvent(this, creator);
        this.ctx_.callEvent(event);
        if (event.isCancelled()) { this.destroy(); }
    }
    public Region(RegionContext context, BoundingBox initialBox, World world, String name, Hierarchy hierarchy) {
        this(context, initialBox, world, name, hierarchy, null);
    }


    //SETTERS
    public void setWorld(World world) {
        world_ = world;
    }
    public void setPermissions(Permission[] permissions) {
        this.permissions_.clear();
        Arrays.stream(permissions).forEach(this::addPermission);
    }
    public void setId(long id) {
        this.id_ = id;
    }
    public void setName(String name) {
        int l = name.length();

        int compare = RegionLib.CONFIG.minNameLength;
        if (l < compare) { throw new IllegalArgumentException("The name is too short! Regions names may have a minimum of " + compare + " characters."); }

        compare = RegionLib.CONFIG.maxNameLength;
        if (l > compare && compare > 0) { throw new IllegalArgumentException("The name is too long! Regions names may have a maximum of " + compare + " characters."); }

        name_ = name;
    }
    public void enabled(boolean bool) {
        if (bool) { this.enable(); } else { this.disable(); }
    }
    public void enable() {
        this.enabled_ = true;
        this.setInsidePlayers_(this.getWorld().getPlayers().stream().filter(p -> this.contains(p.getLocation())).toList());
    }
    public void disable() {
        this.enabled_ = false;
    }
    public void setDataContainer(RegionDataContainer dataContainer){
        dataContainer_ = dataContainer;
    }
    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy_ = hierarchy;
    }


    //GETTERS
    public Long getId() {
        return this.id_;
    }
    public RegionContext getContext() {
        return this.ctx_;
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
    public Hierarchy getHierarchy() {
        return this.hierarchy_;
    }
    public List<RuleValue<?>> getRuleValues() {
        final RuleRegistry rr = this.ctx_.getRuleRegistry();
        return this.rulesValues_.entrySet().stream()
                .<RuleValue<?>>map(e -> new RuleValue<>(rr.get(e.getKey()).orElse(new OrphanRule(e.getKey())), e.getValue().toString()))
                .toList();
    }
    public Optional<ValueHolder<?>> getRuleValue(String name) {
        return Optional.ofNullable(this.rulesValues_.get(name));
    }
    public <T> Optional<T> getRuleValue(String name, ValueType<T> type) {
        ValueHolder<?> ruleValue = this.rulesValues_.get(name);
        if (ruleValue == null) { return Optional.empty(); }
        if (!ruleValue.getType().equals(type)) { return Optional.empty(); }
        return Optional.of(type.valueOf(ruleValue.toString()));
    }
    public <T> Optional<T> getRuleValue(Rule<T> rule) {
        return this.getRuleValue(rule.getName(), rule.getValueType());
    }
    public List<Permission> getPermissions() {
        return List.copyOf(this.permissions_);
    }
    public List<Permission> getPermissions(Player player) {
        return this.permissions_.stream().filter(p -> p.getPlayerId().equals(player.getName())).toList();
    }
    public List<Player> getOnlineMembers(Predicate<Player> condition) {
        return this.permissions_.stream()
                .map(Permission::getPlayer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(condition)
                .toList();
    }
    public List<Player> getOnlineMembers() {
        return this.getOnlineMembers(p -> true);
    }
    public List<Player> getOnlineMembersMaxGroup(int level) {
        return this.permissions_.stream()
                .filter(perm -> perm.getGroup().getLevel() <= level)
                .map(Permission::getPlayer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }


    //PERMISSIONS
    public boolean checkAbility(Player player, Ability ability) {

        if(!this.isEnabled()) { return true; }
        Permission perm = null;
        if (player != null) {
            for (Permission p : this.permissions_) {
                if (p.getPlayerId().equals(player.getUniqueId())) {
                    perm = p;
                    break;
                }
            }
        }
        return (perm == null) ? this.getHierarchy().checkAbility(ability) : this.getHierarchy().checkAbility(ability,perm.getGroup());
    }
    public boolean isMember(Player player) {
        if (player == null) { return false; }
        return permissions_.stream().anyMatch(perm -> perm.getPlayerId().equals(player.getName()));
    }
    public void addPermission(Permission permission) {
        if (!permission.getRegion().equals(this)) {
            throw new IllegalArgumentException("Added permission from a different region");
        }

        this.permissions_.add(permission);
    }
    public void addPermission(UUID playerId, int level) {
        this.addPermission(new Permission(playerId, this, level));
    }
    public void addPermission(Player player, int level) {
        this.addPermission(player.getUniqueId(), level);
    }
    public void addPermission(Player player, Hierarchy.Group group) {
        if (!this.hierarchy_.getGroups().contains(group)) {
            throw new IllegalArgumentException( "Group " + group.getName() + " is not present in " + this.getName() + "'s hierarchy.");
        }
        this.addPermission(player,group.getLevel());
    }
    public boolean removePermission(Permission permission) {
        return this.permissions_.remove(permission);
    }
    public boolean removePermissions(Player player) {
        return this.permissions_.removeIf(p -> p.getPlayer().map(pl -> pl.equals(player)).orElse(false));
    }
    public void clearPermissions() {
        this.permissions_.clear();
    }


    //RULE
    public boolean hasRule(Rule<?> rule) {
        ValueHolder<?> valueHolder = this.rulesValues_.get(rule.getName());
        return rule.getValueType().equals(valueHolder.getType());
    }
    public boolean hasRule(String name) {
        return this.rulesValues_.containsKey(name);
    }
    public void setRuleValue(RuleValue<?> ruleValue) {
        this.rulesValues_.put(ruleValue.getRule().getName(), ruleValue);
    }
    public <T> void setRuleValue(String name, T value) {
        this.rulesValues_.put(name, ValueHolder.of(value));
    }
    public boolean removeRule(String label) {
        return this.rulesValues_.remove(label) != null;
    }
    public void clearRules() {
        this.rulesValues_.clear();
    }


    //SHAPE
    public boolean contains(Location location) {
        return this.contains(location.getX(),location.getY(),location.getZ(),location.getWorld());
    }
    public boolean contains(double x, double y, double z, World world) {
        if(!this.getWorld().equals(world)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    public boolean contains(BoundingBox boundingBox) {
        return this.boundingBox_.contains(boundingBox);
    }
    public List<Region> getOverlappingRegions() {
        return this.ctx_.getIn(this);
    }
    public void resize(double[] corners) {
        if (corners.length < 6) {
            throw new IndexOutOfBoundsException("The provided array must be of length 6");
        }
        this.resize(corners[0],corners[1],corners[2],corners[3],corners[4],corners[5]);
    }
    public void resize(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.boundingBox_.resize(x1, y1, z1, x2, y2, z2);
    }
    public void resize(BoundingBox boundingBox) {
        this.boundingBox_ = boundingBox.clone();
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


    //DISPLAY
    public void display(Player player) {
        this.ctx_.displayRegion(this, player);
    }
    public void display(AreaDisplayer displayer, Player player) {
        this.ctx_.displayRegion(this, displayer, player);
    }
    public void display(long seconds, Player player) {
        this.ctx_.displayRegion(this, seconds, player);
    }
    public void display(AreaDisplayer displayer, long seconds, Player player) {
        this.ctx_.displayRegion(this, displayer, seconds, player);
    }
    public void stopDisplay() {
        this.ctx_.stopDisplayRegion(this);
    }


    //LIFECYCLE
    public void save() {
        this.getContext().save(this);
    }
    public void destroy(@Nullable Entity destructor){
        RegionDestroyEvent event = new RegionDestroyEvent(this, destructor);
        this.ctx_.callEvent(event);
        if (event.isCancelled()) { return; }

        this.insidePlayers_.clear();
        this.isDestroyed_ = true;
    }
    public void destroy() {
        this.destroy(null);
    }


    //IMPLEMENTATIONS
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof Region other)) { return false; }
        return other.id_.equals(this.id_);
    }
    @Override public int hashCode() {
        return this.id_.hashCode();
    }
    @Override public int compareTo(@NotNull Region otherRegion) {
        return Long.compare(this.getId(), otherRegion.getId());
    }

    //PRIVATE METHODS
    private void setInsidePlayers_(List<? extends Player> players) {
        Iterator<? extends Player> i = this.insidePlayers_.iterator();
        players = new LinkedList<>(players);
        Player p;
        while (i.hasNext()) {
            p = i.next();
            if (players.remove(p)) {
                continue;
            }
            i.remove();
            this.ctx_.getServer().getPluginManager().callEvent(new PlayerLeaveRegionEvent(p, this));
        }
        players.forEach(pl -> {
            this.insidePlayers_.add(pl);
            this.ctx_.getServer().getPluginManager().callEvent(new PlayerEnterRegionEvent(pl, this));
        });
    }
}