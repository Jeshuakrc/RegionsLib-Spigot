package com.kntrel.mc.regionLib.persistence.jpa.ext;

import com.kntrel.mc.regionLib.region.Permission;
import com.kntrel.mc.regionLib.region.Region;

import java.util.UUID;

public class idHolderPermission extends Permission implements IdHolder {

    private final Long id_;

    public idHolderPermission(Long id, UUID playerId, Region region, int level) {
        super(playerId, region, level);
        this.id_ = id;
    }

    @Override
    public Long getId() {
        return this.id_;
    }
}
