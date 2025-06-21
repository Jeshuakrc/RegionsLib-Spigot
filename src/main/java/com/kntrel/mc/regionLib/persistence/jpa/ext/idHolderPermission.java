package com.kntrel.mc.regionLib.persistence.jpa.ext;

import com.kntrel.mc.regionLib.region.Permission;
import com.kntrel.mc.regionLib.region.Region;

public class idHolderPermission extends Permission implements IdHolder {

    private final Long id_;

    public idHolderPermission(Long id, String player, Region region, int level) {
        super(player, region, level);
        this.id_ = id;
    }

    @Override
    public Long getId() {
        return this.id_;
    }
}
