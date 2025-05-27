package com.jkantrell.regionslib.persistence.jpa.ext;

import com.jkantrell.regionslib.region.Permission;
import com.jkantrell.regionslib.region.Region;

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
