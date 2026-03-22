package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.util.Area;
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

/**
 * Represents a mutable region with permissions, rules, and spatial bounds.
 */
public class Region implements Comparable<Region> {

    //FIELDS
    private Long id_;
    private final RegionContext ctx_;
    private final World world_;
    private final ArrayList<Permission> permissions_ = new ArrayList<>();
    private String name_;
    private boolean enabled_ = true;
    private boolean isDestroyed_ = false;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private BoundingBox boundingBox_;
    private Hierarchy hierarchy_;
    private final Map<String, ValueHolder<?>> rulesValues_ = new HashMap<>();


    //CONSTRUCTORS
    /**
     * Creates a new region with initial bounds and hierarchy.
     *
     * @param context region context
     * @param initialBox initial bounding box
     * @param world world containing the region
     * @param name region name
     * @param hierarchy region hierarchy
     */
    public Region(RegionContext context, BoundingBox initialBox, World world, String name, Hierarchy hierarchy) {
        this.ctx_ = context;
        this.world_ = world;
        this.resize(initialBox);
        this.setName(name);
        this.setHierarchy(hierarchy);
    }


    //SETTERS
    /**
     * Replaces the region permissions.
     *
     * @param permissions permission entries
     */
    public void setPermissions(Permission[] permissions) {
        this.permissions_.clear();
        Arrays.stream(permissions).forEach(this::addPermission);
    }
    /**
     * Sets the region id.
     *
     * @param id region id
     */
    public void setId(long id) {
        this.id_ = id;
    }
    /**
     * Sets the region name, enforcing min/max length rules.
     *
     * @param name region name
     */
    public void setName(String name) {
        int l = name.length();

        int compare = this.ctx_.getConfig().minNameLength;
        if (l < compare) { throw new IllegalArgumentException("The name is too short! Regions names may have a minimum of " + compare + " characters."); }

        compare = this.ctx_.getConfig().maxNameLength;
        if (l > compare && compare > 0) { throw new IllegalArgumentException("The name is too long! Regions names may have a maximum of " + compare + " characters."); }

        name_ = name;
    }
    /**
     * Enables or disables the region.
     *
     * @param bool true to enable, false to disable
     */
    public void enabled(boolean bool) {
        if (bool) { this.enable(); } else { this.disable(); }
    }
    /**
     * Enables the region.
     */
    public void enable() {
        this.enabled_ = true;
    }
    /**
     * Disables the region.
     */
    public void disable() {
        this.enabled_ = false;
    }
    /**
     * Sets the data container for this region.
     *
     * @param dataContainer data container
     */
    public void setDataContainer(RegionDataContainer dataContainer){
        dataContainer_ = dataContainer;
    }
    /**
     * Sets the hierarchy for this region.
     *
     * @param hierarchy hierarchy instance
     */
    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy_ = hierarchy;
    }


    //GETTERS
    /**
     * Returns the region id.
     *
     * @return id or null if not assigned
     */
    public Long getId() {
        return this.id_;
    }
    /**
     * Returns the region context.
     *
     * @return region context
     */
    public RegionContext getContext() {
        return this.ctx_;
    }
    /**
     * Returns the world environment for this region.
     *
     * @return world environment
     */
    public World.Environment getDimension() {
        return this.world_.getEnvironment();
    }
    /**
     * Returns the region name.
     *
     * @return region name
     */
    public String getName() {
        return this.name_;
    }
    /**
     * Returns the min/max corner coordinates as an array.
     *
     * @return array of [minX, minY, minZ, maxX, maxY, maxZ]
     */
    public double[] getCorners() {
        BoundingBox box = this.boundingBox_;
        return new double[] { box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ() };
    }
    /**
     * Returns whether the region is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return this.enabled_;
    }
    /**
     * Returns whether the region is destroyed.
     *
     * @return true if destroyed
     */
    public boolean isDestroyed() {
        return this.isDestroyed_;
    }
    /**
     * Returns the data container for this region.
     *
     * @return data container
     */
    public RegionDataContainer getDataContainer(){ return dataContainer_; }
    /**
     * Returns a copy of the region bounding box.
     *
     * @return bounding box copy
     */
    public BoundingBox getBoundingBox(){
        return new BoundingBox().copy(this.boundingBox_);
    }
    /**
     * Returns the region volume.
     *
     * @return volume
     */
    public double getVolume() {
        return this.getHeight() * this.getWidthX() * this.getWidthZ();
    }
    /**
     * Returns the region height.
     *
     * @return height
     */
    public double getHeight(){
        return this.boundingBox_.getHeight();
    }
    /**
     * Returns the width on the X axis.
     *
     * @return width on X
     */
    public double getWidthX(){
        return this.boundingBox_.getWidthX();
    }
    /**
     * Returns the width on the Z axis.
     *
     * @return width on Z
     */
    public double getWidthZ(){
        return this.boundingBox_.getWidthZ();
    }
    /**
     * Returns the length along the requested axis.
     *
     * @param axis axis to measure
     * @return length along axis
     */
    public double getLength(Axis axis) {
        return switch (axis) {
            case X -> this.getWidthX();
            case Z -> this.getWidthZ();
            case Y -> this.getHeight();
        };
    }
    /**
     * Returns the minimum X bound.
     *
     * @return min X
     */
    public double getMinX() {
        return this.boundingBox_.getMinX();
    }
    /**
     * Returns the minimum Y bound.
     *
     * @return min Y
     */
    public double getMinY() {
        return this.boundingBox_.getMinY();
    }
    /**
     * Returns the minimum Z bound.
     *
     * @return min Z
     */
    public double getMinZ() {
        return this.boundingBox_.getMinZ();
    }
    /**
     * Returns the maximum X bound.
     *
     * @return max X
     */
    public double getMaxX() {
        return this.boundingBox_.getMaxX();
    }
    /**
     * Returns the maximum Y bound.
     *
     * @return max Y
     */
    public double getMaxY() {
        return this.boundingBox_.getMaxY();
    }
    /**
     * Returns the maximum Z bound.
     *
     * @return max Z
     */
    public double getMaxZ() {
        return this.boundingBox_.getMaxZ();
    }
    /**
     * Returns the dimensions vector.
     *
     * @return width/height/length vector
     */
    public Vector getDimensions() {
        return new Vector(this.getWidthX(), this.getHeight(), this.getWidthZ());
    }
    /**
     * Returns the coordinate for the specified face.
     *
     * @param face cartesian block face
     * @return coordinate for the face
     */
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
    /**
     * Returns the center location of the region.
     *
     * @return center location
     */
    public Location getCenter() {
        Vector center = this.boundingBox_.getCenter();
        return new Location(this.getWorld(), center.getX(), center.getY(), center.getZ());
    }
    /**
     * Returns the world containing the region.
     *
     * @return world instance
     */
    public World getWorld() {
        return this.world_;
    }
    /**
     * Returns the hierarchy assigned to the region.
     *
     * @return hierarchy
     */
    public Hierarchy getHierarchy() {
        return this.hierarchy_;
    }
    /**
     * Returns the resolved rule values for this region.
     *
     * @return list of rule values
     */
    public List<RuleValue<?>> getRuleValues() {
        final RuleRegistry rr = this.ctx_.getRuleRegistry();
        return this.rulesValues_.entrySet().stream()
                .<RuleValue<?>>map(e -> new RuleValue<>(rr.get(e.getKey()).orElse(new OrphanRule(e.getKey())), e.getValue().toString()))
                .toList();
    }
    /**
     * Returns the raw rule value holder.
     *
     * @param name rule name
     * @return optional value holder
     */
    public Optional<ValueHolder<?>> getRuleValue(String name) {
        return Optional.ofNullable(this.rulesValues_.get(name));
    }
    /**
     * Returns a typed rule value if it matches the expected type.
     *
     * @param name rule name
     * @param type expected value type
     * @return optional typed value
     * @param <T> value type
     */
    public <T> Optional<T> getRuleValue(String name, ValueType<T> type) {
        ValueHolder<?> ruleValue = this.rulesValues_.get(name);
        if (ruleValue == null) { return Optional.empty(); }
        if (!ruleValue.getType().equals(type)) { return Optional.empty(); }
        return Optional.of(type.valueOf(ruleValue.toString()));
    }
    /**
     * Returns a typed rule value using the rule definition.
     *
     * @param rule rule definition
     * @return optional typed value
     * @param <T> value type
     */
    public <T> Optional<T> getRuleValue(Rule<T> rule) {
        return this.getRuleValue(rule.name(), rule.valueType());
    }
    /**
     * Returns an immutable list of permissions.
     *
     * @return permissions list
     */
    public List<Permission> getPermissions() {
        return List.copyOf(this.permissions_);
    }
    /**
     * Returns permissions for a specific player.
     *
     * @param player target player
     * @return permissions for player
     */
    public List<Permission> getPermissions(Player player) {
        return this.permissions_.stream().filter(p -> p.getPlayerId().equals(player.getName())).toList();
    }
    /**
     * Returns online members matching the predicate.
     *
     * @param condition filter predicate
     * @return list of online members
     */
    public List<Player> getOnlineMembers(Predicate<Player> condition) {
        return this.permissions_.stream()
                .map(Permission::getPlayer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(condition)
                .toList();
    }
    /**
     * Returns all online members.
     *
     * @return list of online members
     */
    public List<Player> getOnlineMembers() {
        return this.getOnlineMembers(p -> true);
    }
    /**
     * Returns online members whose group level is at most the provided level.
     *
     * @param level max group level
     * @return list of matching online members
     */
    public List<Player> getOnlineMembersMaxGroup(int level) {
        return this.permissions_.stream()
                .filter(perm -> perm.getGroup().getLevel() <= level)
                .map(Permission::getPlayer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
    /**
     * Returns players currently tracked within this region.
     *
     * @return players within this region
     */
    public List<Player> getPlayersWithin() {
        return this.ctx_.getPlayersWithin(this);
    }
    /**
     * Convenience alias for {@link #getPlayersWithin()}.
     *
     * @return players within this region
     */
    public List<Player> getPlayerWithin() {
        return this.getPlayersWithin();
    }
    /**
     * Returns whether the region touches a chunk coordinate.
     *
     * @param x chunk X
     * @param z chunk Z
     * @return true if region intersects the chunk
     */
    public boolean touchesChunk(int x, int z) {
        final int minX = x << Constants.CHUNK_SHIFT, minZ = z << Constants.CHUNK_SHIFT;
        return     this.getMinX() < minX + Constants.CHUNK_SIZE
                && this.getMaxX() > minX
                && this.getMinZ() < minZ + Constants.CHUNK_SIZE
                && this.getMaxZ() > minZ;
    }


    //PERMISSIONS
    /**
     * Checks whether a player is allowed to use an ability.
     *
     * @param player player to check
     * @param ability ability to check
     * @return true if allowed
     */
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
    /**
     * Returns whether the player is a member of the region.
     *
     * @param player player to check
     * @return true if member
     */
    public boolean isMember(Player player) {
        if (player == null) { return false; }
        return permissions_.stream().anyMatch(perm -> perm.getPlayerId().equals(player.getName()));
    }
    /**
     * Adds a permission entry to the region.
     *
     * @param permission permission entry
     */
    public void addPermission(Permission permission) {
        if (!permission.getRegion().equals(this)) {
            throw new IllegalArgumentException("Added permission from a different region");
        }

        this.permissions_.add(permission);
    }
    /**
     * Adds a permission entry for a player id and group level.
     *
     * @param playerId player id
     * @param level group level
     */
    public void addPermission(UUID playerId, int level) {
        this.addPermission(new Permission(playerId, this, level));
    }
    /**
     * Adds a permission entry for a player and group level.
     *
     * @param player player to grant
     * @param level group level
     */
    public void addPermission(Player player, int level) {
        this.addPermission(player.getUniqueId(), level);
    }
    /**
     * Adds a permission entry for a player and hierarchy group.
     *
     * @param player player to grant
     * @param group hierarchy group
     */
    public void addPermission(Player player, Hierarchy.Group group) {
        if (!this.hierarchy_.getGroups().contains(group)) {
            throw new IllegalArgumentException( "Group " + group.getName() + " is not present in " + this.getName() + "'s hierarchy.");
        }
        this.addPermission(player,group.getLevel());
    }
    /**
     * Removes a permission entry from the region.
     *
     * @param permission permission entry
     * @return true if removed
     */
    public boolean removePermission(Permission permission) {
        return this.permissions_.remove(permission);
    }
    /**
     * Removes permissions for a player.
     *
     * @param player player to remove
     * @return true if any removed
     */
    public boolean removePermissions(Player player) {
        return this.permissions_.removeIf(p -> p.getPlayer().map(pl -> pl.equals(player)).orElse(false));
    }
    /**
     * Clears all permissions for the region.
     */
    public void clearPermissions() {
        this.permissions_.clear();
    }


    //RULE
    /**
     * Returns whether a rule is present with a matching value type.
     *
     * @param rule rule definition
     * @return true if present
     */
    public boolean hasRule(Rule<?> rule) {
        ValueHolder<?> valueHolder = this.rulesValues_.get(rule.name());
        return rule.valueType().equals(valueHolder.getType());
    }
    /**
     * Returns whether a rule with the given name exists.
     *
     * @param name rule name
     * @return true if present
     */
    public boolean hasRule(String name) {
        return this.rulesValues_.containsKey(name);
    }
    /**
     * Sets a rule value using a {@link RuleValue}.
     *
     * @param ruleValue rule value to store
     */
    public void setRuleValue(RuleValue<?> ruleValue) {
        this.rulesValues_.put(ruleValue.getRule().name(), ruleValue);
    }
    /**
     * Sets a rule value by name.
     *
     * @param name rule name
     * @param value rule value
     * @param <T> value type
     */
    public <T> void setRuleValue(String name, T value) {
        this.rulesValues_.put(name, ValueHolder.of(value));
    }
    /**
     * Removes a rule by name.
     *
     * @param label rule name
     * @return true if removed
     */
    public boolean removeRule(String label) {
        return this.rulesValues_.remove(label) != null;
    }
    /**
     * Clears all rule values.
     */
    public void clearRules() {
        this.rulesValues_.clear();
    }


    //SHAPE
    /**
     * Returns whether a location point is contained in the region.
     *
     * @param location location to test
     * @return true if contained
     */
    public boolean contains(Location location) {
        return this.contains(location.getX(),location.getY(),location.getZ(),location.getWorld());
    }
    /**
     * Returns whether the region contains a coordinate in a world.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @param world world to validate
     * @return true if contained
     */
    public boolean contains(double x, double y, double z, World world) {
        if(!this.getWorld().equals(world)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    /**
     * Returns whether the region fully contains a bounding box.
     *
     * @param boundingBox bounding box to test
     * @return true if contained
     */
    public boolean contains(BoundingBox boundingBox) {
        return this.boundingBox_.contains(boundingBox);
    }
    /**
     * Returns whether the region fully contains an area.
     *
     * @param area area to test
     * @return true if contained
     */
    public boolean contains(Area area) {
        if (!this.getWorld().equals(area.getWorld())) { return false; }
        return this.contains((BoundingBox) area);
    }
    /**
     * Returns whether the region overlaps with a bounding box.
     *
     * @param boundingBox bounding box to test
     */
    public boolean overlaps(BoundingBox boundingBox) {
        return this.boundingBox_.overlaps(boundingBox);
    }
    /**
     * Returns whether the region overlaps with an area.
     * The check is the same as with {@link Region#overlaps(BoundingBox)}, but it also checks for a worlds match.
     *
     * @param area area to test
     */
    public boolean overlaps(Area area) {
        if (!this.getWorld().equals(area.getWorld())) { return false; }
        return this.overlaps((BoundingBox) area);
    }

    /**
     * Returns whether the region overlaps with another region.
     * The check is the same as with {@link Region#overlaps(BoundingBox)}, but it also checks for a worlds match.
     *
     * @param other region to test
     */
    public boolean overlaps(Region other) {
        if (!this.getWorld().equals(other.getWorld())) { return false; }
        return this.boundingBox_.overlaps(other.boundingBox_);
    }
    /**
     * Returns regions overlapping this region.
     *
     * @return list of overlapping regions
     */
    public List<Region> getOverlappingRegions() {
        return this.ctx_.getIn(this);
    }
    /**
     * Resizes the region to the provided corner array.
     *
     * @param corners [minX, minY, minZ, maxX, maxY, maxZ]
     */
    public void resize(double[] corners) {
        if (corners.length < 6) {
            throw new IndexOutOfBoundsException("The provided array must be of length 6");
        }
        this.resize(corners[0],corners[1],corners[2],corners[3],corners[4],corners[5]);
    }
    /**
     * Resizes the region to the provided bounds.
     *
     * @param x1 min X
     * @param y1 min Y
     * @param z1 min Z
     * @param x2 max X
     * @param y2 max Y
     * @param z2 max Z
     */
    public void resize(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.boundingBox_.resize(x1, y1, z1, x2, y2, z2);
    }
    /**
     * Resizes the region to the provided bounding box.
     *
     * @param boundingBox bounding box to copy
     */
    public void resize(BoundingBox boundingBox) {
        this.boundingBox_ = boundingBox.clone();
    }
    /**
     * Expands the region bounds by directional amounts.
     *
     * @param negativeX negative X expansion
     * @param negativeY negative Y expansion
     * @param negativeZ negative Z expansion
     * @param positiveX positive X expansion
     * @param positiveY positive Y expansion
     * @param positiveZ positive Z expansion
     */
    public void expand(double negativeX, double negativeY, double negativeZ, double positiveX, double positiveY, double positiveZ) {
        this.boundingBox_.expand(negativeX, negativeY, negativeZ, positiveX, positiveY, positiveZ);
    }
    /**
     * Expands the region equally in all directions.
     *
     * @param x expansion on X
     * @param y expansion on Y
     * @param z expansion on Z
     */
    public void expand(double x, double y, double z) {
        this.boundingBox_.expand(x, y, z);
    }
    /**
     * Expands the region by a vector.
     *
     * @param expansion expansion vector
     */
    public void expand(Vector expansion) {
        this.boundingBox_.expand(expansion);
    }
    /**
     * Expands the region by a uniform amount.
     *
     * @param expansion expansion amount
     */
    public void expand(double expansion) {
        this.boundingBox_.expand(expansion);
    }
    /**
     * Expands the region in a direction by a specific amount.
     *
     * @param dirX direction X
     * @param dirY direction Y
     * @param dirZ direction Z
     * @param expansion expansion amount
     */
    public void expand(double dirX, double dirY, double dirZ, double expansion) {
        this.boundingBox_.expand(dirX, dirY, dirZ, expansion);
    }
    /**
     * Expands the region in a direction by a specific amount.
     *
     * @param direction direction vector
     * @param expansion expansion amount
     */
    public void expand(Vector direction, double expansion) {
        this.boundingBox_.expand(direction, expansion);
    }
    /**
     * Expands the region toward a block face.
     *
     * @param blockFace block face direction
     * @param expansion expansion amount
     */
    public void expand(BlockFace blockFace, double expansion) {
        this.boundingBox_.expand(blockFace, expansion);
    }
    /**
     * Expands the region in a directional vector.
     *
     * @param dirX direction X
     * @param dirY direction Y
     * @param dirZ direction Z
     */
    public void expandDirectional(double dirX, double dirY, double dirZ) {
        this.boundingBox_.expandDirectional(dirX, dirY, dirZ);
    }
    /**
     * Expands the region in a directional vector.
     *
     * @param direction direction vector
     */
    public void expandDirectional(Vector direction) {
        this.boundingBox_.expandDirectional(direction);
    }


    //DISPLAY
    /**
     * Displays the region to a player using the default displayer.
     *
     * @param player player to display to
     */
    public void display(Player player) {
        this.ctx_.displayRegion(this, player);
    }
    /**
     * Displays the region to a player using a custom displayer.
     *
     * @param displayer displayer to use
     * @param player player to display to
     */
    public void display(AreaDisplayer displayer, Player player) {
        this.ctx_.displayRegion(this, displayer, player);
    }
    /**
     * Displays the region to a player for a duration.
     *
     * @param seconds duration in seconds
     * @param player player to display to
     */
    public void display(long seconds, Player player) {
        this.ctx_.displayRegion(this, seconds, player);
    }
    /**
     * Displays the region to a player for a duration with a custom displayer.
     *
     * @param displayer displayer to use
     * @param seconds duration in seconds
     * @param player player to display to
     */
    public void display(AreaDisplayer displayer, long seconds, Player player) {
        this.ctx_.displayRegion(this, displayer, seconds, player);
    }
    /**
     * Stops displaying the region.
     */
    public void stopDisplay() {
        this.ctx_.stopDisplayRegion(this);
    }


    //LIFECYCLE
    /**
     * Persists the region changes, optionally attributed to an entity.
     *
     * @param doer entity that initiated the save
     */
    public void save(@Nullable Entity doer) {
        this.getContext().save(doer, this);
    }
    /**
     * Persists the region changes without a specific actor.
     */
    public void save() {
        this.save(null);
    }
    /**
     * Clears region data and marks it as destroyed.
     */
    public void destroy() {
        this.permissions_.clear();
        this.rulesValues_.clear();
        this.dataContainer_.clear();
        this.isDestroyed_ = true;
    }

    //FIELDS
    /**
     * Extracts a field value from the region.
     *
     * @param field region field definition
     * @return extracted value
     * @param <T> value type
     */
    public <T> T getField(RegionField<T> field) {
        return field.extract(this);
    }

    //IMPLEMENTATIONS
    /**
     * Compares regions by id.
     *
     * @param o other object
     * @return true if ids match
     */
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof Region other)) { return false; }
        return other.id_.equals(this.id_);
    }
    /**
     * Hashes the region by id.
     *
     * @return hash code
     */
    @Override public int hashCode() {
        if (this.id_ == null) {
            throw new IllegalStateException("Region has not identity yet. Must be assigned an ID.");
        }
        return this.id_.hashCode();
    }
    /**
     * Compares regions by id ordering.
     *
     * @param otherRegion other region
     * @return comparison result
     */
    @Override public int compareTo(@NotNull Region otherRegion) {
        return Long.compare(this.getId(), otherRegion.getId());
    }
}
