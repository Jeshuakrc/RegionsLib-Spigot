package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import org.bukkit.entity.Player;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player's permission entry within a region.
 */
public class Permission {

    //SUBTYPE
    /**
     * Determines how to resolve overlapping permissions.
     */
    public enum OverlapMode { ALL, ANY, OLDEST, NEWEST }


    //FIELDS
    private final UUID playerId;
    private final Region region_;
    private final Hierarchy.Group group_;


    //CONSTRUCTORS
    /**
     * Creates a permission entry for a player and hierarchy level.
     *
     * @param playerId player unique id
     * @param region region associated with the permission
     * @param level hierarchy level
     */
    public Permission (UUID playerId, Region region, int level) {
        this.playerId = playerId;
        this.region_ = region;
        this.group_ = this.region_.getHierarchy().getGroupAtOrBellow(level).orElse(null);
    }

    //GETTERS
    /**
     * Returns the region for this permission.
     *
     * @return region instance
     */
    public Region getRegion() {
        return this.region_;
    }
    /**
     * Returns the hierarchy group for this permission.
     *
     * @return group instance or null
     */
    public Hierarchy.Group getGroup(){
        return this.group_;
    }
    /**
     * Returns the player id.
     *
     * @return player UUID
     */
    public UUID getPlayerId() {
        return this.playerId;
    }
    /**
     * Returns the online player, if available.
     *
     * @return optional player
     */
    public Optional<Player> getPlayer(){
        return Optional.ofNullable(this.region_.getContext().getServer().getPlayer(this.playerId));
    }
}
