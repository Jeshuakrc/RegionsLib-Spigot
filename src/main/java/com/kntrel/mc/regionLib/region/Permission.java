package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import org.bukkit.entity.Player;
import java.util.Optional;

public class Permission {

    //FIELDS
    private final String playerName_;
    private final Region region_;
    private final Hierarchy.Group group_;


    //CONSTRUCTORS
    public Permission (String player, Region region, int level) {
        this.playerName_ = player;
        this.region_ = region;
        this.group_ = this.region_.getHierarchy().getGroupAtOrAbove(level).orElse(null);
    }

    //GETTERS
    public Region getRegion() {
        return this.region_;
    }
    public Hierarchy.Group getGroup(){
        return this.group_;
    }
    public String getPlayerName() {
        return this.playerName_;
    }
    public Optional<Player> getPlayer(){
        return Optional.ofNullable(this.region_.getContext().getServer().getPlayer(this.playerName_));
    }
}
