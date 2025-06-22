package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import org.bukkit.entity.Player;
import java.util.Optional;
import java.util.UUID;

public class Permission {

    //FIELDS
    private final UUID playerId;
    private final Region region_;
    private final Hierarchy.Group group_;


    //CONSTRUCTORS
    public Permission (UUID playerId, Region region, int level) {
        this.playerId = playerId;
        this.region_ = region;
        this.group_ = this.region_.getHierarchy().getGroupAtOrBellow(level).orElse(null);
    }

    //GETTERS
    public Region getRegion() {
        return this.region_;
    }
    public Hierarchy.Group getGroup(){
        return this.group_;
    }
    public UUID getPlayerId() {
        return this.playerId;
    }
    public Optional<Player> getPlayer(){
        return Optional.ofNullable(this.region_.getContext().getServer().getPlayer(this.playerId));
    }
}